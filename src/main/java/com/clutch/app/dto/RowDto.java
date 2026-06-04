package com.clutch.app.dto;

import java.util.List;
import java.util.UUID;

public record RowDto(UUID uuid, Long orderNumber, List<FieldDto> fieldsData) {
}
