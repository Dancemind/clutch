package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.FormField;
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

    // Лимит таблиц на одну компанию (берем из application.yml)
    @Value("${clutch.quotas.max-forms:10}")
    private int maxFormsQuota;

    private final FormMetadataRepository formMetadataRepository;
    private final ColumnDefinitionRepository formColumnRepository;

    @Transactional
//    @CacheEvict(value = "formDefinitions", key = "#resultId") // Инвалидация кэша при создании/обновлении
    public FormMetadataDto createForm(String name, String description, List<FormField> fields) {
        // checks if quota on create table is exceeded for the company
        checkQuota();

        // 1. Создаем заголовок формы
        Form form = Form.builder()
                .name(name)
                .description(description)
                .build();
        Form formSaved = formMetadataRepository.save(form); // Здесь проставится ID и company_id

        // 2. Распределяем колонки из пула (d_1, s_1...)
        AtomicInteger dCount = new AtomicInteger(1);
        AtomicInteger tCount = new AtomicInteger(1);
        AtomicInteger lCount = new AtomicInteger(1);
        AtomicInteger iCount = new AtomicInteger(1);
        AtomicInteger sCount = new AtomicInteger(1);
        AtomicInteger ttCount = new AtomicInteger(1);
        AtomicInteger bCount = new AtomicInteger(1);

        List<FormColumn> definitions = fields.stream().map(field -> {
            String columnName = switch (field.type()) {
                case MONEY, NUMBER -> (dCount.get() <= 10) ? "d_" + dCount.getAndIncrement() : "extra_data";
                case DATE -> (tCount.get() <= 10) ? "t_" + tCount.getAndIncrement() : "extra_data";
                case LINK -> (lCount.get() <= 10) ? "l_" + lCount.getAndIncrement() : "extra_data";
                case BUSINESS_ID -> (iCount.get() <= 10) ? "id_" + iCount.getAndIncrement() : "extra_data";
                case SHORT_TEXT -> (dCount.get() <= 10) ? "s_" + sCount.getAndIncrement() : "extra_data";
                case TEXT -> (dCount.get() <= 10) ? "txt_" + ttCount.getAndIncrement() : "extra_data";
                case FLAG -> (bCount.get() <= 10) ? "b_" + dCount.getAndIncrement() : "extra_data";

                default -> "extra_data"; // Если тип неизвестен или пул кончился
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


    //  Получение списка форм пользователя текущей компании,
    //  чтобы фронтенд мог отрисовать боковое меню с таблицами пользователя
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
                formColumns.stream().map(column ->
                        new FormField(column.getUuid(), column.getUserKey(), column.getFieldType(), column.getOrderNumber())
                ).toList());
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
        UUID currentCompanyUuid = TenantContext.get(); // Наш ScopedValue из Java 25

        // Выполняем апдейт. Если форма была удалена — она снова станет активной.
        formMetadataRepository.restoreDeletedForm(formUuid, currentCompanyUuid);

        log.info("Form {} restored from trash by company {}", formUuid, currentCompanyUuid);
    }

    // Благодаря @TenantId, count() вернет количество таблиц ТОЛЬКО текущей компании
    private void checkQuota() {
        long currentFormsCount = formMetadataRepository.count();

        if (currentFormsCount >= maxFormsQuota) {
            throw new QuotaExceededException(
                    "Лимит таблиц исчерпан (%d из %d). Перейдите на тариф Business."
                            .formatted(currentFormsCount, maxFormsQuota)
            );
        }
    }

}
