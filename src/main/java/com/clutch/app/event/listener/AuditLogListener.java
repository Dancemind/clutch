package com.clutch.app.event.listener;

import com.clutch.app.entity.audit.AuditLog;
import com.clutch.app.event.ClutchChangeEvent;
import com.clutch.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.clutch.app.config.TenantContext;

@Component
@RequiredArgsConstructor
public class AuditLogListener {

    private final AuditLogRepository auditLogRepository;

    @Async // Выполнится в виртуальном потоке (Loom)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void handleClutchChange(ClutchChangeEvent event) {
        // Привязываем системный контекст для записи в базу,
        // так как в асинхронном потоке ScopedValue оригинального запроса может быть недоступен
        ScopedValue.where(TenantContext.COMPANY_UUID, event.companyUuid()).run(() -> {
            AuditLog log = AuditLog.builder()
                    .clutchUuid(event.clutchUuidd())
                    .action(event.action())
                    .oldValues(event.oldData())
                    .newValues(event.newData())
                    .changedBy(event.user())
                    .build();
            auditLogRepository.save(log);
        });
    }
}

