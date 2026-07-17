package ro.hibyte.ingestion.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class InternalSecretFilterTest {

    private static final String SECRET = "s3cret-value";

    @AfterEach
    void clearSecurityContext() {
        // The filter mutates the global SecurityContextHolder — reset it between tests.
        SecurityContextHolder.clearContext();
    }

    @Test
    void constructorRejectsBlankOrNullSecret() {
        assertThatThrownBy(() -> new InternalSecretFilter("   ", new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InternalSecretFilter(null, new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validSecretAuthenticatesAndCallsChain() throws Exception {
        InternalSecretFilter filter = new InternalSecretFilter(SECRET, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("internal");
    }

    @Test
    void wrongSecretReturns401AndDoesNotCallChain() throws Exception {
        InternalSecretFilter filter = new InternalSecretFilter(SECRET, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("unauthorized");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void missingSecretReturns401() throws Exception {
        InternalSecretFilter filter = new InternalSecretFilter(SECRET, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shorterHeaderThanSecretReturns401() throws Exception {
        // Guards the constant-time compare's b[i % b.length] modulo indexing:
        // a header shorter than the secret must NOT throw ArrayIndexOutOfBounds — it just fails to match.
        InternalSecretFilter filter = new InternalSecretFilter("abcdef", new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", "ab");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }
}
