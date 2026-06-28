package com.zhy.zhyBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class SingleProducer {
    private final static String QUEUE_NAME = "hello";


    public static void main(String[] args) throws Exception {

        //创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        //建立连接，创建评到
        try(
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            //创建消息队列
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                //发送消息
                String message = "hello,World!";
                channel.basicPublish("","hello",null,message.getBytes(StandardCharsets.UTF_8));
                System.out.println("[x] Sent '" + message + "'");

            }
        }

    }
