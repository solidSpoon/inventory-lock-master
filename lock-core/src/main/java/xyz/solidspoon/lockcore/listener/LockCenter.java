package xyz.solidspoon.lockcore.listener;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RSetCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import xyz.solidspoon.lockcore.dao.PStockInstanceDao;
import xyz.solidspoon.lockcore.dto.LockStoreDTO;
import xyz.solidspoon.lockcore.dto.LockStoreItemDTO;
import xyz.solidspoon.lockcore.dto.Message;
import xyz.solidspoon.lockcore.dto.SummarizeDTO;
import xyz.solidspoon.lockcore.handler.GuardedObject;
import xyz.solidspoon.lockcore.handler.CachedHistory;
import xyz.solidspoon.lockcore.handler.LocalTx;
import xyz.solidspoon.lockcore.service.PStockInstanceService;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Slf4j
public class LockCenter implements ApplicationRunner {
    @Autowired
    private PStockInstanceDao pStockInstanceDao;
    @Autowired
    private PStockInstanceService pStockInstanceService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CachedHistory<String> history;

    static RBlockingDeque<Message> lockQueueRemote;
    static BlockingQueue<Message> lockQueueLocal = new LinkedTransferQueue<>();
    static RTopic resultTopic;

    RSetCache<Object> messageTimer;


    public void onLock() throws InterruptedException {
        LocalTx<String> localTx = history.getLocalTx();
        while (true) {
            Message take = lockQueueLocal.take();
            List<SummarizeDTO> summarize = pStockInstanceDao.summarize();
            Map<String, Integer> summarizeMapping = summarize.stream().collect(Collectors.toMap(SummarizeDTO::getKey, SummarizeDTO::getValue));
            log.info("执行了summarize");
            List<LockStoreDTO> totalLockStoreDTOS = new ArrayList<>();
            List<Message> successMessages = new ArrayList<>();
            localTx.start();
            int size = lockQueueLocal.size() + 1;
            for (int i = 0; i < size; i++) {
                String backup = JSON.toJSONString(summarizeMapping);
                Message message = take;
                if (i > 0) {
                    message = lockQueueLocal.poll();
                }
                if (message == null) {
                    break;
                }
                log.info("add message to history");
                boolean b = localTx.addIfAbsent(message.getOrderIds());
                log.info("add message to history result: {}", b);
                if (!b) {
                    message.setIsSuccess(false);
                    message.setErrorMessage("duplicate order number");
                    resultTopic.publishAsync(message);
                }
                List<LockStoreDTO> currentLockStockDTOs = message.getLockStockDTOs();
                message.setLockStockDTOs(null);
                Map<String, Integer> skuTargetMapping = currentLockStockDTOs.stream()
                        .map(LockStoreDTO::getLockStoreItems)
                        .flatMap(List::stream)
                        .collect(Collectors.toMap(LockStoreItemDTO::getSkuNo, LockStoreItemDTO::getQty, Integer::sum));
                boolean success = preLock(summarizeMapping, skuTargetMapping);
                message.setIsSuccess(success);
                if (!success) {
                    message.setErrorMessage("inventory shortage");
                    resultTopic.publishAsync(message);
                    summarizeMapping = JSON.parseObject(backup, Map.class);
                    continue;
                }
                successMessages.add(message);
                totalLockStoreDTOS.addAll(currentLockStockDTOs);
            }
            if (!CollectionUtils.isEmpty(totalLockStoreDTOS)) {
                try {
                    pStockInstanceService.doLockStock(totalLockStoreDTOS);
                    localTx.commit();
                } catch (Exception e) {
                    log.error("执行锁定库存失败", e);
                    successMessages.forEach(message -> message.setIsSuccess(false));
                    successMessages.forEach(message -> message.setErrorMessage(e.getMessage()));
                }
                successMessages.forEach(message -> {
                    resultTopic.publishAsync(message);
                });
                successMessages.clear();
            }
        }

    }

    /**
     * 预锁定库存
     *
     * @param summarizeMapping
     * @param skuTargetMapping
     * @return
     */
    private boolean preLock(Map<String, Integer> summarizeMapping, Map<String, Integer> skuTargetMapping) {
        for (Map.Entry<String, Integer> entry : skuTargetMapping.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            Integer qty = summarizeMapping.getOrDefault(key, 0);
            qty -= value;
            if (qty < 0) {
                return false;
            }
            summarizeMapping.put(key, qty);
        }
        return true;
    }


    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        lockQueueRemote = redissonClient.getBlockingDeque("ced:lockQueue");
        resultTopic = redissonClient.getTopic("ced:resultQueue");
        resultTopic.addListener(Message.class, (channel, msg) -> {
            GuardedObject.fireEvent(msg.getId(), msg);
        });
        messageTimer = redissonClient.getSetCache("ced:messageIds");

        boolean isLock = redissonClient.getLock("ced:lockCenterInstance").tryLock(10, TimeUnit.SECONDS);
        if (!isLock) {
            log.info("监听锁定库存线程已经启动");
            return;
        }
        log.info("监听锁定库存线程启动");
        int transferNum = 10;
        ExecutorService service = Executors.newFixedThreadPool(transferNum + 1);
        IntStream.range(0, transferNum).forEach(i -> {

            service.submit(() -> {
                while (true) {
                    try {
                        Message message = lockQueueRemote.take();
                        log.info("转移消息到本地队列");
                        log.info("id:{}", message.getId());
                        log.info(messageTimer.contains(message.getId()) + "");
                        if (!messageTimer.contains(message.getId())) {
                            message.setIsSuccess(false);
                            message.setErrorMessage("系统繁忙");
                            resultTopic.publishAsync(message);
                            continue;
                        }
                        lockQueueLocal.add(message);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            });
        });
        service.submit(() -> {
            while (true) {
                try {
                    onLock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * 预定库存扣减
     *
     * @param lockStoreDTOs
     * @return
     */
    public GuardedObject<Message, String> reserve(List<LockStoreDTO> lockStoreDTOs) {
        String id = UUID.randomUUID().toString();
        Message message = new Message();
        message.setId(id);
        message.setLockStockDTOs(lockStoreDTOs);
        GuardedObject<Message, String> go = GuardedObject.create(id);
        List<String> orderIds = lockStoreDTOs.stream().map(LockStoreDTO::getOrderSn).collect(Collectors.toList());
        log.info("orderIds:{}", orderIds);
        log.info("contains:" + history.containsAny(orderIds));
        if (history.containsAny(orderIds)) {
            message.setId(id);
            message.setIsSuccess(false);
            message.setErrorMessage("订单号重复");
            GuardedObject.fireEvent(id, message);
            return go;
        }
        messageTimer.add(id, 50, TimeUnit.SECONDS);
        lockQueueRemote.add(message);
        return go;
    }

    /**
     * 获取扣减结果
     *
     * @param go
     * @return
     */
    public Message getResult(GuardedObject<Message, String> go) {
        Message message = go.get(Objects::nonNull).orElseGet(() -> {
            Message defaultMessage = new Message();
            defaultMessage.setIsSuccess(false);
            defaultMessage.setErrorMessage("超时");
            return defaultMessage;
        });
        return message;
    }
}
