package com.clutch.app;

import com.clutch.app.dto.FieldDto;
import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.RowDto;
import com.clutch.app.dto.response.form.FormDto;
import com.clutch.app.enums.FieldType;
import com.clutch.app.service.ClutchService;
import com.clutch.app.service.FormService;
import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FormDataFlowIntegrationTest extends BaseServiceIntegrationTest {

    @Autowired
    private ClutchService clutchService;

    @Autowired
    private FormService formService;

    @Test
    @Transactional
    void createRows_retrieveThemSuccessfully() throws ValidationException {

        // 1. Arrange
        FormMetadataDto formMetadata = createTestFormMetadata();
        UUID nameCol = getFieldUuid(formMetadata, "name");
        UUID sizeCol = getFieldUuid(formMetadata, "size");
        UUID priceCol = getFieldUuid(formMetadata, "price");

        List<RowDto> rowsToCreate = List.of(
                createRow(1L, nameCol, "valenok one", sizeCol, 46, priceCol, 77.90),
                createRow(2L, nameCol, "valenok two", sizeCol, 41, priceCol, 74.90),
                createRow(3L, nameCol, "valenok three", sizeCol, 48, priceCol, 79.90)
        );


        // 2. Act
        List<RowDto> createdRows = clutchService.createRows(formMetadata.formUuid(), rowsToCreate);


        // 3. Assert
        FormDto formResult = clutchService.getForm(formMetadata.formUuid());

        assertThat(formResult.uuid()).isEqualTo(formMetadata.formUuid().toString());
        assertThat(formResult.rows()).hasSameSizeAs(rowsToCreate);

        List<Object> fieldValues = createdRows.stream()
                .flatMap(row -> row.fieldsData().stream())
                .map(FieldDto::value)
                .toList();

        assertThat(fieldValues).contains("valenok one", "valenok two", "valenok three");
    }

    private FormMetadataDto createTestFormMetadata() {
        List<FormFieldDto> fields = List.of(
                new FormFieldDto(null, "name", FieldType.TEXT, 1),
                new FormFieldDto(null, "size", FieldType.NUMBER, 2),
                new FormFieldDto(null, "price", FieldType.MONEY, 3)
        );
        return formService.createForm("valenki", "vidy valenkov", fields);
    }

    private UUID getFieldUuid(FormMetadataDto metadata, String fieldName) {
        return metadata.fields().stream()
                .filter(f -> f.name().equals(fieldName))
                .map(FormFieldDto::uuid)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + fieldName));
    }

    private RowDto createRow(Long orderNumber, UUID idOne, Object valueOne,
                             UUID idTwo, Object valueTwo, UUID idThree, Object valueThree) {
        return new RowDto(
                UUID.randomUUID(),
                orderNumber,
                List.of(
                        new FieldDto(idOne, valueOne),
                        new FieldDto(idTwo, valueTwo),
                        new FieldDto(idThree, valueThree)
                ));
    }

}
