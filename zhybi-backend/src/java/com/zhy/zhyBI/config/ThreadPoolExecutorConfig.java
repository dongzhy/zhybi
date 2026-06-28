package com.zhy.zhyBI.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j // 加日志，方便排查拒绝/执行异常
public class ThreadPoolExecutorConfig {

    @Bean(name = "chartThreadPoolExecutor") // 给Bean命名，避免和其他线程池冲突
    public ThreadPoolExecutor threadPoolExecutor() {
        // 1. 自定义线程工厂：命名更清晰，便于排查
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                // 命名规则：业务标识 + 线程池 + 序号（比如「bi-chart-1」）
                thread.setName("bi-chart-thread-" + count);
                count++;
                // 关键：设置线程为非守护线程，避免主线程退出导致任务中断
                thread.setDaemon(false);
                return thread;
            }
        };

        // 2. 自定义拒绝策略：任务被拒绝时打日志+抛异常（便于发现问题）
        RejectedExecutionHandler rejectedHandler = (r, executor) -> {
            log.error("【BI图表生成】线程池任务被拒绝！当前活跃线程数：{}，队列任务数：{}，已完成任务数：{}",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount());
            // 抛异常（替代默认的AbortPolicy），让上层感知到任务拒绝
            throw new RuntimeException("线程池已满，无法提交新任务：" + r.toString());
        };

        // 3. 核心参数优化
        int corePoolSize = 2; // 核心线程数（根据CPU核心数/业务QPS调整，BI生成任务建议2-4）
        int maxPoolSize = 8; // 最大线程数（核心数的2倍，避免线程过多导致上下文切换）
        long keepAliveTime = 30; // 空闲线程存活时间（30秒，合理值）
        int queueCapacity = 20; // 队列容量（扩大到20，避免小队列快速满）

        // 4. 构建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), // 队列容量扩大
                threadFactory,
                rejectedHandler // 自定义拒绝策略
        );

        // 5. 可选：设置核心线程也允许超时销毁（当任务量长期低迷时释放资源）
        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }
}