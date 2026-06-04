package com.clutch.app.entity;

import java.util.UUID;

public interface TenantEntity {
    UUID getCompanyUuid();
    void setCompanyUuid(UUID companyUuid);
}
