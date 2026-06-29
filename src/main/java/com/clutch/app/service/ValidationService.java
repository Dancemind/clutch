package com.clutch.app.service;

import com.clutch.app.dto.ValidationRuleDto;
import com.clutch.app.entity.RowData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.clutch.app.exceptions.ValidationException;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final VarHandleMappingService mappingService;

    public void validate(RowData row, List<ValidationRuleDto> rules) {
        for (ValidationRuleDto rule : rules) {
            VarHandle handle = mappingService.getHandle(rule.targetColumn());
            Object value = handle.get(row);

            boolean isValid = switch (rule.type()) {
                case REQUIRED -> value != null;
                case MIN -> value instanceof BigDecimal d && d.compareTo(new BigDecimal(rule.value())) >= 0;
                case REGEX -> value instanceof String s && s.matches(rule.value());
                default -> true;
            };

            if (!isValid) {
                throw new ValidationException(rule.errorMessage());
            }
        }
    }
}
