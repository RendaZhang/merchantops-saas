package com.renda.merchantops.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "import_job_item_error")
public class ImportJobItemErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "source_row_number")
    private Integer rowNumber;

    @Column(name = "error_code", nullable = false, length = 64)
    private String errorCode;

    @Column(name = "error_message", nullable = false, length = 512)
    private String errorMessage;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
