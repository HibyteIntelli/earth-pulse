package com.earthpulse.www.security;

import com.earthpulse.www.service.JwtService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("no Authorization header: request passes through to chain without setting auth")
    void noHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/watches");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("non-Bearer Authorization header: request passes through to chain")
    void nonBearerHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/watches");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("valid Bearer token: authentication is set in SecurityContext")
    void validToken_authenticationSet() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = "valid.jwt.token";
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/watches");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenReturn(claims);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userId);
    }

    @Test
    @DisplayName("invalid token: returns 401 and does not pass to chain")
    void invalidToken_returns401() throws Exception {
        String token = "bad.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/watches");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenThrow(new JOSEException("Invalid signature"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"error\"");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token (ParseException): returns 401 and does not pass to chain")
    void expiredToken_returns401() throws Exception {
        String token = "expired.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/watches");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenThrow(new ParseException("malformed", 0));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("401 response body contains error field as JSON")
    void invalidToken_responseBodyIsJson() throws Exception {
        String token = "bad.token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/watches");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken(token)).thenThrow(new JOSEException("Bad token"));

        filter.doFilter(request, response, chain);

        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Invalid or expired token");
    }
}
