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
@Table(name = "ticket_comment")
public class TicketCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
