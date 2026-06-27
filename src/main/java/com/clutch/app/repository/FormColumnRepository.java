package com.clutch.app.repository;

import com.clutch.app.entity.FormColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormColumnRepository extends JpaRepository<FormColumn, UUID> {
    List<FormColumn> findAllByFormUuid(UUID formUuid);


    @Query("SELECT fc FROM FormColumn fc JOIN FETCH fc.form WHERE fc.uuid = :columnUuid")
    Optional<FormColumn> findWithFormByUuid(@Param("columnUuid") UUID columnUuid);
}
