package com.example.iropsim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "robot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String model;

    @Column(name = "joint_count", nullable = false)
    @Builder.Default
    private Integer jointCount = 6;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
