package com.clutch.app.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return Objects.requireNonNullElse(TenantContext.get(), TenantContext.SYSTEM_UUID);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
