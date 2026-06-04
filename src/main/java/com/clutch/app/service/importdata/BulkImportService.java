package com.clutch.app.service.importdata;

import com.clutch.app.config.TenantContext;
import com.clutch.app.entity.Clutch;
import com.clutch.app.service.MetadataService;
import com.clutch.app.service.ValidationRule;
import com.clutch.app.service.ValidationService;
import com.clutch.app.service.VarHandleMappingService;
import com.clutch.app.service.notification.NotificationService;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkImportService {

    private final JdbcTemplate jdbcTemplate;
    private final MetadataService metadataService;
    private final ValidationService validationService;
    private final VarHandleMappingService mappingService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;

//    @Transactional
//    public void importData(UUID formUuid, List<Map<UUID, Object>> rows) throws ValidationException {
//        UUID companyUuid = TenantContext.get();
//        Map<UUID, String> definition = metadataService.getIdToTargetColumnMapping(formUuid);
//        List<ValidationRule> rules = metadataService.getValidationRules(formUuid);
//
//        // 1. ПРЕ-ВАЛИДАЦИЯ (Fail-Fast)
//        // Проходим по всем строкам в памяти, чтобы не мусорить в БД, если есть ошибка
//        for (Map<UUID, Object> row : rows) {
//            Clutch tempClutch = new Clutch();
//            tempClutch.setFormUuid(formUuid);
//            mappingService.mapToEntity(row, tempClutch, definition);
//            validationService.validate(tempClutch, rules); // Наш VarHandle-валидатор в деле
//        }
//
//        // 2. ГЕНЕРАЦИЯ ДИНАМИЧЕСКОГО SQL
//        // Собираем список колонок, которые реально есть в маппинге формы
//        List<String> targetColumns = new ArrayList<>(definition.values());
//
//        String columnsSql = String.join(", ", targetColumns);
//        String placeholders = targetColumns.stream().map(c -> "?").collect(Collectors.joining(", "));
//
//        String sql = """
//                INSERT INTO clutch (uuid, company_uuid, form_uuid, order_number, version, created_at, %s)
//                VALUES (?, ?, ?, ?, 0, now(), %s)
//                """.formatted(columnsSql, placeholders);
//
//        AtomicInteger j = new AtomicInteger(0);
//
//        // 3. BATCH EXECUTION
//        jdbcTemplate.batchUpdate(sql, rows, 500, (ps, row) -> {
//            ps.setObject(1, UUID.randomUUID());
//            ps.setObject(2, companyUuid);
//            ps.setObject(3, formUuid);
//            ps.setObject(4, j.incrementAndGet());
//
//            // Заполняем динамические колонки
//            for (int i = 0; i < targetColumns.size(); i++) {
//                String colName = targetColumns.get(i);
//                UUID userKey = findUserKeyByColumn(definition, colName);
//                Object value = row.get(userKey);
//
//                // Используем наш конвертер из VarHandleMappingService для приведения типов
//                var handle = mappingService.getHandle(colName);
//                ps.setObject(i + 4, mappingService.convertValue(value, handle.varType()));
//            }
//        });
//    }

    /**
     * Импорт данных
     *
     * @param formUuid айди формы/таблицы
     * @param rows     список строк, в map приходят "Название колонки для пользователя" -> "Значение"
     *                 Например, для одной строки Map может быть таким:
     *                 "Название"    -> "Сало"
     *                 "Количество"  -> 150
     *                 "Цена"        -> 13
     * @throws ValidationException при проверке данных колонки по соответствующим правилам
     */
    @Transactional
    public void importData(UUID formUuid, List<Map<String, Object>> rows) throws ValidationException {
        UUID companyUuid = TenantContext.get();
        Map<UUID, String> definition = metadataService.getIdToTargetColumnMapping(formUuid);
        Map<String, UUID> userKeyToColumnId = metadataService.getUserKeyToColumnIdMapping(formUuid);

        // проверка, что для всех колонок есть маппинг
        rows.forEach(row ->
                row.keySet().forEach(columnName -> {
                    if (Objects.isNull(userKeyToColumnId.get(columnName))) {
                        throw new RuntimeException("Unknown column: " + columnName);
                    }
                })
        );


        // todo: переделать - заменить в том же листе, чтобы не удваивать объем данных в памяти
        List<Map<UUID, Object>> newRows = new ArrayList<>();
        rows.forEach(row -> {
            Map<UUID, Object> newRow = row.entrySet().stream()
                    .collect(
                            Collectors.toMap(
                                    entry -> userKeyToColumnId.get(entry.getKey()), // Заменяем пользовательское название колонки на её айди
                                    Map.Entry::getValue
                            ));
            newRows.add(newRow);
        });

        List<ValidationRule> rules = metadataService.getValidationRules(formUuid);

        // 1. ПРЕ-ВАЛИДАЦИЯ (Fail-Fast)
        // Проходим по всем строкам в памяти, чтобы не мусорить в БД, если есть ошибка
        for (Map<UUID, Object> row : newRows) {
            Clutch tempClutch = new Clutch();
            tempClutch.setFormUuid(formUuid);

            mappingService.mapToEntity(row, tempClutch, definition);
            validationService.validate(tempClutch, rules); // Наш VarHandle-валидатор в деле
        }

        // 2. ГЕНЕРАЦИЯ ДИНАМИЧЕСКОГО SQL
        // Собираем список колонок, которые реально есть в маппинге формы
        List<String> targetColumns = new ArrayList<>(definition.values());

        String columnsSql = String.join(", ", targetColumns);
        String placeholders = targetColumns.stream().map(c -> "?").collect(Collectors.joining(", "));

        String sql = """
                INSERT INTO clutch (uuid, company_uuid, form_uuid, order_number, version, created_at, %s)
                VALUES (?, ?, ?, ?, ?, ?, %s)
                """.formatted(columnsSql, placeholders);

        AtomicInteger j = new AtomicInteger(0);

        // 3. BATCH EXECUTION
        jdbcTemplate.batchUpdate(sql, newRows, 500, (ps, row) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, companyUuid);
            ps.setObject(3, formUuid);
            ps.setObject(4, j.incrementAndGet());
            ps.setInt(5, 0);
            ps.setObject(6, OffsetDateTime.now());

            // Заполняем динамические колонки
            for (int i = 0; i < targetColumns.size(); i++) {
                String colName = targetColumns.get(i);
                UUID userKey = findUserKeyByColumn(definition, colName);
                Object value = row.get(userKey);

                // Используем конвертер из VarHandleMappingService для приведения типов
                var handle = mappingService.getHandle(colName);
                ps.setObject(i + 7, mappingService.convertValue(value, handle.varType()));
            }
        });
    }

    /**
     * Архитектурная схема с SSE и виртуальными потоками
     * Frontend подписывается на эндпоинт GET /api/v1/import/{id}/events.
     * Backend открывает соединение и держит его открытым в виртуальном потоке
     * (Loom позволяет держать миллионы таких соединений почти бесплатно).
     *
     * @param formId
     * @param importId
     * @param rows
     */
    @Transactional
    public void importData(Long formId, String importId, List<Map<String, Object>> rows) {
        String redisKey = "import:status:" + importId;
        int total = rows.size();

        // Инициализируем прогресс в Redis (живет 1 час)
        redisTemplate.opsForHash().putAll(redisKey, Map.of("total", total, "processed", 0, "status", "PROCESSING"));
        redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);

        // Разбиваем на батчи и обновляем прогресс
        int batchSize = 500;
        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Map<String, Object>> batch = rows.subList(i, end);

//            executeBatch(formId, batch); // динамический JDBC SQL

            // Обновляем счетчик в Redis
            redisTemplate.opsForHash().increment(redisKey, "processed", batch.size());

            // Отправляем прогресс на фронтенд
            notificationService.sendProgress(importId, i + batchSize, total);
        }

        redisTemplate.opsForHash().put(redisKey, "status", "COMPLETED");

        // Финальный ивент
        notificationService.sendComplete(importId);
    }

    private UUID findUserKeyByColumn(Map<UUID, String> definition, String col) {
        return definition.entrySet().stream()
                .filter(e -> e.getValue().equals(col))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

}
