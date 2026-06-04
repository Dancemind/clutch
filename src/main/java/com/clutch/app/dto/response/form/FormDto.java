package com.clutch.app.dto.response.form;

import java.util.List;

public record FormDto(
        // Form metadata
        String uuid,
        String name,
        String description,

        // Form metadata - columns metadata
        List<FormColumnDto> columns,

        // Form data by row
        List<FormRowDto> rows
) {
}
