package com.clutch.app.controller;

import com.clutch.app.dto.request.CreateUserRequest;
import com.clutch.app.dto.response.CreateUserResponse;
import com.clutch.app.entity.User;
import com.clutch.app.security.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'COMPANY_ADMIN')")
    public CreateUserResponse register(@Valid @RequestBody CreateUserRequest request) {

        User user = userService.registerUser(request);

        return new CreateUserResponse(
                "User successfully registered",
                user.getEmail(),
                user.getCompanyUuid(),
                user.getRole()
        );
    }
}
