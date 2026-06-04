package com.clutch.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Company-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Пытаемся достать ID компании из заголовка
        String headerValue = request.getHeader(TENANT_HEADER);
        UUID companyUuid = null;

        try {
            if (headerValue != null && !headerValue.isBlank()) {
                companyUuid = UUID.fromString(headerValue);
            }
        } catch (NumberFormatException e) {
            // если прислали не число — отдаем 400 Bad Request
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Company-ID format");
            return;
        }

        if (companyUuid != null) {
            // 2. Java 25 Magic: привязываем ID к ScopedValue для текущего потока выполнения
            ScopedValue.where(TenantContext.COMPANY_UUID, companyUuid).run(() -> {
                try {
                    filterChain.doFilter(request, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            // 3. если заголовка нет — просто идем дальше (сработает Fail-Fast в резолвере при попытке доступа к БД)
            filterChain.doFilter(request, response);
        }
    }

}
