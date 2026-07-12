package com.clutch.app.service;

import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.exceptions.IllegalArgumentException;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.mappers.ClutchMapper;
import com.clutch.app.repository.FormColumnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormColumnService extends BaseService<FormColumn, UUID> {

    private static final Pattern COLUMN_PATTERN = Pattern.compile("^([a-z]+_)(\\d+)$");

    private final FormColumnRepository formColumnRepository;
    private final ClutchMapper clutchMapper;

    @Override
    protected JpaRepository<FormColumn, UUID> getRepository() {
        return formColumnRepository;
    }

    @Override
    protected String getEntityName() {
        return FormColumn.class.getSimpleName();
    }

    public List<FormColumn> getColumnsMetadataByFormId(UUID formUuid) {
        return formColumnRepository.findAllByFormUuid(formUuid);
    }

    @Cacheable(value = "formColumnIdTargetColumn", key = "#formUuid")
    public Map<UUID, String> getIdToTargetColumnMapping(UUID formUuid) {
        return formColumnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUuid,
                        FormColumn::getTargetColumn
                ));
    }

    @Cacheable(value = "formUserKeyColumnId", key = "#formUuid")
    public Map<String, UUID> getUserKeyToColumnIdMapping(UUID formUuid) {
        return formColumnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUserKey,
                        FormColumn::getUuid
                ));
    }

    @Cacheable(value = "formColumnTargetColumnId", key = "#formUuid")
    public Map<String, UUID> getTargetColumnToIdMapping(UUID formUuid) {
        return formColumnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getTargetColumn,
                        FormColumn::getUuid,
                        // for "extraData" saves only last UUID, the rest omitted
                        (existing, replacement) -> replacement
                ));
    }

    public FormColumn getColumn(UUID columnUuid) {
        return formColumnRepository.getReferenceById(columnUuid);
    }

    public List<FormColumn> createFormColumns(Form form, List<FormFieldDto> fields) {
        List<FormColumn> formColumns = getFormColumns(fields, form);
        return formColumnRepository.saveAll(formColumns);
    }

    public List<FormColumn> findFormColumnsByFormUuid(UUID formUuid) {
        return formColumnRepository.findAllByFormUuid(formUuid);
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

        return clutchMapper.toFormMetadataDto(form, formColumns);
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

}
