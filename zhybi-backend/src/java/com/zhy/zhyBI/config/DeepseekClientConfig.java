//package com.zhy.zhyBI.config;
//
//import com.tencentcloudapi.common.Credential;
//import com.tencentcloudapi.common.profile.ClientProfile;
//import com.tencentcloudapi.common.profile.HttpProfile;
//import com.tencentcloudapi.lkeap.v20240522.LkeapClient;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@ConfigurationProperties(prefix = "tencent.deepseek.client")
//@Data
//@Slf4j
//@Deprecated
//public class DeepseekClientConfig {
//    /**
//     * 腾讯云账号secretId
//     */
//    private String secretId;
//    /**
//     * 腾讯云账号secretKey
//     */
//    private String secretKey;
//
//    /**
//     * 初始化客户端（修复：添加明确的Region参数）
//     * @return LkeapClient实例
//     */
//    @Bean
//    public LkeapClient deepseekClient() {
//        // 1. 校验密钥
//        if (secretId == null || secretId.trim().isEmpty()) {
//            throw new IllegalArgumentException("腾讯云SecretId未配置，请检查application.yml");
//        }
//        if (secretKey == null || secretKey.trim().isEmpty()) {
//            throw new IllegalArgumentException("腾讯云SecretKey未配置，请检查application.yml");
//        }
//
//        log.info("初始化DeepSeek客户端，SecretId长度：{}，SecretKey长度：{}，地域：{}",
//                secretId.length(),
//                secretKey.length(),
//                "ap-guangzhou");
//
//        // 2. 初始化认证信息
//        Credential cred = new Credential(secretId, secretKey);
//
//        // 3. 设置HttpProfile（广州地域专属端点）
//        HttpProfile httpProfile = new HttpProfile();
//        httpProfile.setEndpoint("lkeap.ap-guangzhou.tencentcloudapi.com");
//        // 可选：设置连接超时和读取超时（单位：秒）
//        httpProfile.setConnTimeout(60);
//        httpProfile.setReadTimeout(60);
//        httpProfile.setWriteTimeout(60);
//
//        // 4. 设置ClientProfile
//        ClientProfile clientProfile = new ClientProfile();
//        clientProfile.setHttpProfile(httpProfile);
//        // 设置签名方法（可选，默认为TC3-HMAC-SHA256）
//        clientProfile.setSignMethod("TC3-HMAC-SHA256");
//
//        // 5. 关键修复：明确设置Region为"ap-guangzhou"
//        LkeapClient client = new LkeapClient(cred, "ap-guangzhou", clientProfile);
//
//        log.info("DeepSeek客户端初始化完成（地域：ap-guangzhou，端点：lkeap.ap-guangzhou.tencentcloudapi.com）");
//        return client;
//    }
//}