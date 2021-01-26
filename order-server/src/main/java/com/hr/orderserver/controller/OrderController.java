package com.hr.orderserver.controller;

import com.hr.orderserver.server.OrderService;
import com.hr.orderserver.vo.OrderCreateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @program: rabbit-mq-server
 * @description:
 * @author: HuRan
 * @create: 2021-01-15 17:49
 */

@RestController
public class OrderController {
    @Autowired
    OrderService orderService;
    @PostMapping(value = "/createOrder")
    public void createOrder(@RequestBody  OrderCreateVO vo) {
        System.out.println("createOrder: " + vo.toString());
        try {
            orderService.createOrder(vo);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}