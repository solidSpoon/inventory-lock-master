package xyz.solidspoon.lockcore.entity;


import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
 * 即时库存表(PStockInstance)表实体类
 *
 * @author makejava
 * @since 2022-05-18 11:36:48
 */
@SuppressWarnings("serial")
@Data
public class PStockInstance extends Model<PStockInstance> {
    //编号
    private String id;
    //产品编码
    private String productSid;
    //品牌
    private String brandName;
    //品名
    private String productName;
    //系列
    private String series;
    //型号
    private String productCode;
    //订货号
    private String orderNo;
    //产品面价
    private Double faceValue;
    //计量单位
    private String measureUnit;
    //重量
    private Double weight;
    //系统调拨价
    private Double transferPrice;
    //调拨折扣
    private Double transferDiscount;
    //库存数量
    private Integer stockQuantity;
    //锁定数量
    private Integer lockingQuantity;
    //入库日期
    private Date inStockTime;
    //库存锁定版本号
    private String lockVersion;
    //创建时间
    private Date createDate;
    //更新时间
    private Date updateDate;
    //删除标记
    private String delFlag;
    }

