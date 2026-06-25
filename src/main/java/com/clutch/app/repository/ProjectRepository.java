package com.clutch.app.repository;

import com.clutch.app.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Where deleted_at is NULL for all requests, except admin ones
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> getProjectByUuidAndDeletedAtIsNull(UUID uuid);

    List<Project> findAllByDeletedAtIsNull();

    Page<Project> findAllByDeletedAtIsNull(Pageable pageable);

}
