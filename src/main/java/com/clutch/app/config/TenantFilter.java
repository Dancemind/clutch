package com.clutch.app.config;

import com.clutch.app.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    // todo: для публичных эндпоинтов (без токена)
    //  требуется какой-то дефолтный контекст (например, TenantContext.SYSTEM_UUID)
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            if (!jwtTokenProvider.validateToken(token)) {
                var authException = new BadCredentialsException("Authentication failed");
                this.authenticationEntryPoint.commence(request, response, authException);
                return;
            }
            String tenantId = jwtTokenProvider.getCompanyIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            var authorities = jwtTokenProvider.getAuthoritiesFromToken(token);

            if (tenantId != null) {
                UUID tenantUuid = UUID.fromString(tenantId);

                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                TenantContext.runWithTenant(tenantUuid, () ->
                    executeNextFilters(request, response, filterChain)
                );
                return;
            }
        }

        executeNextFilters(request, response, filterChain);
    }

    private void executeNextFilters(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
