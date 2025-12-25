package com.example.iropsim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "joint_sample")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JointSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant ts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;

    @Column(name = "joint_index", nullable = false)
    private Integer jointIndex;

    @Column(name = "current_a")
    private Double currentA;

    @Column(name = "vibration_rms")
    private Double vibrationRms;

    @Column(name = "temperature_c")
    private Double temperatureC;

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
