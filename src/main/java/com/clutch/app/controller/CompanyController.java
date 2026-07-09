package com.clutch.app.controller;

import com.clutch.app.dto.request.CompanyCreateRequest;
import com.clutch.app.dto.request.CompanyUpdateRequest;
import com.clutch.app.dto.response.CompanyResponse;
import com.clutch.app.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public CompanyResponse createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        return companyService.createCompany(request);
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'COMPANY_ADMIN')")
    public CompanyResponse updateCompany(@PathVariable("uuid") UUID uuid,
                                         @Valid @RequestBody CompanyUpdateRequest request) {
        return companyService.updateCompany(uuid, request);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void softDelete(@PathVariable("uuid") UUID uuid) {
        companyService.softDeleteCompany(uuid);
    }

}
