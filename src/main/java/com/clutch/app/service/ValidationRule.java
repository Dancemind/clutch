package com.clutch.app.service;

import com.clutch.app.enums.RuleType;

public record ValidationRule(
        String targetColumn,    // например, "d_1"
        RuleType type,         // MIN, MAX, REGEX, REQUIRED
        String value,          // значение для проверки ("0", "^[0-9]+$")
        String errorMessage    // "Цена не может быть отрицательной"
) {}
