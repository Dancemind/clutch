package com.clutch.app.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompanyCreateRequest(
        @NotBlank(message = "Company name is required")
        String name
) {
}
