package com.clutch.app.service;

import com.clutch.app.entity.RowData;
import com.clutch.app.entity.ValidationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.clutch.app.exceptions.ValidationException;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ValidationRuleService validationRuleService;
    private final VarHandleMappingService mappingService;

    public void validateRowData(UUID formUuid, RowData row) {
        List<ValidationRule> rules = validationRuleService.getValidationRules(formUuid);

        for (ValidationRule rule : rules) {
            VarHandle handle = mappingService.getHandle(rule.getTargetColumn());
            Object value = handle.get(row);

            boolean isValid = switch (rule.getRuleType()) {
                case REQUIRED -> value != null;
                case MIN -> value instanceof BigDecimal d && d.compareTo(new BigDecimal(rule.getRuleValue())) >= 0;
                case REGEX -> value instanceof String s && s.matches(rule.getRuleValue());
                default -> true;
            };

            if (!isValid) {
                throw new ValidationException(rule.getErrorMessage());
            }
        }
    }

}
