package com.chatmind.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端配置
 * 配置全局超时设置,用于Spring AI和其他HTTP调用
 */
@Configuration
public class HttpClientConfig {
    
    @Value("${spring.ai.openai.init.timeout:300000}")
    private long timeoutMs;
    
    /**
     * 配置HttpClient,设置连接超时和响应超时
     */
    @Bean
    public HttpClient httpClient() {
        // 连接池管理器
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);
        
        // 请求配置
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.of(timeoutMs, TimeUnit.MILLISECONDS))
            .setResponseTimeout(Timeout.of(timeoutMs, TimeUnit.MILLISECONDS))
            .build();
        
        // 构建HttpClient
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(config)
            .evictExpiredConnections()
            .evictIdleConnections(Timeout.of(60, TimeUnit.SECONDS))
            .build();
    }
    
    /**
     * 配置RestTemplate,使用自定义HttpClient
     */
    @Bean
    public RestTemplate restTemplate(HttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}
