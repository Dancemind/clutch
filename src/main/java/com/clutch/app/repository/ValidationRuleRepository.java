package com.clutch.app.repository;

import com.clutch.app.entity.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, UUID> {
    List<ValidationRule> findAllByFormUuid(UUID formUuid);
}
