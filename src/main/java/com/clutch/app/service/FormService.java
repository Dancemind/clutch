package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormInfoDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.exceptions.IllegalArgumentException;
import com.clutch.app.exceptions.QuotaExceededException;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.mappers.ClutchMapper;
import com.clutch.app.repository.FormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormService extends BaseService<Form, UUID> {

    // max forms/tables per one company
    @Value("${clutch.quotas.max-forms:10}")
    private int maxFormsQuota;

    private final FormRepository formRepository;
    private final FormColumnService formColumnService;
    private final ClutchMapper clutchMapper;

    @Override
    protected JpaRepository<Form, UUID> getRepository() {
        return formRepository;
    }

    @Override
    protected String getEntityName() {
        return Form.class.getSimpleName();
    }

    /**
     * Creates form and adds form fields (optionally, fields can be empty)
     *
     * @param name        form name
     * @param description form description
     * @param fields      form fields, can be empty
     * @return form metadata
     */
    @Transactional
    public FormMetadataDto createForm(String name, String description, List<FormFieldDto> fields) {

        checkQuota();

        Form form = formRepository.save(
                Form.builder()
                        .name(name)
                        .description(description)
                        .build()
        );

        List<FormColumn> formColumns = CollectionUtils.isNotEmpty(fields)
                ? formColumnService.createFormColumns(form, fields)
                : new ArrayList<>();

        return clutchMapper.toFormMetadataDto(form, formColumns);
    }

    /**
     * Creates form fields
     *
     * @param formUuid form uuid
     * @param fields   form fields to create
     * @return form metadata
     */
    @Transactional
    @CacheEvict(value = "form", key = "#formUuid")
    public FormMetadataDto createFormFields(UUID formUuid, List<FormFieldDto> fields) {
        Form form = getForm(formUuid);

        List<FormColumn> formColumns = formColumnService.createFormColumns(form, fields);

        return clutchMapper.toFormMetadataDto(form, formColumns);
    }

    public Form getForm(UUID formUuid) {
        return super.getByIdOrThrow(formUuid);
    }

    public Form getFormIncludeColumns(UUID formUuid) {
        return formRepository.findWithColumnsByUuid(formUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with UUID: %s".formatted(formUuid)));
    }

    /**
     * Gets all forms of user for the company
     *
     * @param includeColumns include form columns or not
     * @return list of forms without data
     */
    public List<FormMetadataDto> getAllForms(Boolean includeColumns) {
        boolean addColumnInfo = Boolean.TRUE.equals(includeColumns);

        List<Form> forms = addColumnInfo
                ? formRepository.findAllWithColumns()
                : formRepository.findAll();

        return forms.stream().map(form -> {
            List<FormColumn> formColumns = addColumnInfo ? form.getColumns() : List.of();
            return clutchMapper.toFormMetadataDto(form, formColumns);
        }).toList();
    }

    @Cacheable(value = "form", key = "#formUuid")
    public FormMetadataDto getFormIncludingColumns(UUID formUuid) {
        Form form = super.getByIdOrThrow(formUuid);
        List<FormColumn> formColumns = formColumnService.findFormColumnsByFormUuid(formUuid);
        return clutchMapper.toFormMetadataDto(form, formColumns);
    }

    @Transactional
    @CacheEvict(value = "form", key = "#formUuid")
    public Form deleteForm(UUID formUuid) {
        Form form = super.getByIdOrThrow(formUuid);

        form.setDeletedAt(OffsetDateTime.now());
        Form deletedForm = formRepository.save(form);

        log.info("Form {} marked as deleted for company {}", formUuid, form.getCompanyUuid());

        return deletedForm;
    }

    /**
     * Find list of deleted forms of company for user of the company
     * @return list of forms
     */
    @Transactional(readOnly = true)
    public List<Form> findDeletedFormByCompany() {
        UUID companyUuid = TenantContext.get()
                .orElseThrow(() -> new IllegalStateException("Security violation: Tenant context is missing"));
        return formRepository.findDeletedFormsByCompany(companyUuid);
    }

    /**
     * Find list of inactive forms of company for company admin
     * @return list of forms
     */
    @Transactional(readOnly = true)
    public List<Form> findInactiveFormsByCompany() {
        return formRepository.findAllByIsActiveFalse();
    }

    @Transactional
    public Form restoreForm(UUID formUuid) {
        checkQuota();

        UUID companyUuid = TenantContext.get()
                .orElseThrow(() -> new IllegalStateException("Security violation: Tenant context is missing"));

        formRepository.restoreDeletedForm(formUuid, companyUuid);

        log.info("Form {} restored from trash for company {}", formUuid, companyUuid);

        return getForm(formUuid);
    }

    /**
     * Counts existing forms of the company, not deleted
     */
    private void checkQuota() {
        long currentFormsCount = formRepository.count();

        if (currentFormsCount >= maxFormsQuota) {
            throw new QuotaExceededException(
                    "Forms quota exceeded (%d of %d). Upgrade to the Business plan."
                            .formatted(currentFormsCount, maxFormsQuota)
            );
        }
    }

    /**
     * Updates name or/and description of Form by form uuid
     *
     * @param formInfoDto form info
     * @return form metadata
     */
    @Transactional
    public Form updateForm(FormInfoDto formInfoDto) {
        if (isNull(formInfoDto.uuid())) {
            throw new IllegalArgumentException("Form uuid is empty");
        }
        Form form = getForm(formInfoDto.uuid());
        form.setName(formInfoDto.formName());
        form.setDescription(formInfoDto.formDescription());
        return formRepository.save(form);
    }

    /**
     * Updates form columns
     *
     * @param formUuid       form uuid
     * @param formColumnDtos form columns to update
     * @return form metadata
     */
    @Transactional
    public FormMetadataDto updateFormColumns(UUID formUuid, List<FormColumnDto> formColumnDtos) {
        Form form = getFormIncludeColumns(formUuid);

        Map<UUID, FormColumnDto> uuidToUpdateColumn;
        try {
            uuidToUpdateColumn = formColumnDtos.stream()
                    .collect(Collectors.toMap(FormColumnDto::uuid, dto -> dto));
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(
                    "Error for update form columns. List of columns contains duplicated uuids");
        }

        List<FormColumn> formColumns = form.getColumns();
        Set<UUID> existingFormColumnUuids = formColumns.stream()
                .map(FormColumn::getUuid)
                .collect(Collectors.toSet());

        List<UUID> wrongFormColumnUuids = uuidToUpdateColumn.keySet().stream()
                .filter(uuid -> !existingFormColumnUuids.contains(uuid))
                .toList();

        if (!wrongFormColumnUuids.isEmpty()) {
            throw new IllegalArgumentException(
                    "Form %s. Can't update. Wrong columns uuids: %s".formatted(formUuid, wrongFormColumnUuids)
            );
        }

        formColumns.forEach(formColumn -> {
            FormColumnDto updateDto = uuidToUpdateColumn.get(formColumn.getUuid());
            if (updateDto != null) {
                formColumn.setUserKey(updateDto.label());
                formColumn.setOrderNumber(updateDto.orderNumber());
            }
        });

//        List<FormColumn> updatedFormColumns = formColumnRepository.saveAll(formColumns);

        return clutchMapper.toFormMetadataDto(form, formColumns);
    }

}
