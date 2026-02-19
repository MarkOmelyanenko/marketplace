package com.example.paymentsservice.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Caches request body for webhook endpoints so signature verification can read it.
 * Sets "rawBody" request attribute.
 */
@Component
@Order(1)
public class RawBodyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (httpRequest.getRequestURI().contains("/webhooks/")) {
            CachedBodyHttpServletRequest cachedBodyRequest = new CachedBodyHttpServletRequest(httpRequest);
            String rawBody = cachedBodyRequest.getCachedBody();
            cachedBodyRequest.setAttribute("rawBody", rawBody);
            chain.doFilter(cachedBodyRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }
    
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private byte[] cachedBody;
        
        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }
        
        public String getCachedBody() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }
        
        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
        
        @Override
        public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(cachedBody);
        }
    }
    
    private static class CachedBodyServletInputStream extends jakarta.servlet.ServletInputStream {
        private final ByteArrayInputStream buffer;
        
        public CachedBodyServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }
        
        @Override
        public int read() throws IOException {
            return buffer.read();
        }
        
        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }
        
        @Override
        public boolean isReady() {
            return true;
        }
        
        @Override
        public void setReadListener(jakarta.servlet.ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
