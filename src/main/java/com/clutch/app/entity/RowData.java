package com.clutch.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Changing fields of the entity requires appropriate changes in
 *      com.clutch.app.service.VarHandleMappingService#isPoolColumn(java.lang.String)
 */
@Entity
@Table(name = "clutch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RowData extends BaseEntity {

    @Column(name = "form_uuid", nullable = false)
    private UUID formUuid;

    // row number in user table
    private Long orderNumber;

    // text - single line text / notes
    private String txt_1;
    private String txt_2;
    private String txt_3;
    private String txt_4;
    private String txt_5;
    private String txt_6;
    private String txt_7;
    private String txt_8;
    private String txt_9;
    private String txt_10;
    private String txt_11;
    private String txt_12;
    private String txt_13;
    private String txt_14;
    private String txt_15;

    // numbers - currency
    @Column(precision = 19, scale = 4)
    private BigDecimal d_1;
    @Column(precision = 19, scale = 4)
    private BigDecimal d_2;
    @Column(precision = 19, scale = 4)
    private BigDecimal d_3;
    @Column(precision = 19, scale = 4)
    private BigDecimal d_4;

    // numbers - percent
    private Double n_1;
    private Double n_2;
    private Double n_3;
    private Double n_4;

    // business id and status - phone / email / codes
    @Column(length = 64)
    private String id_1;
    @Column(length = 64)
    private String id_2;
    @Column(length = 64)
    private String id_3;
    @Column(length = 64)
    private String id_4;
    @Column(length = 64)
    private String id_5;

    // timestamp - dates
    private OffsetDateTime t_1;
    private OffsetDateTime t_2;
    private OffsetDateTime t_3;
    private OffsetDateTime t_4;
    private OffsetDateTime t_5;

    // links - link to another record, user, attachment ID
    private UUID l_1;
    private UUID l_2;
    private UUID l_3;
    private UUID l_4;
    private UUID l_5;

    // flags - checkbox
    private Boolean b_1;
    private Boolean b_2;
    private Boolean b_3;
    private Boolean b_4;
    private Boolean b_5;

    // extra column - for all other columns
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data")
    @Builder.Default
    private Map<UUID, Object> extraData = new HashMap<>();

}
