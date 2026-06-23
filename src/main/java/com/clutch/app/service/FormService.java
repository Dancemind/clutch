package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.FormField;
import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.exception.QuotaExceededException;
import com.clutch.app.repository.ColumnDefinitionRepository;
import com.clutch.app.repository.FormMetadataRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormService {

    // max forms/tables per one company
    @Value("${clutch.quotas.max-forms:10}")
    private int maxFormsQuota;

    private final FormMetadataRepository formMetadataRepository;
    private final ColumnDefinitionRepository formColumnRepository;

    @Transactional
//    @CacheEvict(value = "formDefinitions", key = "#resultId") // Инвалидация кэша при создании/обновлении
    public FormMetadataDto createForm(String name, String description, List<FormFieldDto> fields) {
        // checks if quota on create table is exceeded for the company
        checkQuota();

        // 1. create form
        Form form = Form.builder()
                .name(name)
                .description(description)
                .build();
        Form formSaved = formMetadataRepository.save(form);

        // 2. fields mapping
        AtomicInteger dCount = new AtomicInteger(1);
        AtomicInteger nCount = new AtomicInteger(1);
        AtomicInteger tCount = new AtomicInteger(1);
        AtomicInteger lCount = new AtomicInteger(1);
        AtomicInteger iCount = new AtomicInteger(1);
        AtomicInteger ttCount = new AtomicInteger(1);
        AtomicInteger bCount = new AtomicInteger(1);

        List<FormColumn> definitions = fields.stream().map(field -> {
            String columnName = switch (field.type()) {
                case MONEY -> (dCount.get() <= 4) ? "d_" + dCount.getAndIncrement() : "extra_data";
                case NUMBER -> (nCount.get() <= 4) ? "n_" + nCount.getAndIncrement() : "extra_data";
                case DATE -> (tCount.get() <= 5) ? "t_" + tCount.getAndIncrement() : "extra_data";
                case LINK -> (lCount.get() <= 5) ? "l_" + lCount.getAndIncrement() : "extra_data";
                case BUSINESS_ID -> (iCount.get() <= 5) ? "id_" + iCount.getAndIncrement() : "extra_data";
                case TEXT -> (ttCount.get() <= 15) ? "txt_" + ttCount.getAndIncrement() : "extra_data";
                case FLAG -> (bCount.get() <= 5) ? "b_" + bCount.getAndIncrement() : "extra_data";

                default -> "extra_data"; // if type is unknown or the field poll exceeded
            };

            return FormColumn.builder()
                    .formUuid(formSaved.getUuid())
                    .userKey(field.name())
                    .fieldType(field.type())
                    .targetColumn(columnName)
                    .orderNumber(field.orderNumber())
                    .build();
        }).toList();

        List<FormColumn> formColumns = formColumnRepository.saveAll(definitions);

        return getFormMetadataDto(
                formSaved.getUuid(),
                formSaved.getName(),
                formSaved.getDescription(),
                formColumns);
    }


    //  get all forms of user for the company
    public List<FormMetadataDto> getAllForms() {
        return formMetadataRepository.findAll().stream().map(form -> {
            List<FormColumn> formColumns = formColumnRepository.getByFormUuid(form.getUuid());
            return getFormMetadataDto(
                    form.getUuid(),
                    form.getName(),
                    form.getDescription(),
                    formColumns);
        }).toList();
    }

    private FormMetadataDto getFormMetadataDto(UUID formUuid, String formName, String formDescription,
                                               List<FormColumn> formColumns) {
        return new FormMetadataDto(
                formUuid,
                formName,
                formDescription,
                formColumns.stream()
                        .map(column ->
                                new FormFieldDto(
                                        column.getUuid(),
                                        column.getUserKey(),
                                        column.getFieldType(),
                                        column.getOrderNumber()
                                )
                        ).toList()
        );
    }

    public FormMetadataDto getFormAndColumnMetadata(UUID formUuid) {
        Form form = formMetadataRepository.getFormMetadataByUuid(formUuid);
        List<FormColumn> formColumns = formColumnRepository.getByFormUuid(formUuid);
        return getFormMetadataDto(
                form.getUuid(),
                form.getName(),
                form.getDescription(),
                formColumns);
    }

    public Form getFormMetadata(UUID formUuid) {
        return formMetadataRepository.getFormMetadataByUuid(formUuid);
    }

    @Transactional
    @CacheEvict(value = "formDefinitions", key = "#formUuid")
    public void softDeleteForm(UUID formUuid) {
        Form form = formMetadataRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException("Форма не найдена"));

        form.setDeletedAt(OffsetDateTime.now());
        formMetadataRepository.save(form);

        log.info("Form {} marked as deleted for company {}", formUuid, form.getCompanyUuid());
    }

    @Transactional
    @CacheEvict(value = "formDefinitions", key = "#formUuid")
    public void restoreForm(UUID formUuid) {
        UUID currentCompanyUuid = TenantContext.get();

        formMetadataRepository.restoreDeletedForm(formUuid, currentCompanyUuid);

        log.info("Form {} restored from trash for company {}", formUuid, currentCompanyUuid);
    }

    // count existing forms of the company
    private void checkQuota() {
        long currentFormsCount = formMetadataRepository.count();

        if (currentFormsCount >= maxFormsQuota) {
            throw new QuotaExceededException(
                    "Forms quota exceeded (%d of %d). Upgrade to the Business plan."
                            .formatted(currentFormsCount, maxFormsQuota)
            );
        }
    }

}
