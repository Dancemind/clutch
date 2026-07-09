package com.clutch.app.dto.response;

import java.util.UUID;

public record CompanyResponse(
        UUID uuid,
        String name,
        boolean deleted
) {
}
