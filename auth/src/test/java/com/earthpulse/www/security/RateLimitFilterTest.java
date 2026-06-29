package com.earthpulse.www.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;

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
        filter = new RateLimitFilter(10, 5, Duration.ofMinutes(1));
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
    @DisplayName("X-Forwarded-For header is ignored — remoteAddr is used as key")
    void login_xForwardedForIsIgnored() throws Exception {
        exhaustBucket(LOGIN_PATH, CLIENT_IP, 10);

        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", LOGIN_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr(CLIENT_IP);
        request.addHeader("X-Forwarded-For", "9.9.9.9");
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Retry-After is at least 1 second when rate limited")
    void login_retryAfterIsAtLeastOne() throws Exception {
        exhaustBucket(LOGIN_PATH, CLIENT_IP, 10);

        MockHttpServletResponse response = doRequest(LOGIN_PATH, CLIENT_IP);

        long retryAfter = Long.parseLong(response.getHeader("X-Rate-Limit-Retry-After-Seconds"));
        assertThat(retryAfter).isGreaterThanOrEqualTo(1);
    }

    private MockHttpServletResponse doRequest(String path, String remoteAddr)
            throws ServletException, IOException {
        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr(remoteAddr);
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
