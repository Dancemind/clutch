package com.clutch.app.dto.response;

import com.clutch.app.enums.Role;

import java.util.UUID;

public record CreateUserResponse(
        String message,
        String email,
        UUID companyUuid,
        Role role
) {
}
