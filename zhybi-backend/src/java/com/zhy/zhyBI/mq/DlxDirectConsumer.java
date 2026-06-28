package com.zhy.zhyBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {
    private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";

  private static final String EXCHANGE_NAME = "dlx_direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    //生产者声明了，可无需在消费者声明交换机
    channel.exchangeDeclare(EXCHANGE_NAME, "direct");
      //创建消息队列,指定消息过期参数
      //指定死信队列参数
      Map<String, Object> args = new HashMap<String, Object>();
      //要绑定到哪个交换机
      args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
      //指定死信要发到哪个队列
      args.put("x-dead-letter-routing-key", "waibao");
    //创建队列
    String queueName = "xiaodog_queue";
    channel.queueDeclare(queueName, true, false, false, args);
    channel.queueBind(queueName, EXCHANGE_NAME, "xiaodog");

      //创建消息队列,指定消息过期参数
      Map<String, Object> args2 = new HashMap<String, Object>();
      args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
      args2.put("x-dead-letter-routing-key", "laoban");

      String queueName2 = "xiaocat_queue";
      channel.queueDeclare(queueName2, true, false, false, args2);
      channel.queueBind(queueName2, EXCHANGE_NAME, "xiaocat");



    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        System.out.println(" [小dog] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
      DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
          System.out.println(" [小yong] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
    channel.basicConsume(queueName, true, deliverCallback1, consumerTag -> { });
    channel.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });




  }
}