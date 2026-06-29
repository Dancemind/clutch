package com.clutch.app.dto.response.form;

import java.util.List;
import java.util.UUID;

/**
 * Dto to return all form data.
 * Includes form info, form columns info and form data (data in forms columns that user sees)
 * @param uuid form uuid
 * @param name form name
 * @param description form description
 * @param columns columns info (uuid, label, type, order number)
 * @param rows rows data (row uuid, row data as map - column uuid : cell value)
 */
public record FormDto(

        // Form metadata
        String uuid,
        String name,
        String description,

        UUID companyUuid,
        UUID projectUuid,

        // Form columns metadata
        List<FormColumnDto> columns,

        // Form data by row
        List<FormRowDto> rows
) {
}
