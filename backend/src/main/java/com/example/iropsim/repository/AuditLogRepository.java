package com.example.iropsim.repository;

import com.example.iropsim.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:actorUserId IS NULL OR al.actorUser.id = :actorUserId) AND " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:from IS NULL OR al.ts >= :from) AND " +
           "(:to IS NULL OR al.ts <= :to) " +
           "ORDER BY al.ts DESC")
    Page<AuditLog> findFiltered(
            @Param("actorUserId") String actorUserId,
            @Param("action") String action,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
