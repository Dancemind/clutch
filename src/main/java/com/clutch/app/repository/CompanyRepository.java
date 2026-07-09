package com.clutch.app.repository;

import com.clutch.app.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    @Query(value = "SELECT EXISTS(SELECT 1 FROM companies WHERE name = :name)", nativeQuery = true)
    Boolean existsByName(String name);

}
