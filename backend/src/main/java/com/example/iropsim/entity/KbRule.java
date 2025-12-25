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
@Table(name = "kb_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "when_expr", columnDefinition = "jsonb", nullable = false)
    private JsonNode whenExpr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "then_case_id", nullable = false)
    private KbCase thenCase;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
