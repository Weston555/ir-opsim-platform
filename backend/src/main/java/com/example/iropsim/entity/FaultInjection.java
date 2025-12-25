package com.example.iropsim.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Convert;
import com.example.iropsim.config.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fault_injection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultInjection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_run_id", nullable = false)
    private ScenarioRun scenarioRun;

    @Column(name = "fault_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FaultType faultType;

    @Column(name = "start_ts", nullable = false)
    private Instant startTs;

    @Column(name = "end_ts", nullable = false)
    private Instant endTs;

    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode params;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum FaultType {
        OVERHEAT,
        HIGH_VIBRATION,
        CURRENT_SPIKE,
        SENSOR_DRIFT
    }
}
