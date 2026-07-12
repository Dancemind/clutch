package com.clutch.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestOath2LoginResultController {

    @GetMapping("/dashboard")
    public Map<String, String> dashboard(@RequestParam("token") String token) {
        return Map.of(
                "status", "OAuth2 success login!",
                "your_jwt_token", token
        );
    }

}
