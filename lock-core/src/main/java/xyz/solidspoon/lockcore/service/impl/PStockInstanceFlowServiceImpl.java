package xyz.solidspoon.lockcore.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.solidspoon.lockcore.dao.PStockInstanceFlowDao;
import xyz.solidspoon.lockcore.entity.PStockInstanceFlow;
import xyz.solidspoon.lockcore.service.PStockInstanceFlowService;
import org.springframework.stereotype.Service;

/**
 * 库存流水表(PStockInstanceFlow)表服务实现类
 *
 * @author makejava
 * @since 2022-05-18 18:20:41
 */
@Service("pStockInstanceFlowService")
public class PStockInstanceFlowServiceImpl extends ServiceImpl<PStockInstanceFlowDao, PStockInstanceFlow> implements PStockInstanceFlowService {

}

