package com.hr.orderserver.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.hr.orderserver.dao.OrderDetailDao;
import com.hr.orderserver.dto.OrderMessageDTO;
import com.hr.orderserver.enums.OrderStatus;
import com.hr.orderserver.po.OrderDetailPO;
import com.hr.orderserver.vo.OrderCreateVO;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderDetailDao orderDetailDao;
    @Value("${rabbitmq.exchange}")
    public String exchangeName;
    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;
    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();
    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        log.info("createOrder:orderCreateVO:{}", orderCreateVO);
        OrderDetailPO orderPO = new OrderDetailPO();
        orderPO.setAddress(orderCreateVO.getAddress());
        orderPO.setAccountId(orderCreateVO.getAccountId());
        orderPO.setProductId(orderCreateVO.getProductId());
        orderPO.setStatus(OrderStatus.ORDER_CREATING.name());
        orderPO.setDate(new Date());
        orderDetailDao.insert(orderPO);
        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderPO.getId());
        orderMessageDTO.setProductId(orderPO.getProductId());
        orderMessageDTO.setAccountId(orderCreateVO.getAccountId());
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.confirmSelect();
            //异步确认
            ConfirmListener confirmListener = new ConfirmListener() {
                @Override
                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                    //deliveryTag 发送端的消息序号
                    //multiple 如果multiple是true，在确认多条消息，在确认单条消息
                    log.info("ACK  deliveryTag:{},multiple:{}", deliveryTag, multiple);
                }
                @Override
                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                    //deliveryTag 发送端的消息序号
                    //multiple 如果multiple是true，在确认多条消息，在确认单条消息
                    log.info("NACK  deliveryTag:{},multiple:{}", deliveryTag, multiple);
                }
            };
                channel.addConfirmListener(confirmListener);
            for (int i = 0; i < 50; i++) {
                channel.basicPublish("exchange.order.restaurant", "key.restaurant", null, messageToSend.getBytes());
                log.info("========= send");
            }

//                if (channel.waitForConfirms()) {
//                    log.info("=============>>>> RabbitMQ Confirms success");
//                } else {
//                    log.info("=============>>>> RabbitMQ Confirms failed");
//                }
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
