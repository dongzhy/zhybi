package com.zhy.zhyBI.manager;

import com.zhy.zhyBI.common.ErrorCode;
import com.zhy.zhyBI.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 提供redisLimiter限流的基本服务（提供通用能力）
 */

@Service
public class RedisLimiterManage {
    @Resource
    private RedissonClient redissonClient;

    /**
     * 区分不同限流器，比如不同用户id应该分别统计
     * @param key
     */

    public void doRateLimit(String key){
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);
        //每一个操作来了之后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if(!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }
}
