package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.request.CompanyCreateRequest;
import com.clutch.app.dto.request.CompanyUpdateRequest;
import com.clutch.app.entity.Company;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.exceptions.ValidationException;
import com.clutch.app.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.clutch.app.security.SecurityService.validateAccessToCompanyAdminActions;
import static com.clutch.app.security.SecurityService.validateSystemAdmin;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService extends BaseService<Company, UUID> {

    private final CompanyRepository companyRepository;

    @Override
    protected JpaRepository<Company, UUID> getRepository() {
        return companyRepository;
    }

    @Override
    protected String getEntityName() {
        return Company.class.getSimpleName();
    }

    @Transactional
    public Company createCompany(CompanyCreateRequest request) {

        validateSystemAdmin();

        validateUniquenessCompanyName(request.name());

        Company company = new Company();
        company.setName(request.name());

        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(UUID companyUuid, CompanyUpdateRequest request) {

        validateAccessToCompanyAdminActions(companyUuid);

        Company company = TenantContext.callAsSystem(() ->
                companyRepository.findById(companyUuid)
                        .orElseThrow(() -> new ResourceNotFoundException("Company not found"))
        );

        if (company.isDeleted()) {
            throw new IllegalArgumentException("Cannot update a deleted company");
        }

        company.setName(request.name());
        return TenantContext.callAsSystem(() -> companyRepository.save(company));
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

    private void validateUniquenessCompanyName(String name) {
        if (companyRepository.existsByName(name)) {
            log.error("Company name not unique: ".concat(name));
            throw new ValidationException("Company name not unique: ".concat(name));
        }
    }

}
