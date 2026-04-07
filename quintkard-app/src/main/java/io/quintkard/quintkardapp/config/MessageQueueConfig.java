package io.quintkard.quintkardapp.config;

import io.quintkard.quintkardapp.logging.MdcTaskDecorator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueConfig {

    @Bean(name = "messageQueueTaskExecutor")
    ThreadPoolTaskExecutor messageQueueTaskExecutor(MessageQueueProperties messageQueueProperties) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(messageQueueProperties.getCorePoolSize());
        taskExecutor.setMaxPoolSize(messageQueueProperties.getMaxPoolSize());
        taskExecutor.setQueueCapacity(messageQueueProperties.getQueueCapacity());
        taskExecutor.setThreadNamePrefix("message-queue-");
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(messageQueueProperties.getAwaitTerminationSeconds());
        taskExecutor.setTaskDecorator(new MdcTaskDecorator());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
