package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import org.springframework.stereotype.Service;

// выполнить операцию от имени системы (например, в @Scheduled задаче или при старте приложения):
// вызов в ScopedValue с системным ID
@Service
public class SystemTaskService {

    public void runGlobalMaintenance() {
        // Привязываем системный ID на время выполнения блока
        ScopedValue.where(TenantContext.COMPANY_UUID, TenantContext.SYSTEM_UUID).run(() -> {
            // Весь код здесь, включая вызовы репозиториев Clutch,
            // будет работать в контексте SYSTEM_ID

//            clutchRepository.archiveOldRecords();
        });

//        Thread.ofVirtual().start(() -> {
//            try {
//                ScopedValue.where(TenantContext.COMPANY_UUID, companyUuid).run(() -> {
//                    streamingImportService.process(formId, importId, file);
//                });
//            } catch (Exception e) {
//                notificationService.sendError(importId, "Критическая ошибка системы: " + e.getMessage());
//            }
//        });

    }
}
