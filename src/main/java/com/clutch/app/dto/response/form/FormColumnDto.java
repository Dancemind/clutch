package com.clutch.app.dto.response.form;

import com.clutch.app.enums.FieldType;


public record FormColumnDto(
        	Integer number,     // "orderNumber": 1,
            String uuid,        // "uuid": "sdfsdfsdf",
            String label,       // "label": "ФИО"
			FieldType type
) {
}
