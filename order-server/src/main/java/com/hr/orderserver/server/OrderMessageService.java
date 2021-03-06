package com.hr.orderserver.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hr.orderserver.dao.OrderDetailDao;
import com.hr.orderserver.dto.OrderMessageDTO;
import com.hr.orderserver.enums.OrderStatus;
import com.hr.orderserver.po.OrderDetailPO;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {

    @Value("${rabbitmq.exchange}")
    public String exchangeName;
    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;
    @Value("${rabbitmq.settlement-routing-key}")
    public String settlementRoutingKey;
    @Value("${rabbitmq.reward-routing-key}")
    public String rewardRoutingKey;


    @Autowired
    private OrderDetailDao orderDetailDao;
    ObjectMapper objectMapper = new ObjectMapper();


    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            /*---------------------restaurant 商家---------------------*/
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            channel.queueDeclare(
                    "queue.order",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.restaurant",
                    "key.order");


            /*---------------------deliveryman 骑手---------------------*/
            channel.exchangeDeclare(
                    "exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);


            channel.queueBind(
                    "queue.order",
                    "exchange.order.deliveryman",
                    "key.order");

            /*---------------------settlement 结算---------------------*/

            channel.exchangeDeclare(
                    "exchange.settlement.order",
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.settlement.order",
                    "key.order");

            /*---------------------reward 积分---------------------*/

            channel.exchangeDeclare(
                    "exchange.order.reward",
                    BuiltinExchangeType.TOPIC,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.reward",
                    "key.order");

            channel.basicConsume("queue.order", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(100000);
            }
        }
    }


    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);
            OrderDetailPO orderPO = orderDetailDao.selectOrder(orderMessageDTO.getOrderId());
            log.info("order-server 收到消息为:{}",orderPO.toString());
            switch (orderPO.getStatus()) {
                case "ORDER_CREATING":
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED.name());
                        orderPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.deliveryman", "key.deliveryman", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED.name());
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case "RESTAURANT_CONFIRMED":
                    if (null != orderMessageDTO.getDeliverymanId()) {
                        orderPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED.name());
                        orderPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.settlement", "key.settlement", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED.name());
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case "DELIVERYMAN_CONFIRMED":
                    if (null != orderMessageDTO.getSettlementId()) {
                        orderPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED.name());
                        orderPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.reward", "key.reward", null, messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED.name());
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case "SETTLEMENT_CONFIRMED":
                    if (null != orderMessageDTO.getRewardId()) {
                        orderPO.setStatus(OrderStatus.ORDER_CREATED.name());
                        orderPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailDao.update(orderPO);
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED.name());
                        orderDetailDao.update(orderPO);
                    }
                    break;
            }

        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    };
}
