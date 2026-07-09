package com.clutch.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class CompanyBaseEntity extends BaseEntity implements TenantEntity {

    @TenantId
    @Column(name = "company_uuid", nullable = false, updatable = false)
    private UUID companyUuid;

}
