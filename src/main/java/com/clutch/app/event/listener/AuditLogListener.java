package com.clutch.app.event.listener;

import com.clutch.app.config.TenantContext;
import com.clutch.app.entity.audit.AuditLog;
import com.clutch.app.event.RowChangedEvent;
import com.clutch.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditLogListener {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRowChangedEvent(RowChangedEvent event) {
        TenantContext.runWithTenant(event.companyUuid(), () -> {
            AuditLog log = AuditLog.builder()
                    .rowUuid(event.rowUuid())
                    .action(event.action())
                    .oldValues(event.oldData())
                    .newValues(event.newData())
                    .changedBy(event.user())
                    .build();
            auditLogRepository.save(log);
        });
    }

}
