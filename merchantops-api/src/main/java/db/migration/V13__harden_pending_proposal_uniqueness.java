package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class V13__harden_pending_proposal_uniqueness extends BaseJavaMigration {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String ACTION_IMPORT_JOB_SELECTIVE_REPLAY = "IMPORT_JOB_SELECTIVE_REPLAY";
    private static final String ACTION_TICKET_COMMENT_CREATE = "TICKET_COMMENT_CREATE";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        clearResolvedPendingKeys(connection);

        List<PendingApprovalRow> rows = loadPendingProposalRows(connection);
        Set<String> claimedKeys = new HashSet<>();
        for (PendingApprovalRow row : rows) {
            String pendingRequestKey = toPendingRequestKey(row);
            if (claimedKeys.add(pendingRequestKey)) {
                updatePendingRequestKey(connection, row.id(), pendingRequestKey);
                continue;
            }
            rejectSupersededPendingRow(connection, row.id());
        }
    }

    private void clearResolvedPendingKeys(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE approval_request
                SET pending_request_key = NULL
                WHERE action_type IN (?, ?)
                  AND status <> ?
                """)) {
            statement.setString(1, ACTION_IMPORT_JOB_SELECTIVE_REPLAY);
            statement.setString(2, ACTION_TICKET_COMMENT_CREATE);
            statement.setString(3, STATUS_PENDING);
            statement.executeUpdate();
        }
    }

    private List<PendingApprovalRow> loadPendingProposalRows(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, action_type, entity_id, payload_json
                FROM approval_request
                WHERE status = ?
                  AND action_type IN (?, ?)
                ORDER BY created_at DESC, id DESC
                """)) {
            statement.setString(1, STATUS_PENDING);
            statement.setString(2, ACTION_IMPORT_JOB_SELECTIVE_REPLAY);
            statement.setString(3, ACTION_TICKET_COMMENT_CREATE);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PendingApprovalRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new PendingApprovalRow(
                            resultSet.getLong("id"),
                            resultSet.getLong("tenant_id"),
                            resultSet.getString("action_type"),
                            resultSet.getLong("entity_id"),
                            resultSet.getString("payload_json")
                    ));
                }
                return rows;
            }
        }
    }

    private String toPendingRequestKey(PendingApprovalRow row) throws Exception {
        JsonNode payload = objectMapper.readTree(row.payloadJson());
        String actionType = normalizeKey(row.actionType());
        if (ACTION_IMPORT_JOB_SELECTIVE_REPLAY.equals(actionType)) {
            long sourceJobId = readMatchingId(payload, "sourceJobId", row.entityId());
            List<String> errorCodes = readCanonicalErrorCodes(payload);
            return importJobSelectiveReplayKey(row.tenantId(), sourceJobId, errorCodes);
        }
        if (ACTION_TICKET_COMMENT_CREATE.equals(actionType)) {
            String commentContent = readTrimmedText(payload, "commentContent");
            return ticketCommentCreateKey(row.tenantId(), row.entityId(), commentContent);
        }
        throw new IllegalStateException("unsupported approval action: " + row.actionType());
    }

    private long readMatchingId(JsonNode payload, String fieldName, Long expectedId) {
        JsonNode node = payload.get(fieldName);
        if (node == null || !node.canConvertToLong()) {
            throw new IllegalStateException("approval payload missing numeric field: " + fieldName);
        }
        long payloadId = node.asLong();
        if (!Long.valueOf(payloadId).equals(expectedId)) {
            throw new IllegalStateException("approval payload " + fieldName + " does not match entity_id");
        }
        return payloadId;
    }

    private List<String> readCanonicalErrorCodes(JsonNode payload) {
        JsonNode errorCodesNode = payload.get("errorCodes");
        if (errorCodesNode == null || !errorCodesNode.isArray() || errorCodesNode.isEmpty()) {
            throw new IllegalStateException("approval payload missing errorCodes");
        }
        List<String> canonical = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode node : errorCodesNode) {
            String value = node == null ? null : node.asText(null);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("approval payload contains blank errorCode");
            }
            String trimmed = value.trim();
            if (seen.add(trimmed)) {
                canonical.add(trimmed);
            }
        }
        canonical.sort(String::compareTo);
        return List.copyOf(canonical);
    }

    private String readTrimmedText(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        String value = node == null ? null : node.asText(null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("approval payload missing text field: " + fieldName);
        }
        return value.trim();
    }

    private void updatePendingRequestKey(Connection connection, Long id, String pendingRequestKey) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE approval_request
                SET pending_request_key = ?
                WHERE id = ?
                """)) {
            statement.setString(1, pendingRequestKey);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    private void rejectSupersededPendingRow(Connection connection, Long id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE approval_request
                SET status = ?,
                    pending_request_key = NULL,
                    reviewed_by = NULL,
                    reviewed_at = COALESCE(reviewed_at, created_at, CURRENT_TIMESTAMP),
                    executed_at = NULL
                WHERE id = ?
                """)) {
            statement.setString(1, STATUS_REJECTED);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    private String importJobSelectiveReplayKey(Long tenantId, Long sourceJobId, List<String> errorCodes) {
        return ACTION_IMPORT_JOB_SELECTIVE_REPLAY
                + ":" + requireValue(tenantId, "tenantId")
                + ":" + requireValue(sourceJobId, "sourceJobId")
                + ":" + md5Hex(String.join("|", requireValue(errorCodes, "errorCodes")));
    }

    private String ticketCommentCreateKey(Long tenantId, Long ticketId, String commentContent) {
        return ACTION_TICKET_COMMENT_CREATE
                + ":" + requireValue(tenantId, "tenantId")
                + ":" + requireValue(ticketId, "ticketId")
                + ":" + md5Hex(requireValue(commentContent, "commentContent"));
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + " must not be null");
        }
        return value;
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm unavailable", ex);
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record PendingApprovalRow(Long id,
                                      Long tenantId,
                                      String actionType,
                                      Long entityId,
                                      String payloadJson) {
    }
}
