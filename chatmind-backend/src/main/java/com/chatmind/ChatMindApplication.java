package com.chatmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChatMind应用启动类
 * AI驱动的脑图产品后端服务
 */
@SpringBootApplication
public class ChatMindApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ChatMindApplication.class, args);
        System.out.println("=========================================");
        System.out.println("ChatMind后端服务启动成功！");
        System.out.println("API文档地址: http://localhost:8080/api/doc.html");
        System.out.println("=========================================");
    }
}
