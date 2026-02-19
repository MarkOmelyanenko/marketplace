package com.example.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "requestsPerMinute", 2);
        ReflectionTestUtils.setField(filter, "capacity", 2);
    }

    @Test
    void whenUnderLimit_continuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/offers/v1/offers");
        req.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    void whenOverLimit_returns429() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/offers/v1/offers");
        req.setRemoteAddr("10.0.0.1");

        // first two requests succeed (each needs its own chain and response)
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(req, res1, new MockFilterChain());
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req, res2, new MockFilterChain());

        // third request is rate limited
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        filter.doFilter(req, res3, new MockFilterChain());

        assertThat(res3.getStatus()).isEqualTo(429);
        assertThat(res3.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void whenDisabled_shouldNotFilter() {
        ReflectionTestUtils.setField(filter, "enabled", false);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders/v1/orders");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void actuatorPath_shouldNotFilter() {
        ReflectionTestUtils.setField(filter, "enabled", true);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }
}
