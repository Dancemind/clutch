package com.clutch.app.repository;

import com.clutch.app.entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormRepository extends JpaRepository<Form, UUID> {

    Optional<Form> findByUuid(UUID uuid);

    @Query(value = "SELECT DISTINCT f FROM Form f LEFT JOIN FETCH f.columns WHERE f.uuid = :uuid", nativeQuery = true)
    Optional<Form> findWithColumnsByUuid(@Param("uuid") UUID uuid);

    @Query(value = "SELECT DISTINCT f FROM Form f LEFT JOIN FETCH f.columns", nativeQuery = true)
    List<Form> findAllWithColumns();

    List<Form> deleteAllByUuidIn(List<UUID> formUuids);

    @Modifying
    @Query("UPDATE Form f SET f.deletedAt = null WHERE f.uuid = :uuid AND f.companyUuid = :companyUuid")
    void restoreDeletedForm(@Param("uuid") UUID uuid, @Param("companyUuid") UUID companyUuid);

    @Query(value = "SELECT * FROM forms WHERE deleted_at IS NOT NULL AND company_uuid = :companyUuid", nativeQuery = true)
    List<Form> findDeletedFormsByCompany(@Param("companyUuid") UUID companyUuid);

    List<Form> findAllByIsActiveFalse();

    // find expired forms to remove them from database
    @Query(value = "SELECT * FROM forms WHERE deleted_at < :threshold", nativeQuery = true)
    List<Form> findExpiredForms(@Param("threshold") OffsetDateTime threshold);

}
