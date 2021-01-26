package com.hr.orderserver.config;

import com.hr.orderserver.server.OrderMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @program: rabbit-mq-server
 * @description:
 * @author: HuRan
 * @create: 2021-01-19 15:27
 */
@Configuration
public class RabbitConfig {
    @Autowired
    OrderMessageService orderMessageService;

    @Autowired
    public void startListenMessage() {
        System.out.println("==================》》》 startListenMessage");
        try {
            orderMessageService.handleMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}