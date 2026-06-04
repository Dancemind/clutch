package com.clutch.app.event;

import com.clutch.app.enums.AuditAction;

import java.util.Map;
import java.util.UUID;

public record ClutchChangeEvent(
    UUID companyUuid,
    UUID clutchUuidd,
    AuditAction action,
    Map<String, Object> oldData,
    Map<String, Object> newData,
    String user
) {}
