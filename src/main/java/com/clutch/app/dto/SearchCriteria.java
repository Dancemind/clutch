package com.clutch.app.dto;

public record SearchCriteria(
        String fieldName, // "price"
        String operator,  // "GREATER_THAN", "EQUALS", "LIKE"
        Object value      // 5000
) {}
