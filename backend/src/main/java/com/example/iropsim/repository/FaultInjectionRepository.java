package com.example.iropsim.repository;

import com.example.iropsim.entity.FaultInjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FaultInjectionRepository extends JpaRepository<FaultInjection, UUID> {

    @Query("SELECT fi FROM FaultInjection fi WHERE fi.scenarioRun.id = :scenarioRunId " +
           "AND fi.startTs <= :timestamp AND fi.endTs >= :timestamp")
    List<FaultInjection> findByScenarioRunIdAndTimeRange(
            @Param("scenarioRunId") UUID scenarioRunId,
            @Param("timestamp") Instant timestamp);

    List<FaultInjection> findByScenarioRunId(@Param("scenarioRunId") UUID scenarioRunId);
}
