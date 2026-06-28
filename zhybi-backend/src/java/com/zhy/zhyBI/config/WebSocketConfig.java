package com.zhy.zhyBI.config;

import com.zhy.zhyBI.manager.ChartWebSocketServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.annotation.Resource;

@Configuration
public class WebSocketConfig {
    @Resource
    private ChartWebSocketServer chartWebSocketServer;

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        // 关键：手动注册，防止 Spring 扫描失败
        exporter.setAnnotatedEndpointClasses(ChartWebSocketServer.class);
        return exporter;
    }
}