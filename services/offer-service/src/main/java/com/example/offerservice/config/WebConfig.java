package com.example.offerservice.config;

import com.example.offerservice.interceptor.PartnerIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers PartnerIdInterceptor for /v1/** and excludes /v1/catalog/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final PartnerIdInterceptor partnerIdInterceptor;
    
    public WebConfig(PartnerIdInterceptor partnerIdInterceptor) {
        this.partnerIdInterceptor = partnerIdInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(partnerIdInterceptor)
            .addPathPatterns("/v1/**")
            .excludePathPatterns("/v1/catalog/**");
    }
}
