ALTER TABLE approval_request
    ADD COLUMN pending_request_key VARCHAR(191) NULL;

UPDATE approval_request
SET pending_request_key = NULL;

UPDATE approval_request
SET status = 'REJECTED',
    pending_request_key = NULL,
    reviewed_by = NULL,
    reviewed_at = COALESCE(reviewed_at, created_at, CURRENT_TIMESTAMP),
    executed_at = NULL
WHERE id IN (
    SELECT duplicate_ids.id
    FROM (
        SELECT older.id AS id
        FROM approval_request older
        JOIN approval_request newer
          ON newer.action_type = 'USER_STATUS_DISABLE'
         AND newer.entity_type = 'USER'
         AND newer.status = 'PENDING'
         AND newer.tenant_id = older.tenant_id
         AND newer.entity_id = older.entity_id
         AND (
             newer.created_at > older.created_at
             OR (newer.created_at = older.created_at AND newer.id > older.id)
         )
        WHERE older.action_type = 'USER_STATUS_DISABLE'
          AND older.entity_type = 'USER'
          AND older.status = 'PENDING'
    ) duplicate_ids
);

UPDATE approval_request
SET pending_request_key = CONCAT('USER_STATUS_DISABLE:', tenant_id, ':', entity_id)
WHERE id IN (
    SELECT canonical_ids.id
    FROM (
        SELECT current_row.id AS id
        FROM approval_request current_row
        LEFT JOIN approval_request newer
          ON newer.action_type = 'USER_STATUS_DISABLE'
         AND newer.entity_type = 'USER'
         AND newer.status = 'PENDING'
         AND newer.tenant_id = current_row.tenant_id
         AND newer.entity_id = current_row.entity_id
         AND (
             newer.created_at > current_row.created_at
             OR (newer.created_at = current_row.created_at AND newer.id > current_row.id)
         )
        WHERE current_row.action_type = 'USER_STATUS_DISABLE'
          AND current_row.entity_type = 'USER'
          AND current_row.status = 'PENDING'
          AND newer.id IS NULL
    ) canonical_ids
);

CREATE UNIQUE INDEX uk_approval_request_pending_request_key
    ON approval_request (pending_request_key);
