package xyz.solidspoon.lockcore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;
import xyz.solidspoon.lockcore.dto.LockStoreDTO;
import xyz.solidspoon.lockcore.entity.PStockInstance;

import java.util.List;

/**
 * 即时库存表(PStockInstance)表服务接口
 *
 * @author makejava
 * @since 2022-05-18 11:36:48
 */
public interface PStockInstanceService extends IService<PStockInstance> {
    void lockStock(List<LockStoreDTO> lockStoreDTOs);

    @Transactional(rollbackFor = Exception.class)
    void doLockStock(List<LockStoreDTO> lockStoreDTOs);

    void getLockAndOperateStock(LockStoreDTO lockStoreDTOs) throws InterruptedException;
}

