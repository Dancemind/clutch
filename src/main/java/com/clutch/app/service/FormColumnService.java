package com.clutch.app.service;

import com.clutch.app.dto.ValidationRuleDto;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.repository.FormColumnRepository;
import com.clutch.app.repository.FormRepository;
import com.clutch.app.repository.ValidationRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormColumnService {

    public static final String NOT_FOUND_MSG = "Form is not found";

    private final FormColumnRepository formColumnRepository;
    private final ValidationRuleRepository ruleRepository;
    private final FormRepository formRepository;

    public List<FormColumn> getColumnsMetadataByFormId(UUID formUuid) {
        return formColumnRepository.findAllByFormUuid(formUuid);
    }

    @Cacheable(value = "formColumnIdTargetColumn", key = "#formUuid")
    public Map<UUID, String> getIdToTargetColumnMapping(UUID formUuid) {
        // todo: separate form deleted and form never existed
        formRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MSG));

        return formColumnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUuid,
                        FormColumn::getTargetColumn
                ));
    }

    @Cacheable(value = "formUserKeyColumnId", key = "#formUuid")
    public Map<String, UUID> getUserKeyToColumnIdMapping(UUID formUuid) {
        // todo: separate form deleted and form never existed
        formRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MSG));

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

    @Cacheable(value = "formRules", key = "#formUuid")
    public List<ValidationRuleDto> getValidationRules(UUID formUuid) {
        return ruleRepository.findAllByFormUuid(formUuid).stream()
                .map(entity -> new ValidationRuleDto(
                        entity.getTargetColumn(),
                        entity.getRuleType(),
                        entity.getRuleValue(),
                        entity.getMessage()
                ))
                .toList();
    }

    public FormColumn getColumn(UUID columnUuid) {
        return formColumnRepository.getReferenceById(columnUuid);
    }
}

