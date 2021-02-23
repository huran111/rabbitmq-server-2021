package com.hr.orderserver.config;

import com.hr.orderserver.server.OrderMessageService;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
//@Configuration
public class RabbitConfig {
    @Autowired
    OrderMessageService orderMessageService;

    // @Autowired
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

    @Autowired
    public void initRabbitMQ() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        Exchange directExchange = new DirectExchange("exchange.order.restaurant");
        rabbitAdmin.declareExchange(directExchange);
        Queue queue = new Queue("queue.order");
        rabbitAdmin.declareQueue(queue);
        Binding binding = new Binding("queue.order", Binding.DestinationType.QUEUE, "exchange.order.restaurant", "key.order", null);



        rabbitAdmin.declareBinding(binding);
        directExchange = new DirectExchange("exchange.order.deliveryman");
        rabbitAdmin.declareExchange(directExchange);
        queue = new Queue("queue.order");
        rabbitAdmin.declareQueue(queue);
        binding = new Binding("queue.order", Binding.DestinationType.QUEUE, "exchange.order.deliveryman", "key.order", null);
        rabbitAdmin.declareBinding(binding);




        directExchange = new FanoutExchange("exchange.order.settlement");
        rabbitAdmin.declareExchange(directExchange);
        directExchange = new FanoutExchange("exchange.settlement.order");
        rabbitAdmin.declareExchange(directExchange);



        queue = new Queue("queue.order");
        rabbitAdmin.declareQueue(queue);
        binding = new Binding("queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.settlement.order",
                "key.order", null);
        rabbitAdmin.declareBinding(binding);



        //reward
        directExchange = new TopicExchange("exchange.order.reward");
        rabbitAdmin.declareExchange(directExchange);
        binding = new Binding("queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.reward",
                "key.order", null);
        rabbitAdmin.declareBinding(binding);
    }

}