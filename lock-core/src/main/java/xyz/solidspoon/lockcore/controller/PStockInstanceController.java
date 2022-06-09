package xyz.solidspoon.lockcore.controller;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.solidspoon.lockcore.dto.LockStoreDTO;
import xyz.solidspoon.lockcore.service.PStockInstanceService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 即时库存表(PStockInstance)表控制层
 *
 * @author makejava
 * @since 2022-05-18 11:36:48
 */
@RestController
@RequestMapping("pStockInstance")
@Slf4j
public class PStockInstanceController {
    /**
     * 服务对象
     */
    @Autowired
    private PStockInstanceService pStockInstanceService;
    @Autowired
    RedissonClient redissonClient;

    static AtomicInteger count = new AtomicInteger(0);

    @GetMapping("/superLockStock")
    public Object superLockStock(@RequestBody LockStoreDTO lockStoreDTOs) {
        log.info("执行锁库 {}", count.incrementAndGet());
        try {
            pStockInstanceService.lockStock(lockStoreDTOs.getLockStoreDTOList());
        } catch (Exception e) {
            log.error("lockStock error", e);
            return e.getMessage();
        }
        return "success";
    }


    @GetMapping("/lockStock")
    public Object lockStock(@RequestBody LockStoreDTO lockStoreDTOs) {
        log.info("执行锁库 {}", count.incrementAndGet());
        try {
            pStockInstanceService.getLockAndOperateStock(lockStoreDTOs);
        } catch (Exception e) {
            log.error("lockStock error", e);
            return e.getMessage();
        }
        return "success";
    }
}

