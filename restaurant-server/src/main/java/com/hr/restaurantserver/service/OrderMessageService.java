package com.hr.restaurantserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hr.restaurantserver.dao.ProductDao;
import com.hr.restaurantserver.dao.RestaurantDao;
import com.hr.restaurantserver.dto.OrderMessageDTO;
import com.hr.restaurantserver.enummeration.ProductStatus;
import com.hr.restaurantserver.enummeration.RestaurantStatus;
import com.hr.restaurantserver.po.ProductPO;
import com.hr.restaurantserver.po.RestaurantPO;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProductDao productDao;
    @Autowired
    RestaurantDao restaurantDao;

    Channel channel;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            this.channel = channel;
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);
            //设置队列的参数
            Map<String, Object> args = new HashMap<>(16);
            args.put("x-message-ttl", 15000);
            channel.queueDeclare(
                    "queue.restaurant",
                    true,
                    false,
                    false,
                    args);

            channel.queueBind(
                    "queue.restaurant",
                    "exchange.order.restaurant",
                    "key.restaurant");
            channel.basicConsume("queue.restaurant", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(100);
            }
        }
    }


    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new java.lang.String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);

            ProductPO productPO = productDao.selsctProduct(orderMessageDTO.getProductId());
            log.info("onMessage:productPO:{}", productPO);
            RestaurantPO restaurantPO = restaurantDao.selsctRestaurant(productPO.getRestaurantId());
            log.info("onMessage:restaurantPO:{}", restaurantPO);
            System.out.println(ProductStatus.AVALIABIE.name());
            System.out.println(productPO.getStatus());
            System.out.println(RestaurantStatus.OPEN.name());
            System.out.println(restaurantPO.getStatus());
            System.out.println(ProductStatus.AVALIABIE.name().toString().equals(productPO.getStatus()));
            System.out.println(RestaurantStatus.OPEN.name().toString().equals(restaurantPO.getStatus()));
            if (ProductStatus.AVALIABIE.name().equals(productPO.getStatus()) && RestaurantStatus.OPEN.name().equals(restaurantPO.getStatus())) {
                orderMessageDTO.setConfirmed(true);
                orderMessageDTO.setPrice(productPO.getPrice());
            } else {
                orderMessageDTO.setConfirmed(false);
            }
            log.info("sendMessage:restaurantOrderMessageDTO:{}", orderMessageDTO);
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);

            channel.basicPublish("exchange.order.restaurant", "key.order", true, null, messageToSend.getBytes());
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    };
}

