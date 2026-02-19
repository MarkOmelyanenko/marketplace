package com.example.paymentsservice.config;

import com.example.paymentsservice.interceptor.PartnerIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final PartnerIdInterceptor partnerIdInterceptor;
    
    public WebConfig(PartnerIdInterceptor partnerIdInterceptor) {
        this.partnerIdInterceptor = partnerIdInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(partnerIdInterceptor);
    }
}
