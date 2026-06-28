package com.zhy.zhyBI.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class ChartTaskProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendTask(Long chartId){
        // 同包直接调用MqConfig常量
        rabbitTemplate.convertAndSend(MqConfig.MULTI_EX, MqConfig.MULTI_ROUTE, chartId);
    }
}