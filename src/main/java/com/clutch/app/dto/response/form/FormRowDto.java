package com.clutch.app.dto.response.form;

import java.util.Map;

public record FormRowDto(

        Long number,    // порядковый номер
        String uuid,    // айди

        Map<String, Object> items   // {"full_name": "Full Name", "price": 500..}

) {
}
