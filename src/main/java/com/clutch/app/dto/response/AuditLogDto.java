package com.clutch.app.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(

        UUID rowUuid,
        String action,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String changedBy,
        OffsetDateTime createdAt

) {
}
