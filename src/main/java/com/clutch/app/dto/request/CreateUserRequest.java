package com.clutch.app.dto.request;

import com.clutch.app.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Invalid email format")
        String email,

        String password,

        @NotNull(message = "Role is mandatory")
        Role role,

        @NotNull(message = "Company is mandatory")
        UUID companyUuid,

        @NotNull(message = "Please set provider")
        Boolean googleProvider
) {
}
