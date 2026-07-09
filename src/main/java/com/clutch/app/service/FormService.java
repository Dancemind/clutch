package com.clutch.app.service;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.FormInfoDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.exceptions.IllegalArgumentException;
import com.clutch.app.exceptions.QuotaExceededException;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.repository.FormColumnRepository;
import com.clutch.app.repository.FormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormService {

    private static final Pattern COLUMN_PATTERN = Pattern.compile("^([a-z]+_)(\\d+)$");

    // max forms/tables per one company
    @Value("${clutch.quotas.max-forms:10}")
    private int maxFormsQuota;

    private final FormRepository formRepository;
    private final FormColumnRepository formColumnRepository;

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
                ? createFormColumns(form, fields)
                : new ArrayList<>();

        return getFormMetadataDto(form, formColumns);
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

        List<FormColumn> formColumns = createFormColumns(form, fields);

        return getFormMetadataDto(form, formColumns);
    }

    public Form getForm(UUID formUuid) {
        return formRepository.findByUuid(formUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with UUID: %s".formatted(formUuid)));
    }

    public Form getFormIncludeColumns(UUID formUuid) {
        return formRepository.findWithColumnsByUuid(formUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with UUID: %s".formatted(formUuid)));
    }

    private List<FormColumn> createFormColumns(Form form, List<FormFieldDto> fields) {
        List<FormColumn> formColumns = getFormColumns(fields, form);
        return formColumnRepository.saveAll(formColumns);
    }

    /**
     * Maps user-friendly name of form fields to type-defined field name in Clutch
     *
     * @param fields    form fields
     * @param formSaved form
     * @return form columns
     */
    private List<FormColumn> getFormColumns(List<FormFieldDto> fields, Form formSaved) {
        List<FormColumn> formColumns = formColumnRepository.findAllByFormUuid(formSaved.getUuid());

        int dMax = 0;
        int nMax = 0;
        int tMax = 0;
        int lMax = 0;
        int iMax = 0;
        int ttMax = 0;
        int bMax = 0;

        for (FormColumn formColumn : formColumns) {
            String targetColumn = formColumn.getTargetColumn();

            if (isNull(targetColumn)) {
                continue;
            }

            Matcher matcher = COLUMN_PATTERN.matcher(targetColumn);

            if (matcher.matches()) {
                String prefix = matcher.group(1);
                int number = Integer.parseInt(matcher.group(2));

                switch (prefix) {
                    case "d_" -> dMax = Math.max(dMax, number);
                    case "n_" -> nMax = Math.max(nMax, number);
                    case "t_" -> tMax = Math.max(tMax, number);
                    case "l_" -> lMax = Math.max(lMax, number);
                    case "id_" -> iMax = Math.max(iMax, number);
                    case "txt_" -> ttMax = Math.max(ttMax, number);
                    case "b_" -> bMax = Math.max(bMax, number);

                    default -> log.warn("Prefix is unknown for column: {}", targetColumn);
                }
            } else if (!"extra_data".equals(targetColumn)) {
                log.warn("Target column format is invalid: {}", targetColumn);
            }
        }

        dMax++;
        nMax++;
        tMax++;
        lMax++;
        iMax++;
        ttMax++;
        bMax++;

        List<FormColumn> result = new ArrayList<>(fields.size());

        for (FormFieldDto field : fields) {

            String columnName = switch (field.type()) {
                case MONEY -> (dMax <= 4) ? "d_" + dMax++ : "extra_data";
                case NUMBER -> (nMax <= 4) ? "n_" + nMax++ : "extra_data";
                case DATE -> (tMax <= 5) ? "t_" + tMax++ : "extra_data";
                case LINK -> (lMax <= 5) ? "l_" + lMax++ : "extra_data";
                case BUSINESS_ID -> (iMax <= 5) ? "id_" + iMax++ : "extra_data";
                case TEXT -> (ttMax <= 15) ? "txt_" + ttMax++ : "extra_data";
                case FLAG -> (bMax <= 5) ? "b_" + bMax++ : "extra_data";

                default -> "extra_data"; // if type is unknown or the field poll exceeded
            };

            result.add(FormColumn.builder()
                    .form(formSaved)
                    .userKey(field.name())
                    .fieldType(field.type())
                    .targetColumn(columnName)
                    .orderNumber(field.orderNumber())
                    .build());
        }
        return result;
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
            return getFormMetadataDto(form, formColumns);
        }).toList();
    }

    private FormMetadataDto getFormMetadataDto(Form form, List<FormColumn> formColumns) {
        return getFormMetadataDto(
                form.getUuid(),
                form.getName(),
                form.getDescription(),
                formColumns);
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

    @Cacheable(value = "form", key = "#formUuid")
    public FormMetadataDto getFormIncludingColumns(UUID formUuid) {
        Form form = getForm(formUuid);
        List<FormColumn> formColumns = formColumnRepository.findAllByFormUuid(formUuid);
        return getFormMetadataDto(form, formColumns);
    }

    @Transactional
    @CacheEvict(value = "form", key = "#formUuid")
    public Form deleteForm(UUID formUuid) {
        Form form = formRepository.findById(formUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        form.setDeletedAt(OffsetDateTime.now());
        Form deletedForm = formRepository.save(form);

        log.info("Form {} marked as deleted for company {}", formUuid, form.getCompanyUuid());

        return deletedForm;
    }

    @Transactional(readOnly = true)
    public List<Form> findDeletedFormByCompany() {
        UUID companyUuid = TenantContext.get()
                .orElseThrow(() -> new IllegalStateException("Security violation: Tenant context is missing"));
        return formRepository.findDeletedFormsByCompany(companyUuid);
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
     * Updates form column: userKey (label) and orderNumber
     * FieldType can't be updated
     *
     * @param formUuid      form uuid
     * @param columnUuid    column uuid
     * @param formColumnDto form column info to update
     * @return form metadata
     */
    @Transactional
    public FormMetadataDto updateFormColumn(UUID formUuid, UUID columnUuid, FormColumnDto formColumnDto) {
        FormColumn formColumn = formColumnRepository.findWithFormByUuid(columnUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found with UUID: %s".formatted(columnUuid)));

        Form form = formColumn.getForm();

        if (!formUuid.equals(form.getUuid())) {
            throw new IllegalArgumentException("No column %s found for form %s".formatted(columnUuid, formUuid));
        }

        formColumn.setUserKey(formColumnDto.label());
        formColumn.setOrderNumber(formColumnDto.orderNumber());

        List<FormColumn> formColumns = formColumnRepository.findAllByFormUuid(formUuid);

        return getFormMetadataDto(form, formColumns);
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

        return getFormMetadataDto(form, formColumns);
    }

}
