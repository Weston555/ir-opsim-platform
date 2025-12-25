package com.example.iropsim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alarm_ack")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmAck {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_id", nullable = false)
    private AlarmEvent alarm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ack_by", nullable = false)
    private User ackBy;

    @Column(name = "ack_ts", nullable = false)
    private Instant ackTs;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
