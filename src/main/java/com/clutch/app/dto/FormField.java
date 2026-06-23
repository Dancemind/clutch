package com.clutch.app.dto;

import com.clutch.app.enums.FieldType;

public record FormField(String name, FieldType type, int orderNumber) {
}
