package com.clutch.app.service;

import com.clutch.app.dto.FieldDto;
import com.clutch.app.dto.RowDto;
import com.clutch.app.dto.ValidationRuleDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.dto.response.form.FormDto;
import com.clutch.app.dto.response.form.FormRowDto;
import com.clutch.app.entity.Clutch;
import com.clutch.app.entity.Form;
import com.clutch.app.enums.AuditAction;
import com.clutch.app.event.ClutchChangeEvent;
import com.clutch.app.repository.ClutchRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClutchService {

    private final ClutchRepository clutchRepository;
    private final ValidationService validationService;
    private final VarHandleMappingService mappingService;
    private final MetadataService metadataService;
    private final FormService formService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<RowDto> createRows(UUID formUuid, List<RowDto> rowsData) throws ValidationException {

        List<RowDto> createdRows = new ArrayList<>();

        for (RowDto rowData : rowsData) {
            Map<UUID, Object> fieldsData = rowData.fieldsData().stream()
                    .collect(Collectors.toMap(FieldDto::id, FieldDto::value));

            createdRows.add(
                    createRow(formUuid, rowData.orderNumber(), fieldsData)
            );
        }
        return createdRows;
    }

    @Transactional
    public RowDto createRow(UUID formUuid, Long orderNumber, Map<UUID, Object> payload)
            throws ValidationException {
        // 1. Получаем структуру формы и правила из метаданных (с кэшем в Redis)
        Map<UUID, String> definition = metadataService.getIdToTargetColumnMapping(formUuid);
        List<ValidationRuleDto> rules = metadataService.getValidationRules(formUuid);

        // 2. Инициализируем сущность
        Clutch clutch = Clutch.builder()
                .formUuid(formUuid)
                .orderNumber(orderNumber)
                .extraData(new HashMap<>())
                .build();

        // 3. VarHandle маппинг: перекладываем JSON в физические колонки (d_1, s_1...)
        mappingService.mapToEntity(payload, clutch, definition);

        // 4. Динамическая валидация бизнес-логики
        validationService.validate(clutch, rules);

        // 5. Сохранение (Hibernate сам подставит companyId через @TenantId)
        Clutch savedClutch = clutchRepository.save(clutch);

        log.info("Created Clutch record {} for form {}", savedClutch.getUuid(), formUuid);

        // 6. Возвращаем клиенту "красивый" JSON, а не d_1/s_1
        return mappingService.mapFromEntity(savedClutch, definition);
    }

    // get form/table in user-friendly format
    @Transactional(readOnly = true)
    public FormDto getForm(UUID formUuid) {
        Map<UUID, String> definition = metadataService.getIdToTargetColumnMapping(formUuid);

        Form formMetadata = formService.getFormMetadata(formUuid);

        if (isNull(formMetadata)) {
            throw new EntityNotFoundException("Form metadata not found");
        }

        List<FormColumnDto> formColumnsMetadata = metadataService.getColumnsMetadataByFormId(formUuid).stream()
                .map(columnMD ->
                        new FormColumnDto(
                                columnMD.getOrderNumber(),
                                columnMD.getUuid().toString(),
                                columnMD.getUserKey(),
                                columnMD.getFieldType()
                        )
                )
                .toList();

        List<FormRowDto> rowsData = clutchRepository.findByFormUuid(formUuid).stream()
                .map(row ->
                        new FormRowDto(
                                row.getOrderNumber(),
                                row.getUuid().toString(),
                                mappingService.mapFromEntity(row, definition).fieldsData().stream()
                                        .collect(HashMap::new, (m, v) ->
                                                m.put(v.id().toString(), v.value()), HashMap::putAll)
                        )
                )
                .toList();

        return new FormDto(
                formMetadata.getUuid().toString(),
                formMetadata.getName(),
                formMetadata.getDescription(),
                formColumnsMetadata,
                rowsData
        );
    }

    @Transactional
    public Map<String, Object> updateRow(UUID rowUuid, Map<UUID, Object> payload) throws ValidationException {
        // 1. Находим существующую запись (Hibernate сам добавит WHERE company_id = ?)
        Clutch existingRecord = clutchRepository.findByUuid(rowUuid)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена"));

        // 2. Получаем метаданные формы для маппинга и валидации
        UUID formUuid = existingRecord.getFormUuid();

        Map<UUID, String> definition = metadataService.getIdToTargetColumnMapping(formUuid);
        List<ValidationRuleDto> rules = metadataService.getValidationRules(formUuid);

        // 3. Делаем "снимок" данных ДО изменений для аудита
        Map<String, Object> oldSnapshot = mappingService.mapFromEntity(existingRecord, definition).fieldsData().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.id().toString(), v.value()), HashMap::putAll);

        // 4. Применяем новые данные к существующей сущности через VarHandles
        // Важно: mappingService изменит только те поля, которые пришли в payload
        mappingService.mapToEntity(payload, existingRecord, definition);

        // 5. Валидируем результат (проверяем, не нарушил ли апдейт бизнес-правила)
        validationService.validate(existingRecord, rules);

        // 6. Сохраняем.
        // Если за время работы метода кто-то другой изменил запись,
        // @Version в BaseEntity выкинет ObjectOptimisticLockingFailureException.
        Clutch updated = clutchRepository.save(existingRecord);

        // 7. Публикуем событие аудита (асинхронно)
        Map<String, Object> newSnapshot = mappingService.mapFromEntity(updated, definition).fieldsData().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.id().toString(), v.value()), HashMap::putAll);

        eventPublisher.publishEvent(
                new ClutchChangeEvent(
                        updated.getCompanyUuid(), updated.getUuid(), AuditAction.UPDATE,
                        oldSnapshot, newSnapshot, "current_user"
                )
        );

        return newSnapshot;
    }

}

