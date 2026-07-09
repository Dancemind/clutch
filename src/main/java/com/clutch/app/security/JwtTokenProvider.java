package com.clutch.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;

import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String COMPANY_ID_CLAIM = "companyId";
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${security.jwt.token.secret-key}") String secret,
            @Value("${security.jwt.token.expire-length}") long validityInMilliseconds) {

        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMilliseconds = validityInMilliseconds;

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Jwt expired. ".concat(e.getMessage()));
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Jwt exception. ".concat(e.getMessage()));
            return false;
        }
    }

    public String getCompanyIdFromToken(String token) {
        return getClaims(token).get(COMPANY_ID_CLAIM, String.class);
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Извлечение ролей из JWT и преобразование их в GrantedAuthority для Spring Security
     */
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> getAuthoritiesFromToken(String token) {
        List<String> roles = getClaims(token).get(ROLES_CLAIM, List.class);

        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Метод генерации токена с добавлением списка ролей
     */
    public String createToken(String username, String companyId, List<String> roles) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .claim(COMPANY_ID_CLAIM, companyId)
                .claim(ROLES_CLAIM, roles) // Сохраняем массив строк (ролей) в claim
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
