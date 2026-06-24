package com.clutch.app.dto;

import com.clutch.app.enums.RuleType;

public record ValidationRuleDto(
        String targetColumn,    // for ex. "d_1"
        RuleType type,         // MIN, MAX, REGEX, REQUIRED
        String value,          // correct value ("0", "^[0-9]+$")
        String errorMessage    // "Price can not be negative
) {
}
