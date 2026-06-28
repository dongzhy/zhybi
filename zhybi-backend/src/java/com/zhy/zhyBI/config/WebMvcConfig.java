package com.zhy.zhyBI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Bean
    public SimpleUrlHandlerMapping spaForwardMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        // 顺序放低，优先匹配Controller接口
        mapping.setOrder(Integer.MAX_VALUE - 1);

        ResourceHttpRequestHandler indexHandler = new ResourceHttpRequestHandler();
        indexHandler.setLocations(Collections.singletonList(new ClassPathResource("static/")));
        // 删掉爆红的 setPathMatcher 行
        Map<String, ResourceHttpRequestHandler> urlMap = new HashMap<>();
        urlMap.put("/share/**", indexHandler);
        mapping.setUrlMap(urlMap);
        return mapping;
    }
}