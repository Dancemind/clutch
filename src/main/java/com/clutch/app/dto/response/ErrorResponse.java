package com.clutch.app.dto.response;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message
) {
}
