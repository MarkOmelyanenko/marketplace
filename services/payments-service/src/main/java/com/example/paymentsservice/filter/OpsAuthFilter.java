package com.example.paymentsservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Requires valid Ops-Token header on /internal/** paths. Used for internal ops API.
 */
@Component
public class OpsAuthFilter extends OncePerRequestFilter {

    private static final String OPS_TOKEN_HEADER = "Ops-Token";
    private static final String VALID_OPS_TOKEN = "ops-dev";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/internal/")) {
            String opsToken = request.getHeader(OPS_TOKEN_HEADER);
            
            if (opsToken == null || !VALID_OPS_TOKEN.equals(opsToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"Invalid or missing Ops-Token header\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
