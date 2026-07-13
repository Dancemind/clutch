package com.clutch.app.controller;

import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormInfoDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.response.form.FormColumnDto;
import com.clutch.app.enums.FieldType;
import com.clutch.app.mappers.ClutchMapper;
import com.clutch.app.service.FormColumnService;
import com.clutch.app.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;
    private final FormColumnService formColumnService;
    private final ClutchMapper clutchMapper;

    /**
     * Creates form and optionally creates columns
     *
     * @param formMetadata form metadata
     * @return form metadata
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'COMPANY_ADMIN')")
    public FormMetadataDto createFormAndColumns(@RequestBody FormMetadataDto formMetadata) {
        return formService.createForm(
                formMetadata.name(),
                formMetadata.description(),
                formMetadata.projectUuid(),
                formMetadata.fields());
    }

    /**
     * Creates form columns
     *
     * @param uuid          form id
     * @param formFieldDtos form fields info
     * @return form metadata
     */
    @PostMapping("columns")
    public FormMetadataDto createFormColumns(@RequestParam("id") UUID uuid,
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
     *
     * @param formInfoDto form data to update
     * @return form metadata, includes columns data
     */
    @PatchMapping("{uuid}")
    public FormMetadataDto updateForm(@RequestBody FormInfoDto formInfoDto) {
        return clutchMapper.toFormMetadataDto(
                formService.updateForm(formInfoDto),
                List.of()
        );
    }

    /**
     * Updates form column
     *
     * @param formUuid form uuid
     * @param columnUuid column uuid
     * @param formColumnDto form column data
     * @return form metadata, includes columns data
     */
    @PatchMapping("{formUuid}/columns/{columnUuid}")
    public FormMetadataDto updateFormColumn(@PathVariable UUID formUuid,
                                            @PathVariable UUID columnUuid,
                                            @RequestBody FormColumnDto formColumnDto) {
        return formColumnService.updateFormColumn(formUuid, columnUuid, formColumnDto);
    }

    /**
     * Updates form columns
     *
     * @param formUuid form uuid
     * @param formColumnDtos form columns data to update
     * @return form metadata, includes columns data
     */
    @PatchMapping("{formUuid}/columns")
    public FormMetadataDto updateFormColumns(@PathVariable UUID formUuid,
                                            @RequestBody List<FormColumnDto> formColumnDtos) {
        return formService.updateFormColumns(formUuid, formColumnDtos);
    }

    /**
     * Removes form
     *
     * @param formUuid form uuid
     * @return form
     */
    @DeleteMapping("/forms")
    public FormInfoDto deletedForm(UUID formUuid) {
        return clutchMapper.toFormInfoDto(
                formService.deleteForm(formUuid)
        );
    }

    /**
     * Gets deleted froms of the company
     *
     * @return list of forms
     */
    @GetMapping("/forms/trash")
    public List<FormInfoDto> getDeletedForms() {
        return clutchMapper.toFormInfoDto(
              formService.findDeletedFormByCompany()
        );
    }

    /**
     * Restores deleted form
     *
     * @param formUuid form uuid
     */
    @PostMapping("/forms/restore")
    public FormInfoDto restoreForm(@RequestParam UUID formUuid) {
        return clutchMapper.toFormInfoDto(
                formService.restoreForm(formUuid)
        );
    }
}
