package com.example.iropsim.repository;

import com.example.iropsim.entity.Robot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RobotRepository extends JpaRepository<Robot, UUID> {

    @Query("SELECT r FROM Robot r WHERE LOWER(r.model) LIKE LOWER(CONCAT('%', :model, '%'))")
    Page<Robot> findByModelContainingIgnoreCase(@Param("model") String model, Pageable pageable);
}
