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
@Table(name = "ticket_operation_log")
public class TicketOperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "operation_type", nullable = false, length = 64)
    private String operationType;

    @Column(name = "detail", nullable = false, length = 512)
    private String detail;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
