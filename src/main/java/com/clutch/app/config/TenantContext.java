package com.clutch.app.config;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TenantContext {

    public static final UUID SYSTEM_UUID = UUID.fromString("5757e70a-1d14-4500-8000-0000005757e7");

    private static final ScopedValue<UUID> CURRENT_TENANT = ScopedValue.newInstance();

    private TenantContext() {
    }

    public static <T, E extends Throwable> T callWithTenant(UUID tenantId, CheckedSupplier<T, E> action) throws E {
        return ScopedValue.where(CURRENT_TENANT, tenantId)
                .call(action::get);
    }

    public static void runWithTenant(UUID tenantId, Runnable action) {
        ScopedValue.where(CURRENT_TENANT, tenantId)
                .run(action);
    }

    public static <T> T callAsSystem(Supplier<T> action) {
        return ScopedValue.where(CURRENT_TENANT, SYSTEM_UUID)
                .call(action::get);
    }

    public static void runAsSystem(Runnable action) {
        ScopedValue.where(CURRENT_TENANT, SYSTEM_UUID)
                .run(action);
    }

    public static Optional<UUID> get() {
        return CURRENT_TENANT.isBound()
                ? Optional.of(CURRENT_TENANT.get())
                : Optional.empty();
    }

    @FunctionalInterface
    public interface CheckedSupplier<T, E extends Throwable> {
        T get() throws E;
    }
}
