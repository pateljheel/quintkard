package io.quintkard.quintkardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "quintkard.security.cors")
public class SecurityCorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:3000");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
