package com.example.iropsim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scenario_run")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioRun {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RunMode mode;

    @Column(nullable = false)
    private Long seed;

    @Column(name = "rate_hz", nullable = false)
    @Builder.Default
    private Integer rateHz = 1;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RunStatus status = RunStatus.CREATED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum RunMode {
        REALTIME, REPLAY
    }

    public enum RunStatus {
        CREATED, RUNNING, STOPPED, FINISHED
    }
}
