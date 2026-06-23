package com.clutch.app.controller;

import com.clutch.app.dto.RowDto;
import com.clutch.app.dto.response.form.FormDto;
import com.clutch.app.entity.Clutch;
import com.clutch.app.entity.audit.AuditLog;
import com.clutch.app.repository.AuditLogRepository;
import com.clutch.app.repository.ClutchRepository;
import com.clutch.app.service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clutch")
@RequiredArgsConstructor
public class ClutchController {

    private final ClutchService clutchService;
    private final ClutchRepository clutchRepository;
    private final AuditLogRepository auditLogRepository;

    // Создание записи в динамическом формате
    @PostMapping("/form/{formUuid}")
    public List<RowDto> create(@PathVariable UUID formUuid, @RequestBody List<RowDto> rows)
            throws ValidationException {
        return clutchService.createRows(formUuid, rows);
    }

    // full Form data & column mappings for Form by id
    @GetMapping("/form/{formUuid}")
    public FormDto getAll(@PathVariable UUID formUuid) {
        return clutchService.getForm(formUuid);
    }

    @PatchMapping("/{rowUuid}")
    public Map<String, Object> update(@PathVariable UUID rowUuid, @RequestBody Map<UUID, Object> payload)
            throws ValidationException {
        return clutchService.updateRow(rowUuid, payload);
    }

    @GetMapping("/{rowUuid}/history")
    public Page<Map<String, Object>> getHistory(@PathVariable UUID rowUuid,
                                                @RequestParam(defaultValue = "0") int page) {

        // 1. Сначала находим системный ID записи по её публичному UUID
        Clutch record = clutchRepository.findByUuid(rowUuid)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена"));

        // 2. Достаем историю с пагинацией
        Pageable pageable = PageRequest.of(page, 20);
        Page<AuditLog> history = auditLogRepository.findByClutchUuidOrderByCreatedAtDesc(record.getUuid(), pageable);

        // 3. Маппим результат (можно оставить как есть, так как в JSONB уже лежат понятные ключи)
        // todo: красивый маппинг в дто
        return history.map(log -> Map.of(
                "action", log.getAction(),
                "changedBy", log.getChangedBy(),
                "at", log.getCreatedAt(),
                "diff", Map.of("old", log.getOldValues(), "new", log.getNewValues())
        ));
    }


//    @PostMapping("/form/{formId}/search")
//    public List<Map<String, Object>> search(@PathVariable Long formId, @RequestBody List<SearchCriteria> criteria) {
//        // 1. Выполняем поиск через наш динамический сервис
//        List<Clutch> results = searchService.search(formId, criteria);
//
//        // 2. Получаем маппинг, чтобы превратить d_1/s_1 обратно в понятные имена
//        List<FormColumn> formColumns = metadataService.getFormColumns(formId);
//
//        // 3. Возвращаем клиенту бизнес-JSON
//        return results.stream()
//                .map(record -> mappingService.mapFromEntity(record, formColumns))
//                .toList();
//    }

//    @PostMapping("/dynamic/{formId}")
//    public Clutch createDynamic(@PathVariable Long formId, @RequestBody Map<String, Object> payload) {
//        // В реальности достаем из БД: Map<String, String> definition = metadataService.get(formId);
//        Map<String, String> mockDefinition = Map.of("price", "d_1", "title", "s_1");
//
//        Clutch clutch = Clutch.builder()
//                .formId(formId)
//                .extraData(new HashMap<>())
//                .build();
//
//        varHandleMappingService.mapToEntity(payload, clutch, mockDefinition);
//        return clutchService.createRecord(clutch);
//    }
//
//    @GetMapping("/dynamic/{formId}")
//    public List<Map<String, Object>> getByFormDynamic(@PathVariable Long formId) {
//        // В будущем: Map<String, String> definition = metadataService.get(formId);
//        Map<String, String> mockDefinition = Map.of("price", "d_1", "title", "s_1");
//
//        List<Clutch> records = clutchService.getRecordsByForm(formId);
//
//        // Стримуем и маппим каждую запись
//        return records.stream()
//                .map(record -> varHandleMappingService.mapFromEntity(record, mockDefinition))
//                .toList();
//    }
}
