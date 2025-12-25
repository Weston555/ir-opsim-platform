package com.example.iropsim.repository;

import com.example.iropsim.entity.AlarmAck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlarmAckRepository extends JpaRepository<AlarmAck, UUID> {
}
