package xyz.solidspoon.lockcore.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 锁库信息
 */
@Data
public class LockStoreDTO implements Serializable {

    /**
     * 订单编号
     */
    private String orderSn;
    /**
     * 仓库Id
     */
    private String warehouseId;
    List<LockStoreDTO> lockStoreDTOList;

    private List<LockStoreItemDTO> lockStoreItems;
}
