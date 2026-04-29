CREATE TABLE feature_flag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    flag_key VARCHAR(128) NOT NULL,
    enabled BIT NOT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_feature_flag_tenant_id_flag_key
    ON feature_flag (tenant_id, flag_key);

INSERT INTO feature_flag (tenant_id, flag_key, enabled, updated_by, created_at, updated_at)
SELECT tenant.id, seeded.flag_key, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenant
         CROSS JOIN (
    SELECT 'ai.import.error-summary.enabled' AS flag_key
    UNION ALL
    SELECT 'ai.import.fix-recommendation.enabled'
    UNION ALL
    SELECT 'ai.import.mapping-suggestion.enabled'
    UNION ALL
    SELECT 'ai.ticket.reply-draft.enabled'
    UNION ALL
    SELECT 'ai.ticket.summary.enabled'
    UNION ALL
    SELECT 'ai.ticket.triage.enabled'
    UNION ALL
    SELECT 'workflow.import.selective-replay-proposal.enabled'
    UNION ALL
    SELECT 'workflow.ticket.comment-proposal.enabled'
) seeded;
