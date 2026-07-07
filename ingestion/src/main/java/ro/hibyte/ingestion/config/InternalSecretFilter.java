package ro.hibyte.ingestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class InternalSecretFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Secret";

    private final String internalSecret;

    public InternalSecretFilter(String internalSecret) {
        if (internalSecret == null || internalSecret.isBlank()) {
            throw new IllegalArgumentException("ingestion.internal-secret must not be blank");
        }
        this.internalSecret = internalSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String secret = request.getHeader(HEADER);

        if (constantTimeEquals(internalSecret, secret)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("internal", null, List.of())
            );
            chain.doFilter(request, response);
        } else {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid internal secret\"}");
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);

        int result = a.length ^ b.length;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i % b.length];
        }
        return result == 0;
    }
}
