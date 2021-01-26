package com.hr.orderserver.vo;


/**
 * @program: rabbit-mq-server
 * @description:
 * @author: HuRan
 * @create: 2021-01-15 13:51
 */

public class OrderCreateVO {
    /**
     * 用户ID
     */
    private Integer accountId;
    /**
     * 地址
     */
    private String address;
    /**
     * 产品ID
     */
    private Integer productId;

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getProductId() {
        return productId;
    }

    @Override
    public String toString() {
        return "OrderCreateVO{" +
                "accountId=" + accountId +
                ", address='" + address + '\'' +
                ", productId=" + productId +
                '}';
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }
}