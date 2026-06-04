package com.clutch.app.service;

import com.clutch.app.entity.Clutch;
import com.clutch.app.entity.FormColumn;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VarHandleMappingService {

    // Кэш: имя поля (например, "d_1") -> его дескриптор доступа
    private static final Map<String, VarHandle> POOL_COLUMN_CACHE = new ConcurrentHashMap<>();

    private final MetadataService metadataService;

    @PostConstruct
    public void init() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Clutch.class, MethodHandles.lookup());

            // Сканируем все поля сущности Clutch
            for (var field : Clutch.class.getDeclaredFields()) {
                // Нам нужны только наши пуловые колонки и extra_data
                if (isPoolColumn(field.getName())) {
                    POOL_COLUMN_CACHE.put(field.getName(), lookup.unreflectVarHandle(field));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize VarHandles for Clutch", e);
        }
    }

    private boolean isPoolColumn(String name) {
        return name.matches("^[dstlbix]{1,3}_\\d+$") || name.equals("extraData");
    }

    public VarHandle getHandle(String targetColumn) {
        VarHandle handle = POOL_COLUMN_CACHE.get(targetColumn);
        if (handle == null) {
            throw new IllegalArgumentException("Физическая колонка не найдена в пуле: " + targetColumn);
        }
        return handle;
    }

    public void mapToEntity(Map<UUID, Object> payload, Clutch clutch, Map<UUID, String> definition) {

        payload.forEach((key, value) -> {

            if (definition.keySet().contains(key)) {
                String targetColumn = definition.get(key);
                VarHandle handle = POOL_COLUMN_CACHE.get(targetColumn);

                try {
                    // VarHandle сам попытается выполнить приведение типов (Auto-boxing)
                    // Но для BigDecimal/Long лучше добавить явный конвертер ниже
                    handle.set(clutch, convertValue(value, handle.varType()));
                } catch (Exception e) {
                    // Логируем ошибку маппинга конкретного поля
                    log.error("Не удалось сохранить значение поля: " +
                            "target column = " + targetColumn + " value = " + value.toString());
                    throw new IllegalArgumentException("Не удалось сохранить значение поля: " +
                            "target column = " + targetColumn + " value = " + value);
                }
            } else {
                // Если маппинга нет — в JSONB
                FormColumn column = metadataService.getColumn(key);
                clutch.getExtraData().put(column.getUserKey(), value);
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

        // Базовая конвертация (расширить под свои нужды)
        if (targetType == OffsetDateTime.class) {
            return java.time.OffsetDateTime.parse(value.toString());
        }
        if (targetType == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(value.toString());
        }
        if (targetType == java.util.UUID.class) {
            return java.util.UUID.fromString(value.toString());
        }
        if (targetType == Long.class) {
            return Long.valueOf(value.toString());
        }

        return value;
    }

    /**
     * Превращает сущность Clutch в понятный пользователю Map на основе метаданных
     */
    public Map<String, Object> mapFromEntity(Clutch clutch, Map<UUID, String> definition) {
//        Map<Long, String> idToColumnName = metadataService.getIdToUserColumnNameMapping(clutch.getFormId());
        Map<String, UUID> targetColumnToId = metadataService.getTargetColumnToIdMapping(clutch.getFormUuid());

        // Создаем карту для ответа.
        // Начальный размер = размер маппинга + данные из extra data
        Map<String, Object> result = new HashMap<>(clutch.getExtraData());

        // Проходим по определению формы (title -> s_1)
        definition.forEach((columnId, targetColumn) -> {
            VarHandle handle = POOL_COLUMN_CACHE.get(targetColumn);
            if (handle != null) {
                // Читаем значение из поля (d_1, s_1 и т.д.) через VarHandle
                Object value = handle.get(clutch);
                if (value != null) {
                    result.put(targetColumnToId.get(targetColumn).toString(), value);
                }
            }
        });

        // Добавляем системные поля, которые всегда нужны фронту
        result.put("rowUuid", clutch.getUuid());
        result.put("createdAt", clutch.getCreatedAt());
        result.put("orderNumber", clutch.getOrderNumber());

        return result;
    }
}
