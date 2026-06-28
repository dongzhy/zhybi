package com.zhy.zhyBI.config;

import ai.z.openapi.ZhipuAiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZhipuAiConfig {

    @Value("${zhipu.ai.api-key}")
    private String apiKey;

    @Bean
    public ZhipuAiClient zhipuClient() {
        // 按照官方示例初始化客户端
        return ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .build();
    }
}
