package com.zhy.zhyBI.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于创建测试程序用到的交换机和队列（程序启动前只执行一次）
 */
public class MqInitMain {
    public static void main(String[] args) {

        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("127.0.0.1");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = "code_exchange";
            //生产者声明了，可无需在消费者声明交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            //创建队列
            String queueName = "code_queue";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, "my_routing_key");

        }catch (Exception e){

        }

    }
}
