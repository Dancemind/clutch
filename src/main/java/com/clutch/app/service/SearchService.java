package com.clutch.app.service;

import com.clutch.app.dto.SearchCriteria;
import com.clutch.app.entity.Clutch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final EntityManager entityManager;
    private final MetadataService metadataService;

    public List<Clutch> search(UUID formUuid, List<SearchCriteria> criteriaList) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Clutch> query = cb.createQuery(Clutch.class);
        Root<Clutch> root = query.from(Clutch.class);

        // 1. Получаем маппинг для формы (из кэша Redis через MetadataService)
        Map<UUID, String> definition = metadataService.getIdToTargetColumnMapping(formUuid);

        List<Predicate> predicates = new ArrayList<>();

        // Мы НЕ добавляем company_id вручную
        // Hibernate @TenantId сам допишет его к этому Criteria запросу.
        predicates.add(cb.equal(root.get("formUuid"), formUuid));

        // 2. Строим предикаты на основе метаданных
        for (SearchCriteria criteria : criteriaList) {
            String physicalColumn = definition.get(criteria.fieldName());
            if (physicalColumn != null) {
                predicates.add(buildPredicate(cb, root, physicalColumn, criteria));
            }
        }

        query.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(query).getResultList();
    }

    private Predicate buildPredicate(CriteriaBuilder cb, Root<Clutch> root, String col, SearchCriteria sc) {
        return switch (sc.operator()) {
            case "EQUALS" -> cb.equal(root.get(col), sc.value());
            case "GREATER_THAN" -> cb.greaterThan(root.get(col), (Comparable) sc.value());
            case "LIKE" -> cb.like(root.get(col), "%" + sc.value() + "%");
            default -> cb.equal(root.get(col), sc.value());
        };
    }
}
