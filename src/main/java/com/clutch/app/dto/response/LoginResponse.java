package com.clutch.app.dto.response;

public record LoginResponse(
        String token,
        String type, // "Bearer" type
        String email
) {
}
