package io.quintkard.quintkardapp.config;

import io.quintkard.quintkardapp.logging.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MessageQueueConfig {

    @Bean(name = "messageQueueTaskExecutor")
    ThreadPoolTaskExecutor messageQueueTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setQueueCapacity(1);
        taskExecutor.setThreadNamePrefix("message-queue-");
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(30);
        taskExecutor.setTaskDecorator(new MdcTaskDecorator());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
