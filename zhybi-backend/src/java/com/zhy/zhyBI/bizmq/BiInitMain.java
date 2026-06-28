package com.zhy.zhyBI.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于创建测试程序用到的交换机和队列（程序启动前只执行一次）
 */
public class BiInitMain {
    public static void main(String[] args) {

        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("127.0.0.1");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            //生产者声明了，可无需在消费者声明交换机
            channel.exchangeDeclare(BiMqConstant.BI_EXCHANGE_NAME, "direct");
            //创建队列
            String queueName = BiMqConstant.BI_QUEUE_NAME;
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);

        }catch (Exception e){

        }

    }
}
