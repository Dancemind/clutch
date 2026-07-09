package com.clutch.app.entity;

import com.clutch.app.enums.FieldType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormColumn extends CompanyBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_uuid", nullable = false)
    private Form form;

    @Column(name = "user_key", nullable = false, length = 64)
    private String userKey;         // for ex. "price" as field name for user

    @Column(name = "target_column", nullable = false, length = 10)
    private String targetColumn;    // for ex. "d_1", depends on field type

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type")
    private FieldType fieldType;    // for ex. "MONEY", "TEXT", "LINK"

    @Column(name = "order_number")
    private Integer orderNumber;    // of the field in Form/Table

}
