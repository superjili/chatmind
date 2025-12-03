package com.chatmind.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * AI模型服务
 * 封装Spring AI的ChatClient,提供统一的LLM调用接口
 */
@Slf4j
@Service
public class ChatModelService {
    
    private final ChatClient chatClient;
    
    public ChatModelService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    
    /**
     * 生成文本响应
     * 
     * @param prompt 用户提示词
     * @return LLM生成的文本
     */
    public String generateText(String prompt) {
        try {
            log.debug("发送Prompt到LLM: {}", prompt);
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            log.debug("LLM响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成结构化响应(带完整ChatResponse)
     * 
     * @param prompt 用户提示词
     * @return ChatResponse对象
     */
    public ChatResponse generateResponse(String prompt) {
        try {
            log.debug("发送Prompt到LLM: {}", prompt);
            ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();
            log.debug("LLM响应完成,token使用情况: {}", response.getMetadata());
            return response;
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用自定义Prompt对象生成
     * 
     * @param prompt Prompt对象
     * @return 生成的文本
     */
    public String generate(Prompt prompt) {
        try {
            log.debug("使用自定义Prompt调用LLM");
            String response = chatClient.prompt(prompt)
                .call()
                .content();
            log.debug("LLM响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式生成(适用于长文本生成)
     * 
     * @param prompt 用户提示词
     * @return 流式响应
     */
    public String generateStream(String prompt) {
        try {
            log.debug("启动流式生成: {}", prompt);
            StringBuilder result = new StringBuilder();
            chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(result::append)
                .blockLast();
            log.debug("流式生成完成,总长度: {}", result.length());
            return result.toString();
        } catch (Exception e) {
            log.error("流式生成失败", e);
            throw new RuntimeException("AI流式生成失败: " + e.getMessage(), e);
        }
    }
}
