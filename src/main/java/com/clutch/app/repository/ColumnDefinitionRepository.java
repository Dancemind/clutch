package com.clutch.app.repository;

import com.clutch.app.entity.FormColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ColumnDefinitionRepository extends JpaRepository<FormColumn, UUID> {
    List<FormColumn> findAllByFormUuid(UUID formUuid);

    List<FormColumn> getByFormUuid(UUID formUuid);
}
