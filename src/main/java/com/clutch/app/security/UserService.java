package com.clutch.app.security;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.request.CreateUserRequest;
import com.clutch.app.entity.User;
import com.clutch.app.enums.Role;
import com.clutch.app.exceptions.IllegalArgumentException;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final String EXTERNAL_ACCOUNT_PWD = "OAUTH2_EXTERNAL_ACCOUNT";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UUID getCompanyUuidByUserEmail(String email) {
        return userRepository.findUserByEmail(email)
                .map(User::getCompanyUuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found. Email: %s".formatted(email))
                );
    }

    public User registerUser(CreateUserRequest request) {
        if (!request.googleProvider() && !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Please provide password");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = (authentication != null)
                ? authentication.getAuthorities()
                : List.of();

        boolean isSystemAdmin = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Role.ROLE_SYSTEM_ADMIN.toString()::equals);

        boolean isCompanyAdmin = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Role.ROLE_COMPANY_ADMIN.toString()::equals);

        // todo: notifications to admins about new users in company
        if (isSystemAdmin) {
            // no restrictions
        } else if (isCompanyAdmin) {

            UUID currentTenant = TenantContext.get()
                    .orElseThrow(() -> new IllegalStateException("No tenant context found"));

            // ROLE_COMPANY_ADMIN user adds users to his company only
            if (!currentTenant.equals(request.companyUuid())) {
                throw new AccessDeniedException("You can only register users for your own company");
            }

            // ROLE_COMPANY_ADMIN user can't create system admins
            if (request.role() == Role.ROLE_SYSTEM_ADMIN) {
                throw new AccessDeniedException("Company administrators cannot create system administrators");
            }
        } else {
            throw new AccessDeniedException("You don't have permission to register users");
        }

        String encodedPassword;

        if (StringUtils.hasText(request.password())) {
            if (request.password().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long");
            }
            encodedPassword = passwordEncoder.encode(request.password());
        } else {
            // password for OAuth2/Google
            encodedPassword = EXTERNAL_ACCOUNT_PWD;
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(encodedPassword);
        user.setRole(request.role());
        user.setCompanyUuid(request.companyUuid());

        try {
            return TenantContext.callWithTenant(request.companyUuid(), () -> userRepository.save(user));
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Failed to register user in isolated tenant context", e);
        }
    }
}
