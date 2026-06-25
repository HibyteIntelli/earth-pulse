package com.earthpulse.www.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RateLimitFilterTest {

    private static final String LOGIN_PATH  = "/auth/login";
    private static final String SIGNUP_PATH = "/auth/signup";
    private static final String CLIENT_IP   = "10.0.0.1";

    private RateLimitFilter filter;
    private FilterChain     chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(10, 5);
        chain  = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Login: first 10 requests from same IP are allowed")
    void login_allowsUpToCapacity() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = doRequest(LOGIN_PATH, CLIENT_IP);
            assertThat(response.getStatus())
                    .as("request %d should pass", i + 1)
                    .isEqualTo(200);
        }
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Login: 11th request from same IP returns 429")
    void login_blocksAfterCapacityExceeded() throws Exception {
        exhaustBucket(LOGIN_PATH, CLIENT_IP, 10);

        MockHttpServletResponse response = doRequest(LOGIN_PATH, CLIENT_IP);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Login: 429 response includes Retry-After header")
    void login_blockedResponseIncludesRetryAfterHeader() throws Exception {
        exhaustBucket(LOGIN_PATH, CLIENT_IP, 10);

        MockHttpServletResponse response = doRequest(LOGIN_PATH, CLIENT_IP);

        assertThat(response.getHeader("X-Rate-Limit-Retry-After-Seconds")).isNotNull();
    }

    @Test
    @DisplayName("Login: 429 response has JSON error body")
    void login_blockedResponseHasJsonBody() throws Exception {
        exhaustBucket(LOGIN_PATH, CLIENT_IP, 10);

        MockHttpServletResponse response = doRequest(LOGIN_PATH, CLIENT_IP);

        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"error\"");
    }

    @Test
    @DisplayName("Login: remaining-tokens header decreases with each request")
    void login_remainingTokensHeaderDecreases() throws Exception {
        MockHttpServletResponse first  = doRequest(LOGIN_PATH, CLIENT_IP);
        MockHttpServletResponse second = doRequest(LOGIN_PATH, CLIENT_IP);

        int remainingAfterFirst  = Integer.parseInt(first.getHeader("X-Rate-Limit-Remaining"));
        int remainingAfterSecond = Integer.parseInt(second.getHeader("X-Rate-Limit-Remaining"));

        assertThat(remainingAfterSecond).isLessThan(remainingAfterFirst);
    }

    @Test
    @DisplayName("Signup: first 5 requests from same IP are allowed")
    void signup_allowsUpToCapacity() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = doRequest(SIGNUP_PATH, CLIENT_IP);
            assertThat(response.getStatus())
                    .as("request %d should pass", i + 1)
                    .isEqualTo(200);
        }
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Signup: 6th request from same IP returns 429")
    void signup_blocksAfterCapacityExceeded() throws Exception {
        exhaustBucket(SIGNUP_PATH, CLIENT_IP, 5);

        MockHttpServletResponse response = doRequest(SIGNUP_PATH, CLIENT_IP);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Different IPs have independent buckets for login")
    void login_differentIpsHaveIndependentBuckets() throws Exception {
        exhaustBucket(LOGIN_PATH, "1.1.1.1", 10);

        MockHttpServletResponse response = doRequest(LOGIN_PATH, "2.2.2.2");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Login bucket exhaustion does not affect signup bucket for same IP")
    void login_exhaustionDoesNotAffectSignupBucket() throws Exception {
        exhaustBucket(LOGIN_PATH, CLIENT_IP, 10);

        MockHttpServletResponse response = doRequest(SIGNUP_PATH, CLIENT_IP);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("JWKS endpoint is never rate limited")
    void jwks_passesThrough() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doRequest("/.well-known/jwks.json", CLIENT_IP);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(chain, times(20)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Account endpoints are never rate limited")
    void account_passesThrough() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doRequest("/account/me", CLIENT_IP);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(chain, times(20)).doFilter(any(), any());
    }

    @Test
    @DisplayName("X-Forwarded-For header is used as the rate-limit key")
    void login_xForwardedForIsUsedAsKey() throws Exception {
        for (int i = 0; i < 10; i++) {
            doRequestXff(LOGIN_PATH, "203.0.113.5, 10.0.0.1");
        }

        MockHttpServletResponse response = doRequestXff(LOGIN_PATH, "203.0.113.5, 10.0.0.1");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Different XFF IPs are treated as independent clients")
    void login_differentXffIpsAreIndependent() throws Exception {
        for (int i = 0; i < 10; i++) {
            doRequestXff(LOGIN_PATH, "203.0.113.1");
        }

        MockHttpServletResponse response = doRequestXff(LOGIN_PATH, "203.0.113.2");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // helpers

    private MockHttpServletResponse doRequest(String path, String remoteAddr)
            throws ServletException, IOException {
        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr(remoteAddr);
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletResponse doRequestXff(String path, String xffValue)
            throws ServletException, IOException {
        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("10.0.0.99");
        request.addHeader("X-Forwarded-For", xffValue);
        filter.doFilter(request, response, chain);
        return response;
    }

    private void exhaustBucket(String path, String ip, int times)
            throws ServletException, IOException {
        for (int i = 0; i < times; i++) {
            doRequest(path, ip);
        }
    }
}
