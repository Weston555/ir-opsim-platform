package com.example.iropsim.repository;

import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.ScenarioRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JointSampleRepository extends JpaRepository<JointSample, Long> {

    @Query("SELECT js FROM JointSample js WHERE js.robot.id = :robotId AND js.jointIndex = :jointIndex " +
           "AND js.ts >= :from AND js.ts <= :to ORDER BY js.ts DESC")
    List<JointSample> findByRobotAndJointTimeRange(
            @Param("robotId") UUID robotId,
            @Param("jointIndex") Integer jointIndex,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT js FROM JointSample js WHERE js.robot.id = :robotId " +
           "AND js.ts >= :from AND js.ts <= :to ORDER BY js.ts DESC")
    List<JointSample> findByRobotTimeRange(
            @Param("robotId") UUID robotId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT js FROM JointSample js WHERE js.robot.id = :robotId ORDER BY js.ts DESC LIMIT 1")
    JointSample findLatestByRobot(@Param("robotId") UUID robotId);

    List<JointSample> findByScenarioRunOrderByTs(ScenarioRun scenarioRun);

    @Query("SELECT js FROM JointSample js WHERE js.robot.id = :robotId AND js.jointIndex = :jointIndex " +
           "ORDER BY js.ts DESC LIMIT :limit")
    List<JointSample> findTopByRobotIdAndJointIndexOrderByTsDesc(
            @Param("robotId") UUID robotId,
            @Param("jointIndex") Integer jointIndex,
            @Param("limit") Integer limit);

    @Query("SELECT js FROM JointSample js WHERE js.robot.id = :robotId " +
           "AND js.ts >= :from AND js.ts <= :to ORDER BY js.ts DESC")
    List<JointSample> findByRobotIdAndTsBetweenOrderByTsDesc(
            @Param("robotId") UUID robotId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
