package com.clutch.app.dto.response.form;

import com.clutch.app.enums.FieldType;

import java.util.UUID;


public record FormColumnDto(
		Integer orderNumber,
		UUID uuid,
		String label,       // "label": "product name"
		FieldType type
) {
}
