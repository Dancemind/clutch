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
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClutchServiceTest extends BaseServiceIntegrationTest {

    @Autowired
    private ClutchService clutchService;

    @Autowired
    private FormService formService;

    @Test
    @Transactional
    void createRows() throws ValidationException, NoSuchFieldException {

        String formName = "valenki";
        String formDescription = "vidy valenkov";

        String nameColumn = "name";
        String sizeColumn = "size";
        String priceColumn = "price";


        List<FormFieldDto> formFields = List.of(
                new FormFieldDto(null, nameColumn, FieldType.TEXT, 1),
                new FormFieldDto(null, sizeColumn, FieldType.NUMBER, 2),
                new FormFieldDto(null, priceColumn, FieldType.MONEY, 3)
        );

        // create metadata form
        FormMetadataDto formCreated = formService.createForm(formName, formDescription, formFields);

        UUID nameColumnUUID = formCreated.fields().stream()
                .filter(column -> Objects.equals(column.name(), nameColumn))
                .map(FormFieldDto::id)
                .findFirst().orElseThrow(
                        () -> new NoSuchFieldException(String.join("No field found", nameColumn))
                );

        UUID sizeColumnUUID = formCreated.fields().stream()
                .filter(column -> Objects.equals(column.name(), sizeColumn))
                .map(FormFieldDto::id)
                .findFirst().orElseThrow(
                        () -> new NoSuchFieldException(String.join("No field found", sizeColumn))
                );

        UUID priceColumnUUID = formCreated.fields().stream()
                .filter(column -> Objects.equals(column.name(), priceColumn))
                .map(FormFieldDto::id)
                .findFirst().orElseThrow(
                        () -> new NoSuchFieldException(String.join("No field found", priceColumn))
                );


        String goodOneName = "valenok one";
        String goodTwoName = "valenok two";
        String goodThreeName = "valenok three";

        List<RowDto> rowsData = List.of(
                new RowDto(
                        UUID.randomUUID(),
                        1L,
                        List.of(
                                new FieldDto(nameColumnUUID, goodOneName),
                                new FieldDto(sizeColumnUUID, 46),
                                new FieldDto(priceColumnUUID, 77.90)
                        )),
                new RowDto(
                        UUID.randomUUID(),
                        2L,
                        List.of(
                                new FieldDto(nameColumnUUID, goodTwoName),
                                new FieldDto(sizeColumnUUID, 41),
                                new FieldDto(priceColumnUUID, 74.90)
                        )),
                new RowDto(
                        UUID.randomUUID(),
                        3L,
                        List.of(
                                new FieldDto(nameColumnUUID, goodThreeName),
                                new FieldDto(sizeColumnUUID, 48),
                                new FieldDto(priceColumnUUID, 79.90)
                        ))
        );


        // add data
        List<RowDto> rows = clutchService.createRows(formCreated.formUuid(), rowsData);


        // check number of rows
        FormDto form = clutchService.getForm(formCreated.formUuid());
        assertThat(form.uuid()).isEqualTo(formCreated.formUuid().toString());
        assertThat(form.rows().size()).isEqualTo(rowsData.size());

        // data fields list of created form contains name of goods
        List<Object> values = rows.stream()
                .flatMap(row -> row.fieldsData().stream())
                .map(FieldDto::value)
                .toList();
        assertThat(values)
                .containsAll(List.of(goodOneName, goodTwoName, goodThreeName));

    }

}
