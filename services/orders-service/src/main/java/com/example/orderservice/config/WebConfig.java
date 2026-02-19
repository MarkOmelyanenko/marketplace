package com.example.orderservice.config;

import com.example.orderservice.interceptor.BuyerIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers BuyerIdInterceptor for /v1/** paths.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final BuyerIdInterceptor buyerIdInterceptor;
    
    public WebConfig(BuyerIdInterceptor buyerIdInterceptor) {
        this.buyerIdInterceptor = buyerIdInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(buyerIdInterceptor)
            .addPathPatterns("/v1/**");
    }
}
