package com.chatmind.service;

import com.chatmind.dto.RateLimitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 防滥用限流服务
 * 管理配额、限流、成本控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    
    // 默认配额
    private static final int AI_CALL_HOURLY_QUOTA = 20;      // 每小时20次AI调用
    private static final int AI_CALL_DAILY_QUOTA = 100;      // 每天100次
    private static final int EXPORT_HOURLY_QUOTA = 10;       // 每小时10次导出
    private static final int SHARE_DAILY_QUOTA = 50;         // 每天50个分享
    
    /**
     * 检查是否被限流
     */
    public boolean isRateLimited(Long userId, String limitType, String timeWindow) {
        RateLimitVO limit = getRateLimit(userId, limitType, timeWindow);
        return limit.getRateLimited();
    }
    
    /**
     * 获取限流状态
     */
    public RateLimitVO getRateLimit(Long userId, String limitType, String timeWindow) {
        log.debug("获取限流状态: 用户={}, 类型={}, 窗口={}", userId, limitType, timeWindow);
        
        String key = buildKey(userId, limitType, timeWindow);
        
        Integer used = (Integer) redisTemplate.opsForValue().get(key);
        if (used == null) {
            used = 0;
        }
        
        int quota = getQuota(limitType, timeWindow);
        
        RateLimitVO vo = new RateLimitVO();
        vo.setUserId(userId);
        vo.setLimitType(limitType);
        vo.setTimeWindow(timeWindow);
        vo.setQuota(quota);
        vo.setUsed(used);
        vo.setRemaining(Math.max(0, quota - used));
        vo.setRateLimited(used >= quota);
        vo.setResetAt(getResetTime(timeWindow));
        
        return vo;
    }
    
    /**
     * 增加使用次数
     */
    public void incrementUsage(Long userId, String limitType, String timeWindow) {
        log.debug("增加使用: 用户={}, 类型={}", userId, limitType);
        
        String key = buildKey(userId, limitType, timeWindow);
        
        Integer used = (Integer) redisTemplate.opsForValue().get(key);
        
        if (used == null) {
            // 首次使用,设置TTL
            redisTemplate.opsForValue().set(key, 1, getTTL(timeWindow), TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(key);
        }
    }
    
    /**
     * 检查并消费配额
     */
    public boolean checkAndConsume(Long userId, String limitType, String timeWindow) {
        if (isRateLimited(userId, limitType, timeWindow)) {
            log.warn("用户被限流: 用户={}, 类型={}, 窗口={}", userId, limitType, timeWindow);
            return false;
        }
        
        incrementUsage(userId, limitType, timeWindow);
        return true;
    }
    
    /**
     * 重置限流
     */
    public void resetLimit(Long userId, String limitType, String timeWindow) {
        log.info("重置限流: 用户={}, 类型={}, 窗口={}", userId, limitType, timeWindow);
        
        String key = buildKey(userId, limitType, timeWindow);
        redisTemplate.delete(key);
    }
    
    /**
     * 检查AI调用配额
     */
    public boolean checkAIQuota(Long userId) {
        // 检查小时级和天级限制
        boolean hourlyOk = checkAndConsume(userId, "ai_call", "hour");
        if (!hourlyOk) {
            log.warn("AI调用超过小时限制: {}", userId);
            return false;
        }
        
        boolean dailyOk = checkAndConsume(userId, "ai_call", "day");
        if (!dailyOk) {
            log.warn("AI调用超过日限制: {}", userId);
            // 回滚小时计数
            decrementUsage(userId, "ai_call", "hour");
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查导出配额
     */
    public boolean checkExportQuota(Long userId) {
        return checkAndConsume(userId, "export", "hour");
    }
    
    /**
     * 检查分享配额
     */
    public boolean checkShareQuota(Long userId) {
        return checkAndConsume(userId, "share", "day");
    }
    
    /**
     * 减少使用次数(回滚用)
     */
    private void decrementUsage(Long userId, String limitType, String timeWindow) {
        String key = buildKey(userId, limitType, timeWindow);
        
        Integer used = (Integer) redisTemplate.opsForValue().get(key);
        if (used != null && used > 0) {
            redisTemplate.opsForValue().decrement(key);
        }
    }
    
    /**
     * 构建缓存key
     */
    private String buildKey(Long userId, String limitType, String timeWindow) {
        return RATE_LIMIT_PREFIX + userId + ":" + limitType + ":" + timeWindow;
    }
    
    /**
     * 获取配额上限
     */
    private int getQuota(String limitType, String timeWindow) {
        switch (limitType) {
            case "ai_call":
                return "hour".equals(timeWindow) ? AI_CALL_HOURLY_QUOTA : AI_CALL_DAILY_QUOTA;
            case "export":
                return EXPORT_HOURLY_QUOTA;
            case "share":
                return SHARE_DAILY_QUOTA;
            default:
                return 100;
        }
    }
    
    /**
     * 获取TTL(秒)
     */
    private long getTTL(String timeWindow) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reset;
        
        switch (timeWindow) {
            case "hour":
                reset = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
                break;
            case "day":
                reset = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
                break;
            case "month":
                reset = now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                break;
            default:
                reset = now.plusHours(1);
        }
        
        return ChronoUnit.SECONDS.between(now, reset);
    }
    
    /**
     * 获取重置时间戳
     */
    private Long getResetTime(String timeWindow) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reset;
        
        switch (timeWindow) {
            case "hour":
                reset = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
                break;
            case "day":
                reset = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
                break;
            case "month":
                reset = now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                break;
            default:
                reset = now.plusHours(1);
        }
        
        return reset.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
