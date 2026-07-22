package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración de Thread Pools para SMS
 */
@Slf4j
@Configuration("smsAsyncConfiguration")
@EnableAsync
public class AsyncConfiguration {
    
    @Value("${sms.async.core-pool-size:5}")
    private int corePoolSize;
    
    @Value("${sms.async.max-pool-size:20}")
    private int maxPoolSize;
    
    @Value("${sms.async.queue-capacity:500}")
    private int queueCapacity;
    
    @Value("${sms.async.thread-name-prefix:sms-async-}")
    private String threadNamePrefix;
    
    /**
     * Thread pool dedicado para envío de SMS
     * 
     * Configuración más conservadora que email:
     * - Core threads: 5
     * - Max threads: 20
     * - Queue: 500
     * 
     * Razón: SMS suele tener límites de tasa más estrictos
     */
    @Bean(name = "smsTaskExecutor")
    public Executor smsTaskExecutor() {
        log.info("[AsyncConfig] Configuring SMS thread pool: core={}, max={}, queue={}",
            corePoolSize, maxPoolSize, queueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        executor.initialize();
        
        log.info("[AsyncConfig] ✅ SMS thread pool configured successfully");
        return executor;
    }
}
