package com.clutch.app.repository;

import com.clutch.app.entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FormMetadataRepository extends JpaRepository<Form, UUID> {
    Form getFormMetadataByUuid(UUID uuid);

    // Чтобы найти удаленную форму, нам нужен нативный запрос или специальный Filter,
    // так как @SQLRestriction отсечет её в обычном findById.
    // Обычные методы (findAll, findById) будут автоматически
    // добавлять "WHERE deleted_at IS NULL" благодаря @SQLRestriction

    @Modifying
    @Query("UPDATE Form f SET f.deletedAt = null WHERE f.uuid = :uuid AND f.companyUuid = :companyUuid")
    void restoreDeletedForm(@Param("uuid") UUID uuid, @Param("companyUuid") UUID companyUuid);

    // Метод для корзины: найти только удаленные таблицы компании
    @Query(value = "SELECT * FROM forms WHERE deleted_at IS NOT NULL AND company_uuid = :companyUuid", nativeQuery = true)
    List<Form> findTrash(@Param("companyUuid") UUID companyUuid);

    // Для системного планировщика: найти таблицы помеченные как удаленные
    @Query(value = "SELECT * FROM forms WHERE deleted_at < :threshold", nativeQuery = true)
    List<Form> findExpiredForms(@Param("threshold") OffsetDateTime threshold);

    void deleteAllByUuidIn(List<UUID> oldFormUuids);
}
