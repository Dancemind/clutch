package com.clutch.app.repository;

import com.clutch.app.entity.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Получение истории конкретной записи.
     */
    Page<AuditLog> findByClutchUuidOrderByCreatedAtDesc(UUID clutchUuid, Pageable pageable);

    /**
     * Поиск всех действий конкретного пользователя (для СБ компании).
     */
    List<AuditLog> findByChangedBy(String user);
}
