package xyz.solidspoon.lockcore.entity;


import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
 * 库存流水表(PStockInstanceFlow)表实体类
 *
 * @author makejava
 * @since 2022-05-18 18:20:41
 */
@SuppressWarnings("serial")
@Data
public class PStockInstanceFlow extends Model<PStockInstanceFlow> {
    //主键
    private String id;
    //订单号
    private String orderNum;
    //商品标识
    private String productId;
    //库存标识
    private String iid;
    //库存修改之前数量
    private Integer currentQtyBefore;
    //库存修改之后数量
    private Integer currentQtyAfter;
    //库存修改数量
    private Integer currentQtyChange;
    //库存锁定数量
    private Integer lockQtyBefore;
    //库存锁定之后数量
    private Integer lockQtyAfter;
    //库存锁定数量
    private Integer lockQtyChange;
//    //仓库
//    private String warehouse;
//    //货架
//    private String goodsShelves;
    //成本价
    private Double costPrice;
    //操作类型
    private Type optType;
    //添加时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //状态:0未使用,1已使用
    private Integer status;
    //删除标记
    private Integer delFlag;
    //时间戳
    private Date ts;



    /**
     * 操作来源
     */
    public  enum Type {
        /**
         * 锁定库存
         */
        LOCK
    }

}

