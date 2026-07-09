package com.clutch.app.security;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.request.LoginRequest;
import com.clutch.app.dto.response.LoginResponse;
import com.clutch.app.exceptions.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_PASSWORD_MESSAGE = "Invalid password";
    private static final String BEARER_TYPE = "Bearer";

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public LoginResponse login(LoginRequest request) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            log.warn(INVALID_PASSWORD_MESSAGE);
            throw new BadCredentialsException(INVALID_PASSWORD_MESSAGE);
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        UUID tenantId = TenantContext.callAsSystem(() ->
                userService.getCompanyUuidByUserEmail(request.email())
        );

        String token = jwtTokenProvider.createToken(
                userDetails.getUsername(),
                tenantId.toString(),
                roles
        );

        return new LoginResponse(token, BEARER_TYPE, userDetails.getUsername());
    }
}

