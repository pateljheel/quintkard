package io.quintkard.quintkardapp.config;

import io.quintkard.quintkardapp.logging.HttpRequestLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcLoggingConfig implements WebMvcConfigurer {

    private final HttpRequestLoggingInterceptor httpRequestLoggingInterceptor;

    public WebMvcLoggingConfig(HttpRequestLoggingInterceptor httpRequestLoggingInterceptor) {
        this.httpRequestLoggingInterceptor = httpRequestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpRequestLoggingInterceptor);
    }
}
