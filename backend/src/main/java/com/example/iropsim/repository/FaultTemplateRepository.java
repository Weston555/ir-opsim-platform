package com.example.iropsim.repository;

import com.example.iropsim.entity.FaultTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 故障模板Repository
 */
@Repository
public interface FaultTemplateRepository extends JpaRepository<FaultTemplate, UUID> {

    List<FaultTemplate> findByEnabledTrueOrderByName();

    @Query("SELECT ft FROM FaultTemplate ft WHERE ft.enabled = true AND " +
           "(:faultType IS NULL OR ft.faultType = :faultType) AND " +
           "(:severity IS NULL OR ft.severity = :severity) " +
           "ORDER BY ft.name")
    List<FaultTemplate> findFiltered(
            @Param("faultType") String faultType,
            @Param("severity") FaultTemplate.Severity severity);

    List<FaultTemplate> findByFaultTypeAndEnabledTrue(String faultType);
}

/**
 * 故障模板Repository
 */
@Repository
public interface FaultTemplateRepository extends JpaRepository<FaultTemplate, UUID> {

    List<FaultTemplate> findByEnabledTrueOrderByName();

    @Query("SELECT ft FROM FaultTemplate ft WHERE ft.enabled = true AND " +
           "(:faultType IS NULL OR ft.faultType = :faultType) AND " +
           "(:severity IS NULL OR ft.severity = :severity) " +
           "ORDER BY ft.name")
    List<FaultTemplate> findFiltered(
            @Param("faultType") String faultType,
            @Param("severity") Severity severity);

    List<FaultTemplate> findByFaultTypeAndEnabledTrue(String faultType);
}
