package com.clutch.app.dto;

import java.util.List;
import java.util.UUID;

public record FormMetadataDto(UUID formUuid, String name, String description, List<FormField> fields) {
}
