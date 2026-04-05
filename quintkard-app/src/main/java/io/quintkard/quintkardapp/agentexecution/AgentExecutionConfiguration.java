package io.quintkard.quintkardapp.agentexecution;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentExecutionProperties.class)
public class AgentExecutionConfiguration {
}
