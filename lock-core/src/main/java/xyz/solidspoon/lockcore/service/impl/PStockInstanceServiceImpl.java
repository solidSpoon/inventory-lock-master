package xyz.solidspoon.lockcore.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;
import xyz.solidspoon.lockcore.dao.PStockInstanceDao;
import xyz.solidspoon.lockcore.dao.PStockInstanceFlowDao;
import xyz.solidspoon.lockcore.dto.LockStoreDTO;
import xyz.solidspoon.lockcore.dto.LockStoreItemDTO;
import xyz.solidspoon.lockcore.dto.Message;
import xyz.solidspoon.lockcore.entity.PStockInstance;
import xyz.solidspoon.lockcore.entity.PStockInstanceFlow;
import xyz.solidspoon.lockcore.handler.GuardedObject;
import xyz.solidspoon.lockcore.listener.LockCenter;
import xyz.solidspoon.lockcore.param.AnalysisResults;
import xyz.solidspoon.lockcore.param.LogicFlowParam;
import xyz.solidspoon.lockcore.service.PStockInstanceService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 即时库存表(PStockInstance)表服务实现类
 *
 * @author makejava
 * @since 2022-05-18 11:36:49
 */
@Service("pStockInstanceService")
@Slf4j
public class PStockInstanceServiceImpl extends ServiceImpl<PStockInstanceDao, PStockInstance> implements PStockInstanceService {

    @Autowired
    private PStockInstanceDao pStockInstanceDao;

    @Autowired
    private PStockInstanceFlowDao pStockInstanceFlowDao;

    @Autowired
    @Lazy
    private PStockInstanceServiceImpl pStockInstanceServiceImpl;

    @Autowired
    private LockCenter lockCenter;
    @Autowired
    RedissonClient redissonClient;

