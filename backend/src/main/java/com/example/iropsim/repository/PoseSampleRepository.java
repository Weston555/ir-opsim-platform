package com.example.iropsim.repository;

import com.example.iropsim.entity.PoseSample;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PoseSampleRepository extends JpaRepository<PoseSample, Long> {

    @Query("SELECT ps FROM PoseSample ps WHERE ps.robot.id = :robotId " +
           "AND ps.ts >= :from AND ps.ts <= :to ORDER BY ps.ts DESC")
    List<PoseSample> findByRobotTimeRange(
            @Param("robotId") UUID robotId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT ps FROM PoseSample ps WHERE ps.robot.id = :robotId ORDER BY ps.ts DESC LIMIT 1")
    PoseSample findLatestByRobot(@Param("robotId") UUID robotId);
}
