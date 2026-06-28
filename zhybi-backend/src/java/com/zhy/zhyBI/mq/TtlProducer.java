package com.zhy.zhyBI.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class TtlProducer {
    private final static String QUEUE_NAME = "ttl_queue";


    public static void main(String[] args) throws Exception {

        //创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        //建立连接，创建评到
        try(
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
                //发送消息
                String message = "hello,World!";
            byte[] messageBodyBytes = "Hello, world!".getBytes();
            //给消息指定过期时间
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .expiration("1000")
                    .build();
                channel.basicPublish("","hello",properties,message.getBytes(StandardCharsets.UTF_8));
                System.out.println("[x] Sent '" + message + "'");

            }
        }

    }
