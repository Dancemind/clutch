package com.clutch.app.repository;

import com.clutch.app.entity.Clutch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClutchRepository extends JpaRepository<Clutch, UUID> {

    List<Clutch> findByFormUuid(UUID formUuid);

    Optional<Clutch> findByUuid(UUID uuid);

    void deleteAllByFormUuidIn(List<UUID> oldFormUuids);

}
