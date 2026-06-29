package com.clutch.app.dto.response.form;

import java.util.Map;
import java.util.UUID;

public record FormRowDto(

        Long number,    // order number of row
        String uuid,

        Map<UUID, Object> items   // {"UUID-1": "Full Name", "UUID-2": 500..}

) {
}
