package com.example.iropsim.repository;

import com.example.iropsim.entity.ScenarioRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScenarioRunRepository extends JpaRepository<ScenarioRun, UUID> {
}
