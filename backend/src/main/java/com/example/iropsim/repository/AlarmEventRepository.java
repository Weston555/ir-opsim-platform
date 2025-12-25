package com.example.iropsim.repository;

import com.example.iropsim.entity.AlarmEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlarmEventRepository extends JpaRepository<AlarmEvent, UUID> {

    Optional<AlarmEvent> findByDedupKey(String dedupKey);

    @Query("SELECT ae FROM AlarmEvent ae WHERE " +
           "(:status IS NULL OR ae.status = :status) AND " +
           "(:severity IS NULL OR ae.severity = :severity) AND " +
           "(:robotId IS NULL OR ae.robot.id = :robotId) AND " +
           "(:from IS NULL OR ae.lastSeenTs >= :from) AND " +
           "(:to IS NULL OR ae.lastSeenTs <= :to) " +
           "ORDER BY ae.lastSeenTs DESC")
    Page<AlarmEvent> findFiltered(
            @Param("status") AlarmEvent.Status status,
            @Param("severity") AlarmEvent.Severity severity,
            @Param("robotId") UUID robotId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT ae FROM AlarmEvent ae WHERE ae.status = 'OPEN' ORDER BY ae.lastSeenTs DESC")
    List<AlarmEvent> findOpenAlarms();

    List<AlarmEvent> findByRobotIdAndStatus(UUID robotId, AlarmEvent.Status status);

    @Query("SELECT COUNT(ae) FROM AlarmEvent ae WHERE ae.robot.id = :robotId " +
           "AND ae.alarmType = :alarmType AND ae.lastSeenTs >= :from AND ae.lastSeenTs <= :to")
    long countByRobotAndAlarmTypeAndTimeRange(
            @Param("robotId") UUID robotId,
            @Param("alarmType") AlarmEvent.AlarmType alarmType,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
