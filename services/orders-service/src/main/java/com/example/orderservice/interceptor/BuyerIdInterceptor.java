package com.example.orderservice.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Requires Buyer-Id header on /v1/** except actuator, swagger, api-docs, and /internal.
 * Sets buyerId request attribute for controllers.
 */
@Component
public class BuyerIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/internal")) {
            return true;
        }
        String buyerId = request.getHeader("Buyer-Id");
        if (buyerId == null || buyerId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Buyer-Id header is required\"}");
            return false;
        }
        request.setAttribute("buyerId", buyerId);
        return true;
    }
}
