package com.clutch.app.service.scheduled;

import com.clutch.app.config.TenantContext;
import com.clutch.app.entity.Form;
import com.clutch.app.repository.ClutchRepository;
import com.clutch.app.repository.FormMetadataRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataCleanupTask {

    private final FormMetadataRepository formRepository;
    private final ClutchRepository clutchRepository;

    @Scheduled(cron = "0 0 3 * * *") // Каждый день в 3 утра
    @Transactional
    public void hardDeleteOldForms() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(30);

        // Привязываем системный ID на время выполнения блока
        ScopedValue.where(TenantContext.COMPANY_UUID, TenantContext.SYSTEM_UUID).run(() -> {

            // 1. Находим ID форм, помеченных на удаление более 30 дней назад
            List<UUID> oldFormUuids = formRepository.findExpiredForms(threshold).stream()
                    .map(Form::getUuid)
                    .toList();

            // 2. Сначала чистим данные в тяжелой таблице clutch
            clutchRepository.deleteAllByFormUuidIn(oldFormUuids);

            // 3. Затем удаляем сами метаданные (каскадом удалятся колонки и правила)
            formRepository.deleteAllByUuidIn(oldFormUuids);

//            clutchRepository.archiveOldRecords();
        });

    }
}
