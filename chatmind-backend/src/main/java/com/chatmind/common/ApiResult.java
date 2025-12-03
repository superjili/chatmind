package com.chatmind.common;

import lombok.Data;

/**
 * 统一API响应结果封装类
 * 
 * @param <T> 数据类型
 */
@Data
public class ApiResult<T> {
    
    /**
     * 响应码：0-成功，其他-失败
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public ApiResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResult<T> ok() {
        return new ApiResult<>(0, "success", null);
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "success", data);
    }
    
    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> ApiResult<T> ok(String message, T data) {
        return new ApiResult<>(0, message, data);
    }
    
    /**
     * 失败响应（默认消息）
     */
    public static <T> ApiResult<T> fail() {
        return new ApiResult<>(1, "操作失败", null);
    }
    
    /**
     * 失败响应（自定义消息）
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(1, message, null);
    }
    
    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }
    
    /**
     * 失败响应（自定义错误码、消息和数据）
     */
    public static <T> ApiResult<T> fail(Integer code, String message, T data) {
        return new ApiResult<>(code, message, data);
    }
}
