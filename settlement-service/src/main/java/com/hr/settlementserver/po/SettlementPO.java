package com.hr.settlementserver.po;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class SettlementPO {
    private Integer id;
    private Integer orderId;
    private Integer transactionId;
    private String status;
    private BigDecimal amount;
    private Date date;
}
