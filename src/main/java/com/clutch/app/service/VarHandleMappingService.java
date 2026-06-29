package com.clutch.app.service;

import com.clutch.app.dto.FieldDto;
import com.clutch.app.dto.RowDto;
import com.clutch.app.entity.RowData;
import com.clutch.app.entity.FormColumn;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class VarHandleMappingService {

    // Cache: field name ("d_1") -> access descriptor
    private static final Map<String, VarHandle> POOL_COLUMN_CACHE = new ConcurrentHashMap<>();

    private final FormColumnService formColumnService;

    @PostConstruct
    public void init() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RowData.class, MethodHandles.lookup());

            // cache only pool fields from all fields of Clutch entity
            for (Field field : RowData.class.getDeclaredFields()) {
                if (isPoolColumn(field.getName())) {
                    POOL_COLUMN_CACHE.put(field.getName(), lookup.unreflectVarHandle(field));
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to initialize VarHandles for Clutch", exception);
        }
    }

    // for pool fields of Clutch entity: txt, d, n, id, t, l, b, extraData
    private boolean isPoolColumn(String name) {
        return name.matches("^[txdnilb]{1,3}_\\d+$") || name.equals("extraData");
    }

    public VarHandle getHandle(String targetColumn) {
        VarHandle handle = POOL_COLUMN_CACHE.get(targetColumn);
        if (handle == null) {
            throw new IllegalArgumentException("No field found in column pool with name: " + targetColumn);
        }
        return handle;
    }

    public void mapToEntity(List<FieldDto> fields, RowData row, Map<UUID, String> definition) {

        fields.forEach(field -> {

            if (definition.keySet().contains(field.id())) {
                String targetColumn = definition.get(field.id());
                VarHandle handle = POOL_COLUMN_CACHE.get(targetColumn);

                if (isNull(handle)) {
                    // mapping not found - put in extra_data column (JSONB)
                    FormColumn column = formColumnService.getColumn(field.id());
                    row.getExtraData().put(column.getUuid(), field.value());
                } else {
                    try {
                        // VarHandle Auto-boxing
                        handle.set(row, convertValue(field.value(), handle.varType()));
                    } catch (Exception exception) {
                        log.error("Save field value error: " +
                                "target column = " + targetColumn + " value = " + field.value().toString(), exception);
                        throw new IllegalArgumentException("Couldn't save field value: " +
                                "target column = " + targetColumn + " value = " + field.value());
                    }
                }
            } else {
                // mapping not found - put in extra_data column (JSONB)
                FormColumn column = formColumnService.getColumn(field.id());
                row.getExtraData().put(column.getUuid(), field.value());
            }

        });
    }

    public Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        // handle types
        if (targetType == OffsetDateTime.class) {
            return java.time.OffsetDateTime.parse(value.toString());
        }
        if (targetType == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(value.toString());
        }
        if (targetType == java.util.UUID.class) {
            return java.util.UUID.fromString(value.toString());
        }
        if (targetType == Double.class) {
            return Double.valueOf(value.toString());
        }
        if (targetType == Integer.class) {
            return Integer.valueOf(value.toString());
        }
        if (targetType == Long.class) {
            return Long.valueOf(value.toString());
        }

        return value;
    }

    /**
     * convert Clutch to user-friendly map
     */
    public RowDto mapFromEntity(RowData row, Map<UUID, String> definition) {

        Map<String, UUID> targetColumnToId = formColumnService.getTargetColumnToIdMapping(row.getFormUuid());

        List<FieldDto> fields = new ArrayList<>();

        Map<UUID, Object> extraData = new HashMap<>(row.getExtraData());
        extraData.entrySet().forEach(entry -> fields.add(new FieldDto(entry.getKey(), entry.getValue())));

        // form definition (title -> s_1)
        definition.forEach((columnId, targetColumn) -> {
            VarHandle handle = POOL_COLUMN_CACHE.get(targetColumn);
            if (handle != null) {
                Object value = handle.get(row);
                if (value != null) {
                    fields.add(new FieldDto(targetColumnToId.get(targetColumn), value));
                }
            }
        });

        return new RowDto(row.getUuid(), row.getOrderNumber(), fields);
    }
}
