package com.clutch.app.repository;

import com.clutch.app.entity.RowData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RowDataRepository extends JpaRepository<RowData, UUID> {

    List<RowData> findByFormUuid(UUID formUuid);

    Optional<RowData> findByUuid(UUID uuid);

    void deleteAllByFormUuidIn(List<UUID> oldFormUuids);

}
