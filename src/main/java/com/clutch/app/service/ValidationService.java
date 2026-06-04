package com.clutch.app.service;

import com.clutch.app.entity.Clutch;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final VarHandleMappingService mappingService;

    public void validate(Clutch clutch, List<ValidationRule> rules) throws ValidationException {
        for (ValidationRule rule : rules) {
            VarHandle handle = mappingService.getHandle(rule.targetColumn());
            Object value = handle.get(clutch);

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
