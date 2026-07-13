package com.clutch.app.security;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.request.LoginRequest;
import com.clutch.app.dto.response.LoginResponse;
import com.clutch.app.exceptions.BadCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success_loginResponse() {
        String email = "user@clutch.app";
        String rawPassword = "securePassword";
        String encodedPassword = "encodedSecurePassword";
        String fakeToken = "generated-jwt-token";
        UUID tenantId = UUID.randomUUID();

        LoginRequest request = new LoginRequest(email, rawPassword);

        UserDetails userDetails = new User(
                email,
                encodedPassword,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> {
                        Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    });

            when(userService.getCompanyUuidByUserEmail(email)).thenReturn(tenantId);
            when(jwtTokenProvider.createToken(eq(email), eq(tenantId.toString()), eq(List.of("ROLE_USER"))))
                    .thenReturn(fakeToken);


            LoginResponse response = authService.login(request);


            assertNotNull(response);
            assertEquals(fakeToken, response.token());
            assertEquals("Bearer", response.type());
            assertEquals(email, response.email());

            verify(userDetailsService).loadUserByUsername(email);
            verify(passwordEncoder).matches(rawPassword, encodedPassword);
            verify(jwtTokenProvider).createToken(any(), any(), any());
        }
    }

    @Test
    void login_invalidPassword_badCredentialsException() {
        String email = "user@clutch.app";
        String wrongPassword = "wrongPassword";
        String encodedPassword = "encodedSecurePassword";

        LoginRequest request = new LoginRequest(email, wrongPassword);
        UserDetails userDetails = new User(email, encodedPassword, Collections.emptyList());

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        when(passwordEncoder.matches(wrongPassword, encodedPassword)).thenReturn(false);


        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });


        assertEquals("Invalid password", exception.getMessage());

        verifyNoInteractions(jwtTokenProvider, userService);
    }

    @Test
    void login_userNotFound_exception() {
        String email = "unknown@clutch.app";
        LoginRequest request = new LoginRequest(email, "somePassword");

        when(userDetailsService.loadUserByUsername(email))
                .thenThrow(new UsernameNotFoundException("User not found"));


        assertThrows(UsernameNotFoundException.class, () -> {
            authService.login(request);
        });


        verifyNoInteractions(passwordEncoder, jwtTokenProvider, userService);
    }
}