    @Override
    public void lockStock(List<LockStoreDTO> lockStoreDTOs) {
        GuardedObject<Message, String> go = lockCenter.reserve(lockStoreDTOs);

        // 其他操作

        Message result = lockCenter.getResult(go);
        if (!result.getIsSuccess()) {
            throw new RuntimeException(result.getErrorMessage());
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doLockStock(List<LockStoreDTO> lockStoreDTOs) {
        StopWatch totalWatch = new StopWatch();
        totalWatch.start();

        AnalysisResults analysisResults = analyse(lockStoreDTOs);

        Map<String, Integer> operationMapping = analysisResults.getOperationMapping();
        List<PStockInstanceFlow> logicFlows = analysisResults.getPStockInstanceFlows();

        StopWatch stockInstanceFlowStopWatch = new StopWatch();
        stockInstanceFlowStopWatch.start();
        log.info("插入库存流水行数：{}", logicFlows.size());
        pStockInstanceFlowDao.insertBatch(logicFlows);
        stockInstanceFlowStopWatch.stop();
        log.info("插入库存流水耗时：{}", stockInstanceFlowStopWatch.getTotalTimeMillis());

        StopWatch lockWatch = new StopWatch();
        lockWatch.start();
        log.info("锁库行数：{}", operationMapping.size());
        quickOperateStock(operationMapping);
        lockWatch.stop();
        log.info("锁库耗时：{} ms", lockWatch.getTotalTimeMillis());

        totalWatch.stop();
        log.info("总耗时：{} ms", totalWatch.getTotalTimeMillis());


    }

    @Override
    public void getLockAndOperateStock(LockStoreDTO lockStoreDTOs) throws InterruptedException {

        Runnable unlockOrderNo = lockOrderNo(lockStoreDTOs);

        RLock skuLock = getSkuLock(lockStoreDTOs);

//        RSemaphore limiter = redissonClient.getSemaphore("ced:lock-stock:limiter");
//        limiter.trySetPermits(50);

        try {
            skuLock.lock();
//            limiter.acquire();
            pStockInstanceServiceImpl.doLockStock(lockStoreDTOs.getLockStoreDTOList());
        } catch (Exception e) {
            log.error("lockStock or limiter error: {}", e.getMessage());
            unlockOrderNo.run();
            throw e;
        } finally {
            skuLock.unlockAsync();
//            limiter.release();
        }
    }
    private Runnable lockOrderNo(LockStoreDTO lockStoreDTOs) {
        RSetCache<String> setCache = redissonClient.getSetCache("ced:pStockInstance:orderSnLock");
        List<String> orderNos = lockStoreDTOs.getLockStoreDTOList().stream()
                .map(LockStoreDTO::getOrderSn)
                .collect(Collectors.toList());
        if (!setCache.tryAdd(5, TimeUnit.MINUTES, orderNos.toArray(new String[0]))) {
            throw new RuntimeException("订单号已被锁定");
        }
        return () -> setCache.removeAllAsync(orderNos);
    }

    private RLock getSkuLock(LockStoreDTO lockStoreDTOs) {
        RLock[] locks = lockStoreDTOs.getLockStoreDTOList()
                .stream()
                .map(LockStoreDTO::getLockStoreItems)
                .flatMap(Collection::stream)
                .map(LockStoreItemDTO::getSkuNo)
                .distinct()
                .map(key -> "ced:pStockInstance:" + key)
                .sorted()
                .map(key -> redissonClient.getLock(key))
                .toArray(RLock[]::new);
        RLock lock = redissonClient.getMultiLock(locks);
        return lock;
    }

    private void quickOperateStock(Map<String, Integer> operationMapping) {
        if (operationMapping.size() < 45) {
            Integer num = pStockInstanceDao.operationStockSmallData(operationMapping);
            if (num != operationMapping.size()) {
                throw new RuntimeException("操作失败");
            }
        } else {
            Boolean success = pStockInstanceDao.operationStockBigData(operationMapping);
            if (!success) {
                throw new RuntimeException("操作失败");
            }
        }
    }

    private void operateStock(Map<String, Integer> operationMapping) {
        //TODO 事务回滚临时表会回滚吗 答案是不会
        pStockInstanceDao.createTempTable();
        pStockInstanceDao.insertTemp(operationMapping);
        //TODO 'update' 的inner join 更新重复了会咋样？
        pStockInstanceDao.updateStock();
        Boolean success = pStockInstanceDao.isSuccess();
        pStockInstanceDao.dropTempTable();
        if (!success) {
            throw new RuntimeException("操作失败");
        }
    }

    /**
     * skuQtyMapping 会被修改
     *
     * @param lockStoreDTOs
     * @return
     */
    private AnalysisResults analyse(List<LockStoreDTO> lockStoreDTOs) {

        Map<String, Integer> skuTargetMapping = lockStoreDTOs.stream()
                .map(LockStoreDTO::getLockStoreItems)
                .flatMap(List::stream)
                .collect(Collectors.toMap(LockStoreItemDTO::getSkuNo, LockStoreItemDTO::getQty, Integer::sum));

        Map<String, List<LockStoreItemDTO>> skuItemMapping = lockStoreDTOs.stream()
                .peek(lockStoreDTO ->
                        lockStoreDTO.getLockStoreItems().forEach(lockStoreItemDTO -> {
                            lockStoreItemDTO.setMasterSn(lockStoreDTO.getOrderSn());
                        })
                ).map(LockStoreDTO::getLockStoreItems)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(LockStoreItemDTO::getSkuNo));


        List<LogicFlowParam> operationData = getOperationData(skuTargetMapping);

        List<PStockInstanceFlow> pStockInstanceFlows = operationData.stream()
                .map(logicFlowParam -> getLogicFlows(logicFlowParam, skuItemMapping))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        Map<String, Integer> operationMapping = operationData.stream()
                .collect(Collectors.toMap(logicFlowParam -> logicFlowParam.getPStockInstance().getId(), LogicFlowParam::getOpQty));

        return AnalysisResults.builder()
                .operationMapping(operationMapping)
                .pStockInstanceFlows(pStockInstanceFlows)
                .build();
    }

    private List<LogicFlowParam> getOperationData(Map<String, Integer> skuQtyMapping) {
        Map<String, LogicFlowParam> operationMapping = new HashMap<>();
        int pageSize = 1000;
        int pageNo = 0;
        Map<String, Integer> countMapping = new HashMap<>();
        AtomicInteger totalCount = new AtomicInteger(0);
        while (true) {
            //TODO 这块需要new吗？
            Set<String> skus = skuQtyMapping.keySet();
            int offset = (pageNo++ * pageSize) - totalCount.get();
            List<PStockInstance> pStockInstances = pStockInstanceDao.listLockable(skus, pageSize, offset);
            if (CollectionUtils.isEmpty(pStockInstances)) {
                break;
            }
            pStockInstances.forEach(pStockInstance -> {
                countMapping.putIfAbsent(pStockInstance.getProductSid(), 0);
                countMapping.computeIfPresent(pStockInstance.getProductSid(), (k, v) -> v + 1);
            });
            log.info("pStockInstancesSize:{}", pStockInstances.size());
            for (PStockInstance pStockInstance : pStockInstances) {
                Integer count = countMapping.getOrDefault(pStockInstance.getProductSid(), 0);

                Integer stockQty = pStockInstance.getStockQuantity();
                Integer targetQty = skuQtyMapping.remove(pStockInstance.getProductSid());
                if (targetQty == null) {
                    continue;
                }
                totalCount.addAndGet(count);

                Integer opQty = Math.min(stockQty, targetQty);
                if (targetQty - opQty > 0) {
                    skuQtyMapping.put(pStockInstance.getProductSid(), targetQty - opQty);
                    totalCount.addAndGet(-count);
                }

                if (operationMapping.containsKey(pStockInstance.getId())) {
                    throw new RuntimeException("幻读");
                }
                LogicFlowParam logicFlowParam = LogicFlowParam.builder()
                        .pStockInstance(pStockInstance)
                        .opQty(opQty)
                        .build();
                operationMapping.put(pStockInstance.getId(), logicFlowParam);
            }
            if (CollectionUtils.isEmpty(skuQtyMapping)) {
                break;
            }
        }
        if (!CollectionUtils.isEmpty(skuQtyMapping)) {
            throw new RuntimeException("库存不足");
        }
        return new ArrayList<>(operationMapping.values());
    }

    /**
     * skuItemMapping 会被修改
     *
     * @param logicFlowParam
     * @param skuItemMapping
     * @return
     */
    private List<PStockInstanceFlow> getLogicFlows(LogicFlowParam logicFlowParam, Map<String, List<LockStoreItemDTO>> skuItemMapping) {
        PStockInstance pStockInstance = logicFlowParam.getPStockInstance();
        Integer targetQty = logicFlowParam.getOpQty();
        List<PStockInstanceFlow> actuallyFlows = new ArrayList<>();
        List<LockStoreItemDTO> lockStoreItemDTOS = skuItemMapping.get(pStockInstance.getProductSid());
        if (CollectionUtils.isEmpty(lockStoreItemDTOS)) {
            throw new RuntimeException("生成锁库流水失败");
        }
        for (LockStoreItemDTO lockStoreItemDTO : lockStoreItemDTOS) {
            if (targetQty <= 0) {
                break;
            }
            Integer opQty = Math.min(targetQty, lockStoreItemDTO.getQty());
            lockStoreItemDTO.setQty(lockStoreItemDTO.getQty() - opQty);
            targetQty -= opQty;
            PStockInstanceFlow pStockInstanceFlow = new PStockInstanceFlow();
            pStockInstanceFlow.setId(UUID.randomUUID().toString());
            pStockInstanceFlow.setProductId(pStockInstance.getProductSid());
            pStockInstanceFlow.setCurrentQtyBefore(pStockInstance.getStockQuantity());
            pStockInstanceFlow.setCurrentQtyAfter(pStockInstance.getStockQuantity() - opQty);
            pStockInstanceFlow.setCurrentQtyChange(opQty);
            pStockInstanceFlow.setLockQtyBefore(pStockInstance.getLockingQuantity());
            pStockInstanceFlow.setLockQtyAfter(pStockInstance.getLockingQuantity() + opQty);
            pStockInstanceFlow.setLockQtyChange(opQty);
            pStockInstanceFlow.setIid(pStockInstance.getId());
            pStockInstanceFlow.setOptType(PStockInstanceFlow.Type.LOCK);
            pStockInstanceFlow.setCreateTime(new Date());
            pStockInstanceFlow.setOrderNum(lockStoreItemDTO.getMasterSn());
            pStockInstanceFlow.setCostPrice(0d);
            pStockInstanceFlow.setUpdateTime(new Date());
            pStockInstanceFlow.setStatus(1);
            pStockInstanceFlow.setDelFlag(0);
            pStockInstanceFlow.setTs(new Date());
            actuallyFlows.add(pStockInstanceFlow);
        }
        List<LockStoreItemDTO> remainingLockStoreItems = lockStoreItemDTOS
                .stream()
                .filter(item -> item.getQty() > 0)
                .collect(Collectors.toList());
        skuItemMapping.put(pStockInstance.getProductSid(), remainingLockStoreItems);
        return actuallyFlows;
    }
}

