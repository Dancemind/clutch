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

    private final ColumnDefinitionRepository columnRepository;
    private final ValidationRuleRepository ruleRepository;
    private final FormMetadataRepository formMetadataRepository;

    public List<FormColumn> getColumnsMetadataByFormId(UUID formUuid) {
        return columnRepository.findAllByFormUuid(formUuid);
    }

    // Кэшируем маппинг по formId. При изменении формы — делаем CacheEvict.
    @Cacheable(value = "formColumnIdTargetColumn", key = "#formUuid")
    public Map<UUID, String> getIdToTargetColumnMapping(UUID formUuid) {
        // 1. Проверяем, существует ли форма и не удалена ли она
        // findById вернет Empty, если форма в корзине
        // todo: может проверить isExist, чтобы различать удалена форма или такого айди не существует?
        formMetadataRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException("Форма не найдена или находится в корзине"));

        // 2. Если форма не удалена, возвращаем маппинг колонок
        return columnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUuid,
                        FormColumn::getTargetColumn
                ));
    }

    // Кэшируем маппинг по formId. При изменении формы — делаем CacheEvict.
    @Cacheable(value = "formUserKeyColumnId", key = "#formUuid")
    public Map<String, UUID> getUserKeyToColumnIdMapping(UUID formUuid) {
        // 1. Проверяем, существует ли форма и не удалена ли она
        // todo: различать удалена форма или такого айди не существует
        formMetadataRepository.findById(formUuid)
                .orElseThrow(() -> new EntityNotFoundException("Форма не найдена или находится в корзине"));

        // 2. Если форма не удалена, возвращаем маппинг колонок
        return columnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getUserKey,
                        FormColumn::getUuid
                ));
    }

    // Кэшируем маппинг по formId. При изменении формы — делаем CacheEvict.
    @Cacheable(value = "formColumnTargetColumnId", key = "#formUuid")
    public Map<String, UUID> getTargetColumnToIdMapping(UUID formUuid) {
        return columnRepository.findAllByFormUuid(formUuid).stream()
                .collect(Collectors.toMap(
                        FormColumn::getTargetColumn,
                        FormColumn::getUuid
                ));
    }

//    @Cacheable(value = "formColumnIdUserKey", key = "#formUuid")
//    public Map<UUID, String> getIdToUserColumnNameMapping(UUID formUuid) {
//        return columnRepository.findAllByFormUuid(formUuid).stream()
//                .collect(Collectors.toMap(
//                        FormColumn::getUuid,
//                        FormColumn::getUserKey
//                ));
//    }

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

