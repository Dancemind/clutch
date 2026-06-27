package com.clutch.app.controller;

import com.clutch.app.config.TenantContext;
import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.dto.response.form.FormDto;
import com.clutch.app.entity.Form;
import com.clutch.app.enums.FieldType;
import com.clutch.app.mappers.ClutchMapper;
import com.clutch.app.repository.FormRepository;
import com.clutch.app.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;
    private final FormRepository formRepository;
    private final ClutchMapper clutchMapper;

    /**
     * Creates form and optionally creates columns
     *
     * @param formMetadata form metadata
     * @return form metadata
     */
    @PostMapping
    public FormMetadataDto createForm(@RequestBody FormMetadataDto formMetadata) {
        return formService.createForm(formMetadata.name(), formMetadata.description(), formMetadata.fields());
    }

    /**
     * Creates form columns
     *
     * @param uuid          form id
     * @param formFieldDtos form fields info
     * @return form metadata
     */
    @PostMapping("columns")
    public FormMetadataDto createForm(@RequestParam("id") UUID uuid,
                                      @RequestBody List<FormFieldDto> formFieldDtos) {
        return formService.createFormFields(uuid, formFieldDtos);
    }

    /**
     * Gets available field types for form field
     *
     * @return field types
     */
    @GetMapping("field-types")
    public List<FieldType> getAvailableFieldTypes() {
        return Arrays.asList(FieldType.values());
    }

    /**
     * Gets form name, description and columns info (uuid, type, name, order orderNumber)
     *
     * @param formUuid form uuid
     * @return form metadata
     */
    @GetMapping
    public FormMetadataDto getForm(@RequestParam("uuid") UUID formUuid) {
        return formService.getFormIncludingColumns(formUuid);
    }

    /**
     * Gets all forms of the company
     *
     * @param includeColumns include columns info?
     * @return list of forms
     */
    @GetMapping("all")
    public List<FormMetadataDto> getAllForms(@RequestParam(required = false) Boolean includeColumns) {
        return formService.getAllForms(includeColumns);
    }

    /**
     * Updates form (name, description)
     */
    @PatchMapping("{uuid}")
    public FormMetadataDto updateForm(@PathVariable UUID uuid,
                                      @RequestBody FormDto formDto) {
        return clutchMapper.toFormMetadataDto(
                formService.updateForm(uuid, formDto),
                List.of()
        );
    }

    /**
     * Updates form column
     */
    @PatchMapping("{formUuid}/columns/{columnUuid}")
    public FormMetadataDto updateFormColumn(@PathVariable UUID formUuid,
                                            @PathVariable UUID columnUuid,
                                            @RequestBody FormColumnDto formColumnDto) {
        return formService.updateFormColumn(formUuid, columnUuid, formColumnDto);
    }

    /**
     * Updates form column
     */
    @PatchMapping("{formUuid}/columns")
    public FormMetadataDto updateFormColumn(@PathVariable UUID formUuid,
                                            @RequestBody List<FormColumnDto> formColumnDtos) {
        return formService.updateFormColumns(formUuid, formColumnDtos);
    }

    /**
     * Removes forms
     *
     * @return list of deleted forms
     */
    @DeleteMapping("/forms")
    public Form deletedForm(UUID formUuid) {
        return formService.deleteForm(formUuid);
    }

    /**
     * Gets deleted froms of the company
     *
     * @return list of forms
     */
    @GetMapping("/forms/trash")
    public List<Form> getDeletedForms() {
        return formRepository.findDeletedFormsByCompany(TenantContext.get());
    }

    /**
     * Restores form
     *
     * @param formUuid form uuid
     */
    @PostMapping("/forms/restore")
    public void restoreForm(@RequestParam UUID formUuid) {
        formService.restoreForm(formUuid);
    }
}
