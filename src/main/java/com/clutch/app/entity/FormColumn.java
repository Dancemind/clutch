package com.clutch.app.entity;

import com.clutch.app.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "form_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormColumn extends BaseEntity {
    @Column(name = "form_uuid", nullable = false)
    private UUID formUuid;

    @Column(name = "user_key", nullable = false, length = 64)
    private String userKey;      // например: "price"

    @Column(name = "target_column", nullable = false, length = 10)
    private String targetColumn; // например: "d_1"

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type")
    private FieldType fieldType;    // например: "MONEY", "TEXT", "LINK"

    @Column(name = "order_number")
    private Integer orderNumber;   // порядок отображения на фронте - порядковый номер колонки в таблице
}
