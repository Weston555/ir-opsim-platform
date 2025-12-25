package com.example.iropsim.repository;

import com.example.iropsim.entity.KbRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KbRuleRepository extends JpaRepository<KbRule, UUID> {

    @Query("SELECT kr FROM KbRule kr WHERE kr.enabled = true ORDER BY kr.priority DESC")
    List<KbRule> findEnabledRulesOrderedByPriority();
}
