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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

            channel.queueDeclare(
                    "queue.restaurant",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.restaurant",
                    "exchange.order.restaurant",
                    "key.restaurant");
            channel.basicQos(2);
            //autoAck=false 关闭自动ack
            channel.basicConsume("queue.restaurant", false, deliverCallback, consumerTag -> {
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
            //AutoClosable //自动关闭连接 ，发送完消息后，会自动关闭连接
//            try (Connection connection = connectionFactory.newConnection();
//                 Channel channel = connection.createChannel()) {
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            //mandatory 当消息无法路由的时候会调用发送方的returnListener
//                channel.addReturnListener(new ReturnListener() {
//                    @Override
//                    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                        System.out.println("===============================================================================================================================================");
//                        log.info("replyCode:{}", replyCode);
//                        log.info("replyText:{}", replyText);
//                        log.info("exchange:{}", exchange);
//                        log.info("routingKey:{}", routingKey);
//                        log.info("properties:{}", properties);
//                        log.info("body:{}", new String(body));
//                        System.out.println("===============================================================================================================================================");
//                    }
//                });
            channel.addReturnListener(new ReturnCallback() {
                @Override
                public void handle(Return returnMessage) {
                    log.info("========================================================================================================================");
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        final byte[] body = returnMessage.getBody();
                        System.out.println(new java.lang.String(body));
                        System.out.println(mapper.writeValueAsString(returnMessage));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    log.info("========================================================================================================================");
                }
            });
            //multiple 签收单条
//            if (message.getEnvelope().getDeliveryTag() % 5 == 0) {
//                channel.basicAck(message.getEnvelope().getDeliveryTag(), true);
//            }

            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            //requeue=true 重回队列
            // channel.basicNack(message.getEnvelope().getDeliveryTag(),false,true);
            //mandatory 当消息无法路由的时候会调用发送方的returnListener
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

