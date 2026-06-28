package com.zhy.zhyBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

public class DlxDirectProducer {

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
    private static final String EXCHANGE_NAME = "dlx_direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        //声明死信交换机
        channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");

        //创建队列
        String queueName = "laoban_dlx_queue";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, DEAD_EXCHANGE_NAME, "laoban");

        //创建队列
        String queueName2 = "waibao_dlx_queue";
        channel.queueDeclare(queueName2, true, false, false, null);
        channel.queueBind(queueName2, DEAD_EXCHANGE_NAME, "waibao");


        DeliverCallback deliverCallback3 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [laoban] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback deliverCallback4 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [waibao] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback3, consumerTag -> { });
        channel.basicConsume(queueName2, true, deliverCallback4, consumerTag -> { });

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String userInput = scanner.nextLine();
            String[] strings = userInput.split(" ");
            if (strings.length < 1) {
                continue;
            }
            String message  = strings[0];
            String routingKey = strings[1];
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "with routing："+routingKey+ "'");

        }


    }
  }
  //..
}