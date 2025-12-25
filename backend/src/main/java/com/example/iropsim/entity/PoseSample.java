package com.example.iropsim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pose_sample")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant ts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;

    private Double x;
    private Double y;
    private Double z;
    private Double rx;
    private Double ry;
    private Double rz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_run_id")
    private ScenarioRun scenarioRun;

    @Enumerated(EnumType.STRING)
    private SampleLabel label;

    public enum SampleLabel {
        NORMAL,
        FAULT_OVERHEAT,
        FAULT_HIGH_VIBRATION,
        FAULT_CURRENT_SPIKE,
        FAULT_SENSOR_DRIFT
    }
}
