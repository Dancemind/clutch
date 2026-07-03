package com.clutch.app.service.scheduled;

import com.clutch.app.config.TenantContext;
import com.clutch.app.entity.Form;
import com.clutch.app.repository.RowDataRepository;
import com.clutch.app.repository.FormRepository;
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

    private final FormRepository formRepository;
    private final RowDataRepository rowDataRepository;

    /**
     * Removes forms deleted more than 30 days ago
     * Uses system id
     */
    @Scheduled(cron = "0 0 3 * * *") // each day 3 am
    @Transactional
    public void hardDeleteOldForms() {
        // todo: move to env var or system
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(30);

        ScopedValue.where(TenantContext.COMPANY_UUID, TenantContext.SYSTEM_UUID).run(() -> {

            List<UUID> oldFormUuids = formRepository.findExpiredForms(threshold).stream()
                    .map(Form::getUuid)
                    .toList();

            // delete form's data
            rowDataRepository.deleteAllByFormUuidIn(oldFormUuids);

            // delete forms metadata
            formRepository.deleteAllByUuidIn(oldFormUuids);

            // todo: archive old records? => clutchRepository.archiveOldRecords();
        });

    }
}
