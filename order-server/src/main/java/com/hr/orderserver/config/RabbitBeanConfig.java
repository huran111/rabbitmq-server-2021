package com.hr.orderserver.config;

import com.hr.orderserver.server.OrderMessageService;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @program: rabbit-mq-server
 * @description: spring boot 配置
 * @author: HuRan
 * @create: 2021-01-19 15:27
 */
@Configuration
public class RabbitBeanConfig {
    @Resource
    OrderMessageService orderMessageService;

    @Autowired
    public void startListenMessage() throws InterruptedException, TimeoutException, IOException {
        orderMessageService.handleMessage();
    }

    @Bean
    public Exchange exchange01() { //餐厅微服务
        return new DirectExchange("exchange.order.restaurant");
    }

    @Bean
    public Queue queue01() {
        return new Queue("queue.order");
    }

    @Bean
    public Binding binding01() {
        return new Binding(
                "queue.order", Binding.DestinationType.QUEUE,
                "exchange.order.restaurant",
                "key.order",
                null);
    }
    //==================deliveryman=======

    @Bean
    public Exchange exchange02() {
        return new DirectExchange("exchange.order.deliveryman");
    }

    @Bean
    public Binding binding02() {
        return new Binding(
                "queue.order", Binding.DestinationType.QUEUE,
                "exchange.order.deliveryman",
                "key.order",
                null);
    }

    //=============settlement=========
    @Bean
    public Exchange exchange03() {
        return new FanoutExchange("exchange.order.settlement");
    }

    @Bean
    public Exchange exchange04() {
        return new FanoutExchange("exchange.settlement.order");
    }

    @Bean
    public Binding binding03() {
        return new Binding(
                "queue.order", Binding.DestinationType.QUEUE,
                "exchange.order.settlement",
                "key.order",
                null);
    }

    //=============reward==========
    @Bean
    public Exchange exchange05() {
        return new TopicExchange("exchange.order.reward");
    }

    @Bean
    public Binding binding04() {
        return new Binding(
                "queue.order", Binding.DestinationType.QUEUE,
                "exchange.order.reward",
                "key.order",
                null);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        // ====用一下
      //  connectionFactory.createConnection();
        return connectionFactory;
    }
    //ApplicationContextAware 获取spring 的上下文
    //afterPropertiesSet 导致bean在spring处理好之后调用
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        //连接被使用才会创建
        final RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }
}