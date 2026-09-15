package com.logistics.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
// Add this replacement line instead
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.List;

/**
 * Verifies X-API-Key for the two external-caller boundaries (webhook
 * ingestion, agent tools). Runs before JwtAuthenticationFilter but is a
 * no-op for any other path - human/user JWT auth is untouched.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.security.webhook-api-key:}")
    private String webhookApiKey;

    @Value("${app.security.agent-api-key:}")
    private String agentApiKey;

    private static final String HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String providedKey = request.getHeader(HEADER);

        if (path.startsWith("/api/v1/events/") && matchesConfigured(providedKey, webhookApiKey)) {
            authenticateAs("ROLE_WEBHOOK");
        } else if (path.startsWith("/api/v1/agent-tools/") && matchesConfigured(providedKey, agentApiKey)) {
            authenticateAs("ROLE_AGENT");
        }
        // No match: leave context unauthenticated. SecurityConfig's
        // authorization rules (not this filter) decide the resulting
        // 401/403 - this filter's only job is to grant identity when a
        // valid key is presented, never to reject directly.

        filterChain.doFilter(request, response);
    }

    private boolean matchesConfigured(String provided, String configured) {
        // Empty configured key = feature not set up yet -> never authenticates.
        // Prevents an accidental "empty key matches empty header" bypass.
        return provided != null && !configured.isBlank() && provided.equals(configured);
    }

    private void authenticateAs(String role) {
        var authToken = new UsernamePasswordAuthenticationToken(
                role, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}