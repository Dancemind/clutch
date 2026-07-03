package com.clutch.app.service.importdata;

import com.clutch.app.config.TenantContext;
import com.clutch.app.entity.RowData;
import com.clutch.app.service.FormColumnService;
import com.clutch.app.dto.ValidationRuleDto;
import com.clutch.app.service.ValidationService;
import com.clutch.app.service.VarHandleMappingService;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkImportService {

    private static final List<String> SYSTEM_COLUMN_NAMES =
            List.of("uuid", "company_uuid", "form_uuid", "order_number", "version", "created_at");

    private final JdbcTemplate jdbcTemplate;
    private final FormColumnService formColumnService;
    private final ValidationService validationService;
    private final VarHandleMappingService mappingService;

    /**
     * Data import (PgCopy)
     *
     * @param formUuid id of table/form
     * @param rows     map of "user column name" -> "value"
     *                 for ex, for a single row Map could be:
     *                 "Name"      -> "Pork fat"
     *                 "Quantity"  -> 150
     *                 "Price"     -> 13
     * @throws ValidationException validation exception
     */
    public void importDataPgCopy(UUID formUuid, List<Map<String, Object>> rows) throws ValidationException {
        UUID companyUuid = TenantContext.get();

        Map<UUID, String> definition = formColumnService.getIdToTargetColumnMapping(formUuid);
        Map<String, UUID> columnNameToColumnId = mapColumnNameToUserKey(definition);
        Map<String, UUID> userKeyToColumnId = formColumnService.getUserKeyToColumnIdMapping(formUuid);

        List<Map<UUID, Object>> newRows = new ArrayList<>(rows.size());
        List<ValidationRuleDto> rules = formColumnService.getValidationRules(formUuid);

        mapUserKeyToColumnIdAndValidate(formUuid, definition, rules, rows, userKeyToColumnId, newRows);

        // create copy query
        List<String> targetColumns = new ArrayList<>(definition.values());

        // collect table headers
        List<String> columnHeaders = new ArrayList<>();
        columnHeaders.addAll(SYSTEM_COLUMN_NAMES);
        columnHeaders.addAll(targetColumns);

        // initialize buffer: approx 120 bytes each
        StringBuilder csvBuffer = new StringBuilder(rows.size() * 120);

        addDataToCSVBuffer(companyUuid, formUuid, columnNameToColumnId, newRows, targetColumns, columnHeaders, csvBuffer);

        // PgCopyManager CSV
        String copyQuery = "COPY clutch ("
                + String.join(", ", columnHeaders)
                + ") FROM STDIN WITH CSV HEADER DELIMITER ',' NULL 'NULL'";

        // PgManager Copy
        streamDataToDatabase(csvBuffer, copyQuery);
    }

    private void streamDataToDatabase(StringBuilder csvBuffer, String copyQuery) {
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            CopyManager copyManager = pgConnection.getCopyAPI();

            // stream data
            try (StringReader reader = new StringReader(csvBuffer.toString())) {
                copyManager.copyIn(copyQuery, reader);
                connection.commit();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import data via PostgreSQL COPY", e);
        }
    }

    private void addDataToCSVBuffer(UUID companyUuid,
                                    UUID formUuid,
                                    Map<String, UUID> columnNameToColumnId,
                                    List<Map<UUID, Object>> newRows,
                                    List<String> targetColumns,
                                    List<String> allColumns,
                                    StringBuilder csvBuffer) {

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        String nowStr = isoFormatter.format(OffsetDateTime.now());

        // add table headers
        csvBuffer.append(String.join(",", allColumns)).append("\n");

        int orderNumber = 0;
        for (Map<UUID, Object> row : newRows) {
            orderNumber++;

            // system fields
            csvBuffer.append(UUID.randomUUID()).append(",")
                    .append(companyUuid).append(",")
                    .append(formUuid).append(",")
                    .append(orderNumber).append(",")
                    .append(0).append(",")
                    .append(nowStr);

            // dynamic fields
            for (String colName : targetColumns) {
                csvBuffer.append(",");
                UUID userKey = columnNameToColumnId.get(colName);
                Object value = row.get(userKey);

                var handle = mappingService.getHandle(colName);
                Object convertedValue = mappingService.convertValue(value, handle.varType());

                if (convertedValue == null) {
                    csvBuffer.append("NULL");
                } else {
                    String valStr = convertedValue.toString();
                    if (valStr.contains(",") || valStr.contains("\"") || valStr.contains("\n") || valStr.contains("\r")) {
                        csvBuffer.append("\"").append(valStr.replace("\"", "\"\"")).append("\"");
                    } else {
                        csvBuffer.append(valStr);
                    }
                }
            }
            csvBuffer.append("\n");
        }
    }

    private void mapUserKeyToColumnIdAndValidate(UUID formUuid,
                                                 Map<UUID, String> definition,
                                                 List<ValidationRuleDto> rules,
                                                 List<Map<String, Object>> rows,
                                                 Map<String, UUID> userKeyToColumnId,
                                                 List<Map<UUID, Object>> newRows) {
        for (Map<String, Object> row : rows) {
            Map<UUID, Object> newRow = new HashMap<>(row.size());

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String columnName = entry.getKey();
                UUID columnId = userKeyToColumnId.get(columnName);

                if (columnId == null) {
                    throw new RuntimeException("Unknown column: " + columnName);
                }
                newRow.put(columnId, entry.getValue());
            }
            newRows.add(newRow);

            // fail-fast validation
            RowData rowData = new RowData();
            rowData.setFormUuid(formUuid);
            mappingService.mapToEntity(newRow, rowData, definition);
            validationService.validate(rowData, rules);
        }
    }

    private Map<String, UUID> mapColumnNameToUserKey(Map<UUID, String> definition) {
        return definition.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

}
