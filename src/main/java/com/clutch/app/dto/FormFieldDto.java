package com.clutch.app.dto;

import com.clutch.app.enums.FieldType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FormFieldDto(UUID uuid, String name, @NotNull FieldType type, int orderNumber) {
    public FormFieldDto {
        java.util.Objects.requireNonNull(type, "FieldType cannot be null");
    }
}
