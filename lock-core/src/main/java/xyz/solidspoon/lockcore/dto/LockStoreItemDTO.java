package xyz.solidspoon.lockcore.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 锁库明细信息
 */
@Data
public class LockStoreItemDTO implements Serializable {
    /**
     * 商品skuNo
     */
    private String skuNo;
    /**
     * 库存操作数量
     */
    private Integer qty;

    private String masterSn;
}
