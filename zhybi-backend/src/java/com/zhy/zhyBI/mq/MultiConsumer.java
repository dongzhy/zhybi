package com.zhy.zhyBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

  private static final String TASK_QUEUE_NAME = "multi_queue";

  public static void main(String[] argv) throws Exception {
      //建立连接
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    final Connection connection = factory.newConnection();
      for (int i = 0; i < 2 ; i++) {
          final Channel channel = connection.createChannel();
          //声明队列

          channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
          System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
          //控制单个消费者的处理任务数【积压数】：
          //每个消费者最多处理1个任务
          channel.basicQos(1);
          //定义了如何处理消息
          int finalI = i;
          DeliverCallback deliverCallback = (consumerTag, delivery) -> {
              String message = new String(delivery.getBody(), "UTF-8");

              try {
                  System.out.println(" [x] Received '" +"编号："+ finalI +":"+ message + "'");
                  //20s模拟机器处理能力有限
                  Thread.sleep(20000);
                  channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
              } catch (InterruptedException e) {
                  e.printStackTrace();
                  channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
              } finally {
                  System.out.println(" [x] Done");
                  channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
              }
          };
          //开启消费监听
          channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> { });

      }
  }


}