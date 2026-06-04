package com.clutch.app.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ClutchRequest(
        Long formId,
        BigDecimal price, // мапим в d_1
        String title,     // мапим в s_1
        UUID link         // мапим в l_1
) {}
