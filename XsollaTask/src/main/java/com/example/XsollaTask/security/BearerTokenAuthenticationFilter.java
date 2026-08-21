package com.example.XsollaTask.security;

import com.example.XsollaTask.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;


public final class BearerTokenAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final byte[] expectedToken;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationFilter(
            SecurityProperties securityProperties,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.expectedToken = securityProperties.bearerToken()
                .getBytes(StandardCharsets.UTF_8);
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return !path.equals("/v1") && !path.startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String presentedToken = extractBearerToken(authorization);

        if (presentedToken == null || !tokenMatches(presentedToken)) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid bearer token")
            );
            return;
        }

        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "api-client",
                        null,
                        List.of()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String authorization) {
        if (authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(
                true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length()
        )) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        return token.isBlank() ? null : token;
    }

    private boolean tokenMatches(String presentedToken) {
        return MessageDigest.isEqual(
                expectedToken,
                presentedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}