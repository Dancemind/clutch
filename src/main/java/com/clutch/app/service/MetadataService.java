package com.clutch.app.service;

import com.clutch.app.entity.FormColumn;
import com.clutch.app.repository.ColumnDefinitionRepository;
import com.clutch.app.repository.FormMetadataRepository;
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
public class MetadataService {

    public static final String NOT_FOUND_MSG = "Form is not found";

    private final ColumnDefinitionRepository columnRepository;
    private final ValidationRuleRepository ruleRepository;
    private final FormMetadataRepository formMetadataRepository;

    public List<FormColumn> getColumnsMetadataByFormId(UUID formUuid) {
        return columnRepository.findAllByFormUuid(formUuid);
    }

    @Cacheable(value = "formColumnIdTargetColumn", key = "#formUuid")
    public Map<UUID, String> getIdToTargetColumnMapping(UUID formUuid) {
        // todo: separate form deleted and form never existed
        formMetadataRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MSG));

        return columnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUuid,
                        FormColumn::getTargetColumn
                ));
    }

    @Cacheable(value = "formUserKeyColumnId", key = "#formUuid")
    public Map<String, UUID> getUserKeyToColumnIdMapping(UUID formUuid) {
        // todo: separate form deleted and form never existed
        formMetadataRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MSG));

        return columnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUserKey,
                        FormColumn::getUuid
                ));
    }

    @Cacheable(value = "formColumnTargetColumnId", key = "#formUuid")
    public Map<String, UUID> getTargetColumnToIdMapping(UUID formUuid) {
        return columnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getTargetColumn,
                        FormColumn::getUuid
                ));
    }

    @Cacheable(value = "formRules", key = "#formUuid")
    public List<ValidationRule> getValidationRules(UUID formUuid) {
        return ruleRepository.findAllByFormUuid(formUuid).stream()
                .map(entity -> new ValidationRule(
                        entity.getTargetColumn(),
                        entity.getRuleType(),
                        entity.getRuleValue(),
                        entity.getMessage()
                ))
                .toList();
    }

    public FormColumn getColumn(UUID columnUuid) {
        return columnRepository.getReferenceById(columnUuid);
    }
}

