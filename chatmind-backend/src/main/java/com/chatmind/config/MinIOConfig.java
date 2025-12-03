package com.chatmind.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO对象存储配置
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinIOConfig {
    
    /**
     * MinIO服务端点
     */
    private String endpoint;
    
    /**
     * 访问密钥
     */
    private String accessKey;
    
    /**
     * 密钥
     */
    private String secretKey;
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * 是否自动创建存储桶
     */
    private Boolean autoCreateBucket = true;
    
    /**
     * 配置MinIO客户端
     */
    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
            
            // 自动创建存储桶
            if (autoCreateBucket) {
                boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
                );
                if (!exists) {
                    client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                    );
                    log.info("MinIO存储桶创建成功: {}", bucket);
                }
            }
            
            log.info("MinIO客户端初始化成功: {}", endpoint);
            return client;
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败", e);
            throw new RuntimeException("MinIO客户端初始化失败", e);
        }
    }
}
