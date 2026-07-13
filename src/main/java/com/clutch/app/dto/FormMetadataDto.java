package com.clutch.app.dto;

import java.util.List;
import java.util.UUID;

public record FormMetadataDto(
        UUID projectUuid,
        UUID formUuid,
        String name,
        String description,
        List<FormFieldDto> fields
) {
}
