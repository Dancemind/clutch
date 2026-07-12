package com.clutch.app.service;

import com.clutch.app.dto.response.AuditLogDto;
import com.clutch.app.entity.RowData;
import com.clutch.app.entity.audit.AuditLog;
import com.clutch.app.mappers.ClutchMapper;
import com.clutch.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int DEFAULT_AUDIT_LOG_PAGE_SIZE = 20;

    private final AuditLogRepository auditLogRepository;
    private final RowDataService rowDataService;
    private final ClutchMapper clutchMapper;

    /**
     * Gets history of data changes in row
     *
     * @param rowDataUuid row uuid
     * @param pageNumber    page number
     * @return page of data changes for row
     */
    public Page<AuditLogDto> getRowHistory(UUID rowDataUuid, int pageNumber) {

        RowData row = rowDataService.getByIdOrThrow(rowDataUuid);

        Pageable pageable = PageRequest.of(pageNumber, DEFAULT_AUDIT_LOG_PAGE_SIZE);
        Page<AuditLog> history = auditLogRepository.findByRowUuidOrderByCreatedAtDesc(row.getUuid(), pageable);

        return clutchMapper.toAuditLogDto(history);
    }

    /**
     * Gets history of data changes in row by user
     *
     * @param rowDataUuid row uuid
     * @param userEmail user email (id)
     * @param pageNumber    page number
     * @return page of data changes for row
     */
    public Page<AuditLogDto> getRowHistoryByUser(UUID rowDataUuid, String userEmail, int pageNumber) {

        RowData row = rowDataService.getByIdOrThrow(rowDataUuid);

        Pageable pageable = PageRequest.of(pageNumber, DEFAULT_AUDIT_LOG_PAGE_SIZE);
        Page<AuditLog> history = auditLogRepository.findByRowUuidAndChangedByOrderByCreatedAtDesc(row.getUuid(),
                userEmail, pageable);

        return clutchMapper.toAuditLogDto(history);
    }

    // todo: audit log for company responsible person - by project, form and user -
    // todo: what projects (row numbers) affected by user
}
