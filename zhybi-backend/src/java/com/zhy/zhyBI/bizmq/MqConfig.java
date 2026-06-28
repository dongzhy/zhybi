package com.zhy.zhyBI.bizmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zhy.zhyBI.bizmq.BiMqConstant.*;

@Configuration
public class MqConfig {

    //多文件分析MQ
    static final String MULTI_EX = "bi_multi_file_exchange";
    static final String MULTI_QUEUE = "bi_multi_file_queue";
    static final String MULTI_ROUTE = "bi.multi.file.task";


    @Bean
    public DirectExchange multiExchange(){
        return new DirectExchange(MULTI_EX,true,false);
    }
    @Bean
    public Queue multiQueue(){
        return new Queue(MULTI_QUEUE,true);
    }
    @Bean
    public Binding multiBind(){
        return BindingBuilder.bind(multiQueue()).to(multiExchange()).with(MULTI_ROUTE);
    }
}
