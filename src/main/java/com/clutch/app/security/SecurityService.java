package com.clutch.app.security;

import com.clutch.app.config.TenantContext;
import com.clutch.app.entity.User;
import com.clutch.app.enums.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.clutch.app.config.TenantContext.SYSTEM_UUID;

@Slf4j
@Service
public class SecurityService {

    public boolean isSystemUser(User user) {
        return user != null
                && SYSTEM_UUID.equals(user.getCompanyUuid());
    }

    public static void validateSystemAdmin() {
        if (!isRoleSystemAdmin()) {
            log.error("Only system administrators can perform this action");
            throw new AccessDeniedException("Only system administrators can perform this action");
        }
    }

    private void validateCompanyAdmin() {
        if (!isRoleCompanyAdmin()) {
            throw new AccessDeniedException("Only company administrators can perform this action");
        }
    }

    public static void validateCompanyUser() {
        if (!isRoleCompanyUser()) {
            throw new AccessDeniedException("Only company user can perform this action");
        }
    }

    public static void validateAccessToCompanyAdminActions(UUID companyUuid) {
        // system admin - allow
        if (isRoleSystemAdmin()) {
            return;
        }
        // company admin - allow actions with his company
        if (isRoleCompanyAdmin()) {
            UUID currentTenant = TenantContext.get()
                    .orElseThrow(() -> new IllegalStateException("No tenant context found"));

            if (!currentTenant.equals(companyUuid)) {
                throw new AccessDeniedException("You can only modify your own company");
            }
            return;
        }
        // all other roles and tenants
        throw new AccessDeniedException("You don't have permission to perform this action");
    }

    private static boolean isRoleSystemAdmin() {
        return checkUserRole(Role.ROLE_SYSTEM_ADMIN);
    }

    private static boolean isRoleCompanyAdmin() {
        return checkUserRole(Role.ROLE_COMPANY_ADMIN);
    }

    private static boolean isRoleCompanyUser() {
        return checkUserRole(Role.ROLE_COMPANY_USER);
    }

    private static boolean checkUserRole(Role userRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = (authentication != null)
                ? authentication.getAuthorities()
                : List.of();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> userRole.name().equals(role));
    }

}
