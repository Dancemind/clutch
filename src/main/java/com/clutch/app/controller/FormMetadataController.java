package com.clutch.app.controller;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.entity.Form;
import com.clutch.app.enums.FieldType;
import com.clutch.app.repository.FormMetadataRepository;
import com.clutch.app.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata/forms")
@RequiredArgsConstructor
public class FormMetadataController {

    private final FormService formService;
    private final FormMetadataRepository formMetadataRepository;

    /**
     * Возвращает список доступных типов колонок для конструктора таблиц
     */
    @GetMapping("field-types")
    public List<FieldType> getAvailableFieldTypes() {
        return Arrays.asList(FieldType.values());
    }

    /**
     * Создание новой таблицы (формы) пользователем
     */
    @PostMapping
    public FormMetadataDto createForm(@RequestBody FormMetadataDto formMetadata) {
        return formService.createForm(formMetadata.name(), formMetadata.description(), formMetadata.fields());
    }

    /**
     * Получение заголовка таблицы и названий колонок
     */
    @PostMapping("id")
    public FormMetadataDto getFormMetadata(@RequestBody UUID formUuid) {
        return formService.getFormAndColumnMetadata(formUuid);
    }

    /**
     * Получение структуры всех таблиц текущей компании
     */
    @GetMapping
    public List<FormMetadataDto> getAllFormsMetadata() {
        return formService.getAllForms();
    }

    @GetMapping("/forms/trash")
    public List<Form> getTrash() {
        return formMetadataRepository.findTrash(TenantContext.get());
    }

    @PostMapping("/forms/restore")
    public void restore(@RequestBody UUID formUuid) {
        formService.restoreForm(formUuid);
    }
}
