package com.renda.merchantops.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ai_interaction_record")
public class AiInteractionRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "interaction_type", nullable = false, length = 64)
    private String interactionType;

    @Column(name = "prompt_version", nullable = false, length = 128)
    private String promptVersion;

    @Column(name = "model_id", length = 128)
    private String modelId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "output_summary")
    private String outputSummary;

    @Column(name = "usage_prompt_tokens")
    private Integer usagePromptTokens;

    @Column(name = "usage_completion_tokens")
    private Integer usageCompletionTokens;

    @Column(name = "usage_total_tokens")
    private Integer usageTotalTokens;

    @Column(name = "usage_cost_micros")
    private Long usageCostMicros;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
