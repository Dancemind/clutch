package com.clutch.app.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectDto(
        UUID uuid,
        String name,
        String description,
        boolean isActive,
        UUID companyUuid,
        OffsetDateTime createdAt
) {
}
