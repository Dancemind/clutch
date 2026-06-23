package com.clutch.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "forms")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Form extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
