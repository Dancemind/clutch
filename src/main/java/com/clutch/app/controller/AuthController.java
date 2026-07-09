package com.clutch.app.controller;

import com.clutch.app.dto.request.LoginRequest;
import com.clutch.app.dto.response.LoginResponse;
import com.clutch.app.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse authenticate(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
