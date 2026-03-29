ALTER TABLE approval_request
    ADD COLUMN pending_request_key VARCHAR(191) NULL;

UPDATE approval_request
SET pending_request_key = NULL;

UPDATE approval_request
SET status = 'REJECTED',
    pending_request_key = NULL,
    reviewed_by = NULL,
    reviewed_at = NULL,
    executed_at = NULL
WHERE action_type = 'USER_STATUS_DISABLE'
  AND entity_type = 'USER'
  AND status = 'PENDING'
  AND EXISTS (
    SELECT 1
    FROM approval_request newer
    WHERE newer.action_type = 'USER_STATUS_DISABLE'
      AND newer.entity_type = 'USER'
      AND newer.status = 'PENDING'
      AND newer.tenant_id = approval_request.tenant_id
      AND newer.entity_id = approval_request.entity_id
      AND (
        newer.created_at > approval_request.created_at
        OR (newer.created_at = approval_request.created_at AND newer.id > approval_request.id)
      )
  );

UPDATE approval_request
SET pending_request_key = CONCAT('USER_STATUS_DISABLE:', tenant_id, ':', entity_id)
WHERE action_type = 'USER_STATUS_DISABLE'
  AND entity_type = 'USER'
  AND status = 'PENDING'
  AND NOT EXISTS (
    SELECT 1
    FROM approval_request newer
    WHERE newer.action_type = 'USER_STATUS_DISABLE'
      AND newer.entity_type = 'USER'
      AND newer.status = 'PENDING'
      AND newer.tenant_id = approval_request.tenant_id
      AND newer.entity_id = approval_request.entity_id
      AND (
        newer.created_at > approval_request.created_at
        OR (newer.created_at = approval_request.created_at AND newer.id > approval_request.id)
      )
  );

CREATE UNIQUE INDEX uk_approval_request_pending_request_key
    ON approval_request (pending_request_key);
