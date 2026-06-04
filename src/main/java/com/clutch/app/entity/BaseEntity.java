package com.clutch.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity implements TenantEntity {

    @Id
    @GeneratedValue
    private UUID uuid;

    @TenantId // нативный механизм Hibernate 6+
    @Column(name = "company_uuid", nullable = false, updatable = false)
    private UUID companyUuid;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Version // оптимистическая блокировка — обязательно для HighLoad
    private Long version;

}
