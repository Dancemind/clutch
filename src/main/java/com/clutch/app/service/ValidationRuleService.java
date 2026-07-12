package com.clutch.app.service;

import com.clutch.app.entity.ValidationRule;
import com.clutch.app.repository.ValidationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationRuleService extends BaseService<ValidationRule, UUID> {

    private final ValidationRuleRepository validationRuleRepository;

    @Override
    protected JpaRepository<ValidationRule, UUID> getRepository() {
        return validationRuleRepository;
    }

    @Override
    protected String getEntityName() {
        return ValidationRule.class.getSimpleName();
    }

    @Cacheable(value = "formRules", key = "#formUuid")
    public List<ValidationRule> getValidationRules(UUID formUuid) {
        return validationRuleRepository.findAllByFormUuid(formUuid);
    }

}
