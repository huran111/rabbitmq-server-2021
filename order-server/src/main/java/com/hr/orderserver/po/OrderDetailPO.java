package com.hr.orderserver.po;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * order_detal 表对应的Entity
 */
@Getter
@Setter
@ToString
public class OrderDetailPO {
    private Integer id;
    private String status;
    private String address;
    private Integer accountId;
    private Integer productId;
    private Integer deliverymanId;
    private Integer settlementId;
    private Integer rewardId;
    private BigDecimal price;
    private Date date;
}
