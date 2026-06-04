package com.clutch.app.entity;

import com.clutch.app.enums.RuleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "validation_rules", indexes = {
        @Index(name = "idx_val_rules_form", columnList = "form_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ValidationRule extends BaseEntity {
    @Column(name = "form_uuid", nullable = false)
    private UUID formUuid;

    @Column(name = "target_column", nullable = false, length = 10)
    private String targetColumn; // Ссылка на физическую колонку, например "d_1"

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(name = "rule_value")
    private String ruleValue;    // Значение для проверки: "0", "^[0-9]+$"

    @Column(name = "message")
    private String message;      // Текст ошибки для пользователя
}
