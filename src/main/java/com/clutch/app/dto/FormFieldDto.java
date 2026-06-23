package com.clutch.app.dto;

import com.clutch.app.enums.FieldType;

import java.util.UUID;

public record FormFieldDto(UUID id, String name, FieldType type, int orderNumber) {
}
