package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.request.CompanyCreateRequest;
import com.clutch.app.dto.request.CompanyUpdateRequest;
import com.clutch.app.dto.response.CompanyResponse;
import com.clutch.app.entity.Company;
import com.clutch.app.enums.Role;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.exceptions.ValidationException;
import com.clutch.app.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyResponse createCompany(CompanyCreateRequest request) {
        validateSystemAdmin();

        validateUniquenessCompanyName(request.name());

        Company company = new Company();
        company.setName(request.name());

        Company saved = companyRepository.save(company);
        return mapToResponse(saved);
    }

    @Transactional
    public CompanyResponse updateCompany(UUID companyUuid, CompanyUpdateRequest request) {

        validateAccessToCompanyAdminActions(companyUuid);

        Company company = TenantContext.callAsSystem(() ->
                companyRepository.findById(companyUuid)
                        .orElseThrow(() -> new ResourceNotFoundException("Company not found"))
        );

        if (company.isDeleted()) {
            throw new IllegalArgumentException("Cannot update a deleted company");
        }

        company.setName(request.name());
        Company updated = TenantContext.callAsSystem(() -> companyRepository.save(company));
        return mapToResponse(updated);
    }

    @Transactional
    public void softDeleteCompany(UUID companyUuid) {
        validateSystemAdmin();

        Company company = TenantContext.callAsSystem(() ->
                companyRepository.findById(companyUuid)
                        .orElseThrow(() -> new ResourceNotFoundException("Company not found"))
        );

        company.setDeletedAt(OffsetDateTime.now());
        TenantContext.callAsSystem(() -> companyRepository.save(company));
    }

    private void validateSystemAdmin() {
        if (!isRoleSystemAdmin()) {
            log.error("Only system administrators can perform this action");
            throw new AccessDeniedException("Only system administrators can perform this action");
        }
    }

    private void validateUniquenessCompanyName(String name) {
        if (companyRepository.existsByName(name)) {
            log.error("Company name not unique: ".concat(name));
            throw new ValidationException("Company name not unique: ".concat(name));
        }
    }

    private void validateCompanyAdmin() {
        if (!isRoleCompanyAdmin()) {
            throw new AccessDeniedException("Only company administrators can perform this action");
        }
    }

    private void validateCompanyUser() {
        if (!isRoleCompanyUser()) {
            throw new AccessDeniedException("Only company user can perform this action");
        }
    }

    private void validateAccessToCompanyAdminActions(UUID targetCompanyUuid) {
        if (isRoleSystemAdmin()) {
            return;
        }
        if (!isRoleCompanyAdmin()) {
            throw new AccessDeniedException("You don't have permission to perform this action");
        }

        UUID currentTenant = TenantContext.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"));

        if (!currentTenant.equals(targetCompanyUuid)) {
            throw new AccessDeniedException("You can only modify your own company");
        }
    }

    private boolean isRoleSystemAdmin() {
        return checkUserRole(Role.ROLE_SYSTEM_ADMIN);
    }

    private boolean isRoleCompanyAdmin() {
        return checkUserRole(Role.ROLE_COMPANY_ADMIN);
    }

    private boolean isRoleCompanyUser() {
        return checkUserRole(Role.ROLE_COMPANY_USER);
    }

    private boolean checkUserRole(Role userRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = (authentication != null)
                ? authentication.getAuthorities()
                : List.of();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> userRole.name().equals(role));
    }

    private CompanyResponse mapToResponse(Company company) {
        return new CompanyResponse(company.getUuid(), company.getName(), company.isDeleted());
    }

}
