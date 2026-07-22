package com.essenza.sms_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración para procesamiento asíncrono de tareas
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor específico para el procesamiento de flujos de notificación
     * Configurado para no bloquear el flujo principal de notificaciones
     */
    @Bean(name = "notificationFlowTaskExecutor")
    public Executor notificationFlowTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración optimizada para flujo de notificaciones
        executor.setCorePoolSize(2);           // Mínimo 2 threads
        executor.setMaxPoolSize(5);             // Máximo 5 threads concurrentes
        executor.setQueueCapacity(100);         // Cola de 100 tareas pendientes
        executor.setThreadNamePrefix("notification-flow-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // No esperar a que las tareas terminen al cerrar la aplicación
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(1);
        
        executor.initialize();
        return executor;
    }

    /**
     * Executor general para otras tareas asíncronas si se necesita
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
    }
}
