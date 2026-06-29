package com.clutch.app.controller;

import com.clutch.app.dto.RowDto;
import com.clutch.app.dto.response.form.FormDto;
import com.clutch.app.service.FormDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class FormDataController {

    private final FormDataService formDataService;

    /**
     * Adds rows (data by columns)
     *
     * @param formUuid form uuid
     * @param rows     rows data
     * @return list of created rows
     */
    @PostMapping("forms/{formUuid}/rows")
    public List<RowDto> addRows(@PathVariable UUID formUuid,
                                @RequestBody List<RowDto> rows) {
        return formDataService.createRows(formUuid, rows);
    }

    /**
     * Gets form and its data
     *
     * @param formUuid form uuid
     * @return form and its data
     */
    @GetMapping("forms/{formUuid}/rows")
    public FormDto getFormData(@PathVariable UUID formUuid) {
        return formDataService.getFormData(formUuid);
    }

    /**
     * Updates row data
     *
     * @param rowUuid row uuid
     * @param rowDto  row data to update
     * @return updated row
     */
    @PatchMapping("rows/{rowUuid}")
    public RowDto updateRow(@PathVariable UUID rowUuid,
                            @RequestBody RowDto rowDto) {
        return formDataService.updateRowData(rowUuid, rowDto);
    }

}
