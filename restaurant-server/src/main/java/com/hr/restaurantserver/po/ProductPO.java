package com.hr.restaurantserver.po;

import com.hr.restaurantserver.enummeration.ProductStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class ProductPO {
    private Integer id;
    private String name;
    private BigDecimal price;
    private Integer restaurantId;
    private String status;
    private Date date;
}
