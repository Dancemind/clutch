package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// run task as system
// for ex. @Scheduled tasks or on app start
// uses virtual thread
@Slf4j
@Service
public class SystemTaskService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public Future<?> runAsSystem(@NotNull Runnable task) {
        return executor.submit(() -> {
            try {
                ScopedValue
                        .where(TenantContext.COMPANY_UUID, TenantContext.SYSTEM_UUID)
                        .run(task);
            } catch (Throwable throwable) {
                log.error("Critical error in system task execution", throwable);
                throw throwable;
            }
        });
    }
}
