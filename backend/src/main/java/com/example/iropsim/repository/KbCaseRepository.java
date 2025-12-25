package com.example.iropsim.repository;

import com.example.iropsim.entity.KbCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KbCaseRepository extends JpaRepository<KbCase, UUID> {

    @Query("SELECT kc FROM KbCase kc WHERE " +
           "(:keyword IS NULL OR LOWER(kc.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(kc.rootCause) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:faultType IS NULL OR kc.faultType = :faultType)")
    Page<KbCase> search(
            @Param("keyword") String keyword,
            @Param("faultType") String faultType,
            Pageable pageable);

    List<KbCase> findByFaultType(String faultType);
}
