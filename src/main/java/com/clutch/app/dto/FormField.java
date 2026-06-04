package com.clutch.app.dto;

import com.clutch.app.enums.FieldType;

import java.util.UUID;

public record FormField(UUID fieldUuid, String name, FieldType type, int orderNumber) {
}
