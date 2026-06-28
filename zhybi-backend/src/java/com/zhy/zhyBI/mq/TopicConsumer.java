package com.zhy.zhyBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class TopicConsumer {

  private static final String EXCHANGE_NAME = "topic_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    //声明交换机

    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
      //创建队列
      String queueName = "前端队列";
      channel.queueDeclare(queueName, true, false, false, null);
      channel.queueBind(queueName, EXCHANGE_NAME, "#.前端.#");

      String queueName2 = "后端队列";
      channel.queueDeclare(queueName2, true, false, false, null);
      channel.queueBind(queueName2, EXCHANGE_NAME, "#.后端.#");

      String queueName3 = "产品队列";
      channel.queueDeclare(queueName3, true, false, false, null);
      channel.queueBind(queueName3, EXCHANGE_NAME, "#.产品.#");



      DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [小A] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
      DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [小B] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
      DeliverCallback deliverCallback3 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [小C] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
      channel.basicConsume(queueName, true, deliverCallback1, consumerTag -> { });
      channel.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });
      channel.basicConsume(queueName3, true, deliverCallback3, consumerTag -> { });
  }
}