package com.clutch.app.dto.request;

import com.clutch.app.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank
        @Email
        String email,

        String password,

        @NotNull
        Role role,

        @NotNull
        UUID companyUuid
) {
}
