package com.clutch.app.security;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.request.CreateUserRequest;
import com.clutch.app.entity.User;
import com.clutch.app.enums.Role;
import com.clutch.app.exceptions.IllegalArgumentException;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUserRole(Role role) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority(role.name()))).when(authentication).getAuthorities();
    }

    @Test
    void getCompanyUuidByUserEmail_userExists_shouldReturnUuid() {
        String email = "test@clutch.app";
        UUID companyUuid = UUID.randomUUID();
        User user = new User();
        user.setCompanyUuid(companyUuid);

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));


        UUID result = userService.getCompanyUuidByUserEmail(email);


        assertEquals(companyUuid, result);
    }

    @Test
    void getCompanyUuidByUserEmail_userNotFound_shouldThrowException() {
        String email = "missing@clutch.app";
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getCompanyUuidByUserEmail(email));
    }

    @Test
    void registerUser_noPasswordForNonGoogleUser_shouldThrowIllegalArgumentException() {
        CreateUserRequest request = new CreateUserRequest(
                "test@clutch.app", null, Role.ROLE_COMPANY_USER, UUID.randomUUID(), false
        );


        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(request));


        assertEquals("Please provide password", ex.getMessage());
    }

    @Test
    void registerUser_shortPassword_shouldThrowIllegalArgumentException() {
        mockCurrentUserRole(Role.ROLE_SYSTEM_ADMIN);
        CreateUserRequest request = new CreateUserRequest(
                "test@clutch.app", "123", Role.ROLE_COMPANY_USER, UUID.randomUUID(), false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);


        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(request));


        assertEquals("Password must be at least 6 characters long", ex.getMessage());
    }

    @Test
    void registerUser_emailAlreadyRegistered_shouldThrowIllegalArgumentException() {
        CreateUserRequest request = new CreateUserRequest(
                "exists@clutch.app", "password123", Role.ROLE_COMPANY_USER, UUID.randomUUID(), false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);


        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(request)
        );


        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void registerUser_systemAdmin_shouldRegisterSuccessfully() {
        mockCurrentUserRole(Role.ROLE_SYSTEM_ADMIN);
        UUID targetCompany = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest(
                "new-admin@clutch.app", "password123", Role.ROLE_SYSTEM_ADMIN, targetCompany, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pwd");

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callWithTenant(any(), any()))
                    .thenAnswer(invocation -> {
                        TenantContext.CheckedSupplier<?, ?> action = invocation.getArgument(1);
                        return action.get();
                    });

            User savedUser = new User();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);


            User result = userService.registerUser(request);


            assertNotNull(result);
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void registerUser_companyAdmin_success_whenSameCompany() {
        mockCurrentUserRole(Role.ROLE_COMPANY_ADMIN);
        UUID myCompanyUuid = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest(
                "employee@clutch.app", "password123", Role.ROLE_COMPANY_USER, myCompanyUuid, false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pwd");

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::get).thenReturn(Optional.of(myCompanyUuid));
            tenantContextMock.when(() -> TenantContext.callWithTenant(any(), any()))
                    .thenAnswer(invocation -> {
                        TenantContext.CheckedSupplier<?, ?> action = invocation.getArgument(1);
                        return action.get();
                    });

            when(userRepository.save(any(User.class))).thenReturn(new User());


            User result = userService.registerUser(request);


            assertNotNull(result);
        }
    }

    @Test
    void registerUser_companyAdmin_throwsAccessDenied_whenDifferentCompany() {
        mockCurrentUserRole(Role.ROLE_COMPANY_ADMIN);
        UUID myCompanyUuid = UUID.randomUUID();
        UUID alienCompanyUuid = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "employee@clutch.app", "password123", Role.ROLE_COMPANY_USER, alienCompanyUuid, false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::get).thenReturn(Optional.of(myCompanyUuid));


            assertThrows(
                    AccessDeniedException.class,
                    () -> userService.registerUser(request)
            );


            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void registerUser_companyAdmin_throwsAccessDenied_whenCreatingSystemAdmin() {
        mockCurrentUserRole(Role.ROLE_COMPANY_ADMIN);
        UUID myCompanyUuid = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "hacker@clutch.app", "password123", Role.ROLE_SYSTEM_ADMIN, myCompanyUuid, false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::get).thenReturn(Optional.of(myCompanyUuid));


            assertThrows(
                    AccessDeniedException.class,
                    () -> userService.registerUser(request)
            );
        }
    }

    @Test
    void registerUser_regularUser_throwsAccessDeniedException() {
        mockCurrentUserRole(Role.ROLE_COMPANY_USER);
        CreateUserRequest request = new CreateUserRequest(
                "any@clutch.app", "password123", Role.ROLE_COMPANY_USER, UUID.randomUUID(), false
        );


        when(userRepository.existsByEmail(request.email())).thenReturn(false);


        assertThrows(
                AccessDeniedException.class,
                () -> userService.registerUser(request)
        );
    }

    @Test
    void registerUser_googleProviderWithoutPassword_shouldUseExternalAccountConstant() {
        mockCurrentUserRole(Role.ROLE_SYSTEM_ADMIN);
        UUID companyUuid = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "google-user@clutch.app", null, Role.ROLE_COMPANY_USER, companyUuid, true
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(() -> TenantContext.callWithTenant(any(), any()))
                    .thenAnswer(invocation -> {
                        TenantContext.CheckedSupplier<?, ?> action = invocation.getArgument(1);
                        return action.get();
                    });


            userService.registerUser(request);


            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository)
                    .save(
                            argThat(user ->
                                    user.getPassword().equals(UserService.EXTERNAL_ACCOUNT_PWD)
                                            && user.getEmail().equals("google-user@clutch.app")
                            )
                    );
        }
    }

}
