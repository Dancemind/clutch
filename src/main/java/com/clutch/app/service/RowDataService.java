package com.clutch.app.service;

import com.clutch.app.dto.RowDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.dto.response.form.FormDto;
import com.clutch.app.dto.response.form.FormRowDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.RowData;
import com.clutch.app.enums.AuditAction;
import com.clutch.app.event.RowChangedEvent;
import com.clutch.app.repository.RowDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RowDataService extends BaseService<RowData, UUID> {

    private final RowDataRepository rowDataRepository;
    private final ValidationService validationService;
    private final VarHandleMappingService mappingService;
    private final FormColumnService formColumnService;
    private final FormService formService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    protected JpaRepository<RowData, UUID> getRepository() {
        return rowDataRepository;
    }

    @Override
    protected String getEntityName() {
        return RowData.class.getSimpleName();
    }

    /**
     * Creates rows - adds data
     *
     * @param formUuid form uuid
     * @param rows     rows data
     * @return created rows
     */
    @Transactional
    public List<RowDto> createRows(UUID formUuid, List<RowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        formService.validateEntity(formUuid);

        Map<UUID, String> definition = formColumnService.getIdToTargetColumnMapping(formUuid);

        List<RowData> entitiesToSave = rows.stream()
                .map(rowDto -> {
                    RowData newRow = RowData.builder()
                            .formUuid(formUuid)
                            .orderNumber(rowDto.orderNumber())
                            .extraData(new HashMap<>())
                            .build();

                    mappingService.mapToEntity(rowDto.fieldsData(), newRow, definition);
                    validationService.validateRowData(formUuid, newRow);

                    return newRow;
                })
                .toList();

        List<RowData> savedRows = rowDataRepository.saveAll(entitiesToSave);

        List<UUID> savedUuids = savedRows.stream().map(RowData::getUuid).toList();
        log.info("Created {} rows for form {}. UUIDs {} ", savedRows.size(), formUuid, savedUuids);

        return savedRows.stream()
                .map(row -> mappingService.mapFromEntity(row, definition))
                .toList();
    }

    /**
     * Gets form - form info, column info and form data
     *
     * @param formUuid form uuid
     * @return form and its data
     */
    @Transactional(readOnly = true)
    public FormDto getFormData(UUID formUuid) {
        Map<UUID, String> definition = formColumnService.getIdToTargetColumnMapping(formUuid);

        Form form = formService.getForm(formUuid);

        List<FormColumnDto> formColumnsMetadata = formColumnService.getColumnsMetadataByFormId(formUuid).stream()
                .map(columnMD ->
                        new FormColumnDto(
                                columnMD.getOrderNumber(),
                                columnMD.getUuid(),
                                columnMD.getUserKey(),
                                columnMD.getFieldType()
                        )
                )
                .toList();

        List<FormRowDto> rowsData = rowDataRepository.findByFormUuid(formUuid).stream()
                .map(row ->
                        new FormRowDto(
                                row.getOrderNumber(),
                                row.getUuid().toString(),
                                mappingService.mapFromEntity(row, definition).fieldsData().stream()
                                        .collect(HashMap::new, (m, v) ->
                                                m.put(v.id(), v.value()), HashMap::putAll)
                        )
                )
                .toList();

        return new FormDto(
                form.getUuid().toString(),
                form.getName(),
                form.getDescription(),
                form.getCompanyUuid(),
                form.getProjectUuid(),
                formColumnsMetadata,
                rowsData
        );
    }

    /**
     * Updates row data
     *
     * @param rowUuid row uuid
     * @param rowDto  row data to insert
     * @return row data
     */
    @Transactional
    public RowDto updateRowData(UUID rowUuid, RowDto rowDto) {
        RowData existingRow = super.getByIdOrThrow(rowUuid);

        UUID formUuid = existingRow.getFormUuid();

        Map<UUID, String> definition = formColumnService.getIdToTargetColumnMapping(formUuid);

        // data snapshot before changes
        Map<String, Object> oldSnapshot = mappingService.mapFromEntity(existingRow, definition).fieldsData().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.id().toString(), v.value()), HashMap::putAll);

        // update existing row
        mappingService.mapToEntity(rowDto.fieldsData(), existingRow, definition);

        validationService.validateRowData(formUuid, existingRow);

        // if someone changed the same row data in parallel we expect to see ObjectOptimisticLockingFailureException
        RowData updatedRow = rowDataRepository.save(existingRow);

        // publish audit event
        RowDto updatedRowDto = mappingService.mapFromEntity(updatedRow, definition);
        Map<String, Object> newSnapshot = updatedRowDto.fieldsData().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.id().toString(), v.value()), HashMap::putAll);

        Form form = formService.getForm(formUuid);

        eventPublisher.publishEvent(
                new RowChangedEvent(
                        updatedRow.getCompanyUuid(),
                        form.getProjectUuid(),
                        updatedRow.getUuid(),
                        AuditAction.UPDATE,
                        oldSnapshot,
                        newSnapshot,
                        "current_user"
                )
        );

        return updatedRowDto;
    }

}

