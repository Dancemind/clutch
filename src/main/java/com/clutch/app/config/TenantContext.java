package com.clutch.app.config;

import java.util.UUID;

public class TenantContext {

    // системный контекст
    public static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-1111-1111-555555555555");

    // в Java 25 ScopedValue — это новый стандарт передачи данных между слоями
    public static final ScopedValue<UUID> COMPANY_UUID = ScopedValue.newInstance();

    public static UUID get() {
        return COMPANY_UUID.isBound() ? COMPANY_UUID.get() : null;
    }
}
