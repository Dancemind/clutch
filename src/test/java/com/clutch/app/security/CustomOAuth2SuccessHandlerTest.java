package com.clutch.app.security;

import com.clutch.app.config.MyJwtOAuth2SuccessHandler;
import com.clutch.app.config.TenantContext;
import com.clutch.app.enums.Role;
import com.clutch.app.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2SuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private MyJwtOAuth2SuccessHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @BeforeEach
    void setUp() {
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationSuccess_redirectSuccess() throws Exception {
        String email = "user@example.com";
        UUID companyUuid = UUID.randomUUID();
        String generatedToken = "mocked-jwt-token";

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);

        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> {
                        java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    });

            when(userService.getCompanyUuidByUserEmail(email)).thenReturn(companyUuid);
            when(jwtTokenProvider.createToken(eq(email), eq(companyUuid.toString()), anyList())).thenReturn(generatedToken);


            handler.onAuthenticationSuccess(request, response, authentication);


            ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy)
                    .sendRedirect(eq(request), eq(response), redirectUrlCaptor.capture());


            String targetUrl = redirectUrlCaptor.getValue();
            assertTrue(targetUrl.contains("/dashboard"));
            assertTrue(targetUrl.contains("token=" + generatedToken));
        }
    }

    @Test
    void onAuthenticationSuccess_emailAttributeIsNull_oauth2UserNameUsed() throws Exception {
        String username = "github_user_123";
        UUID companyUuid = UUID.randomUUID();
        String generatedToken = "mocked-jwt-token";

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getName()).thenReturn(username);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> {
                        java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    });

            when(userService.getCompanyUuidByUserEmail(username)).thenReturn(companyUuid);
            when(jwtTokenProvider.createToken(eq(username), eq(companyUuid.toString()), anyList()))
                    .thenReturn(generatedToken);


            handler.onAuthenticationSuccess(request, response, authentication);


            verify(redirectStrategy)
                    .sendRedirect(eq(request), eq(response), contains("/dashboard?token=" + generatedToken));
        }
    }

    @Test
    void onAuthenticationSuccess_userNotFound_redirectToLogin() throws Exception {
        // Given
        String email = "unknown@example.com";
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> {
                        java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    });

            when(userService.getCompanyUuidByUserEmail(email))
                    .thenThrow(new ResourceNotFoundException("Not found"));


            handler.onAuthenticationSuccess(request, response, authentication);


            verify(jwtTokenProvider, never()).createToken(any(), any(), any());
            verify(redirectStrategy)
                    .sendRedirect(eq(request), eq(response), contains("/login?error=user_not_registered"));
        }
    }

    @Test
    void onAuthenticationSuccess_principalNotOAuth2User_throwException() {
        when(authentication.getPrincipal()).thenReturn("JustAStringNotOAuth2User");

        assertThrows(AuthenticationServiceException.class, () ->
                handler.onAuthenticationSuccess(request, response, authentication)
        );

        verifyNoInteractions(userService, jwtTokenProvider, redirectStrategy);
    }

    @Test
    void onAuthenticationSuccess_emptyRoles_succeedWithEmptyListInToken() throws Exception {
        String email = "noroles@example.com";
        UUID companyUuid = UUID.randomUUID();
        String generatedToken = "token-for-user-without-roles";

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);

        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());

            when(userService.getCompanyUuidByUserEmail(email)).thenReturn(companyUuid);

            when(jwtTokenProvider.createToken(eq(email), eq(companyUuid.toString()), eq(Collections.emptyList())))
                    .thenReturn(generatedToken);


            handler.onAuthenticationSuccess(request, response, authentication);


            verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("/dashboard?token=" + generatedToken));
        }
    }

    @Test
    void onAuthenticationSuccess_authoritiesMappingPresent_roleNamesExtracted() throws Exception {
        String email = "complex-roles@example.com";
        UUID companyUuid = UUID.randomUUID();
        String generatedToken = "token-with-mapped-roles";

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);


        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(Role.ROLE_COMPANY_ADMIN.name())
        );
        doReturn(authorities).when(authentication).getAuthorities();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());

            when(userService.getCompanyUuidByUserEmail(email)).thenReturn(companyUuid);

            List<String> expectedRoles = List.of(Role.ROLE_COMPANY_ADMIN.name());
            when(jwtTokenProvider.createToken(eq(email), eq(companyUuid.toString()), eq(expectedRoles)))
                    .thenReturn(generatedToken);


            handler.onAuthenticationSuccess(request, response, authentication);


            verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("/dashboard?token=" + generatedToken));
        }
    }

    @Test
    void onAuthenticationSuccess_databaseCrash_exception() throws Exception {
        String email = "db-error@example.com";
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callAsSystem(any()))
                    .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());

            RuntimeException dbException = new RuntimeException("Database connection timeout");
            when(userService.getCompanyUuidByUserEmail(email)).thenThrow(dbException);


            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                handler.onAuthenticationSuccess(request, response, authentication)
            );


            assertEquals("Database connection timeout", exception.getMessage());

            verify(jwtTokenProvider, never()).createToken(any(), any(), any());
            verify(redirectStrategy, never()).sendRedirect(any(), any(), any());
        }
    }

}
