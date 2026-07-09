package com.clutch.app.entity.audit;

import com.clutch.app.entity.CompanyBaseEntity;
import com.clutch.app.enums.AuditAction;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_clutch", columnList = "clutch_uuid"),
        @Index(name = "idx_audit_company", columnList = "company_uuid")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends CompanyBaseEntity {

    @Column(name = "clutch_uuid", nullable = false)
    private UUID rowUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action; // CREATE, UPDATE, DELETE

    @Type(JsonBinaryType.class)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @Type(JsonBinaryType.class)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @Column(name = "changed_by")
    private String changedBy; // ID or Email
}

