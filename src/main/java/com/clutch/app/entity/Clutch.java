package com.clutch.app.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "clutch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clutch extends BaseEntity {

    // к какой форме/таблице относится запись
    @Column(name = "form_uuid", nullable = false)
    private UUID formUuid;
    // порядковый номер строки в форме/таблице
    @Column(name = "order_number")
    private Long orderNumber;

    // пул колонок
    // разнести бы деньги и числа - разные констрейнты
    // --- ГРУППА 1: MONEY / DECIMAL (10 полей) --- Деньги/Числа
    private BigDecimal d_1, d_2, d_3, d_4, d_5, d_6, d_7, d_8, d_9, d_10;
    // --- ГРУППА 2: DATES / TIMESTAMP (10 полей) --- Даты
    private OffsetDateTime t_1, t_2, t_3, t_4, t_5, t_6, t_7, t_8, t_9, t_10;
    // --- ГРУППА 3: SYSTEM LINKS (UUID) (8 полей) --- Ссылки
    // эти значения будут ссылаться на row_uuid других записей
    private UUID l_1, l_2, l_3, l_4, l_5, l_6, l_7, l_8;
    // --- ГРУППА 4: BUSINESS IDs (VARCHAR 128) (7 полей) --- (ИНН и т.д.)
    @Column(length = 128)
    private String id_1, id_2, id_3, id_4, id_5, id_6, id_7;
    // --- ГРУППА 5: SHORT TEXT (VARCHAR 255) (10 полей) --- Текст
    @Column(length = 255)
    private String s_1, s_2, s_3, s_4, s_5, s_6, s_7, s_8, s_9, s_10;
    // --- ГРУППА 6: LONG TEXT (TEXT) (5 полей) --- Текст
    @Column(columnDefinition = "TEXT")
    private String txt_1, txt_2, txt_3, txt_4, txt_5;
    // --- ГРУППА 7: BOOLEAN FLAGS (10 полей) --- Флаги
    private Boolean b_1, b_2, b_3, b_4, b_5, b_6, b_7, b_8, b_9, b_10;
    // --- ГИБРИДНЫЙ СЛОЙ: JSONB OVERFLOW --- Все остальное (гибкость)
    @Type(JsonBinaryType.class)
    @Column(name = "extra_data", columnDefinition = "jsonb")
    private Map<String, Object> extraData = new HashMap<>();

}
