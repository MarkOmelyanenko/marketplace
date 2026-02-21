package com.example.offerservice.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Requires Partner-Id header on protected paths. Skips actuator, swagger, api-docs. Sets partnerId request attribute.
 */
@Component
public class PartnerIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS preflight (OPTIONS) without Partner-Id so the actual request can be sent with the header
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        String partnerId = request.getHeader("Partner-Id");
        if (partnerId == null || partnerId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Partner-Id header is required\"}");
            return false;
        }
        request.setAttribute("partnerId", partnerId);
        return true;
    }
}
