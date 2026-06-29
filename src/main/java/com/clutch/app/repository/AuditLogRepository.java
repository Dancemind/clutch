package com.clutch.app.repository;

import com.clutch.app.entity.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByRowUuidOrderByCreatedAtDesc(UUID rowUuid, Pageable pageable);

    Page<AuditLog> findByRowUuidAndChangedByOrderByCreatedAtDesc(UUID rowUuid, String user, Pageable pageable);

}
