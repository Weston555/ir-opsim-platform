package com.example.iropsim.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alarm_event", uniqueConstraints = @UniqueConstraint(columnNames = "dedup_key"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "first_seen_ts", nullable = false)
    private Instant firstSeenTs;

    @Column(name = "last_seen_ts", nullable = false)
    private Instant lastSeenTs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;

    @Column(name = "joint_index")
    private Integer jointIndex;

    @Column(name = "alarm_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    @Column(nullable = false)
    @Builder.Default
    private Integer count = 1;

    @Column(nullable = false)
    private String detector;

    @Column(nullable = false)
    private Double score;

    @Column(columnDefinition = "jsonb")
    private JsonNode evidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_run_id")
    private ScenarioRun scenarioRun;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum AlarmType {
        TEMP_ANOMALY,
        VIB_ANOMALY,
        CURRENT_ANOMALY,
        POSE_ANOMALY
    }

    public enum Severity {
        INFO, WARN, CRITICAL
    }

    public enum Status {
        OPEN, ACKED, CLOSED
    }
}
