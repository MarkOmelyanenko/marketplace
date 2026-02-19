package com.example.paymentsservice.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Requires Partner-Id or Buyer-Id header. Skips /webhooks/ and /internal/. Sets partnerId/buyerId and ownerType/ownerId.
 */
@Component
public class PartnerIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.contains("/webhooks/") || path.startsWith("/internal/")) {
            return true;
        }
        
        String partnerId = request.getHeader("Partner-Id");
        String buyerId = request.getHeader("Buyer-Id");
        
        if (partnerId != null && !partnerId.isBlank()) {
            request.setAttribute("partnerId", partnerId);
            request.setAttribute("ownerType", "PARTNER");
            request.setAttribute("ownerId", partnerId);
            return true;
        } else if (buyerId != null && !buyerId.isBlank()) {
            request.setAttribute("buyerId", buyerId);
            request.setAttribute("ownerType", "BUYER");
            request.setAttribute("ownerId", buyerId);
            return true;
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"Partner-Id or Buyer-Id header is required\"}");
            return false;
        }
    }
}
