package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.service.UserCommandService;
import com.renda.merchantops.api.validation.PasswordRules;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class UserCsvImportProcessor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserCommandService userCommandService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRow(ImportJobEntity job, int rowNumber, List<String> columns) {
        String username = required(columns.get(0), "username", "MISSING_USERNAME");
        String displayName = required(columns.get(1), "displayName", "MISSING_DISPLAY_NAME");
        String email = required(columns.get(2), "email", "INVALID_EMAIL");
        String password = requiredPreserve(columns.get(3), "password", "INVALID_PASSWORD");
        String roleCodesText = required(columns.get(4), "roleCodes", "MISSING_ROLE_CODES");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ImportRowProcessingException("INVALID_EMAIL", "email format is invalid");
        }
        if (PasswordRules.hasBoundaryWhitespace(password)) {
            throw new ImportRowProcessingException("INVALID_PASSWORD", PasswordRules.NO_BOUNDARY_WHITESPACE_MESSAGE);
        }

        List<String> roleCodes = Arrays.stream(roleCodesText.split("\\|", -1))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (roleCodes.isEmpty()) {
            throw new ImportRowProcessingException("MISSING_ROLE_CODES", "roleCodes must not be empty");
        }

        UserCreateCommand command = new UserCreateCommand(username, password, displayName, email, roleCodes);
        try {
            userCommandService.createUser(
                    job.getTenantId(),
                    job.getRequestedBy(),
                    RequestIdPolicy.createImportRowRequestId(job.getRequestId(), rowNumber),
                    command
            );
        } catch (BizException ex) {
            throw mapBizException(ex);
        } catch (DataIntegrityViolationException ex) {
            throw mapDataIntegrityViolation(ex);
        }
    }

    private String required(String value, String fieldName, String code) {
        if (!StringUtils.hasText(value)) {
            throw new ImportRowProcessingException(code, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String requiredPreserve(String value, String fieldName, String code) {
        if (!StringUtils.hasText(value)) {
            throw new ImportRowProcessingException(code, fieldName + " must not be blank");
        }
        return value;
    }

    private ImportRowProcessingException mapBizException(BizException ex) {
        String message = ex.getMessage() == null ? "import row failed" : ex.getMessage();
        if (message.contains("username already exists")) {
            return new ImportRowProcessingException("DUPLICATE_USERNAME", message);
        }
        if (message.contains("roleCodes must exist")) {
            return new ImportRowProcessingException("UNKNOWN_ROLE", message);
        }
        if (message.contains("password must not start or end with whitespace")) {
            return new ImportRowProcessingException("INVALID_PASSWORD", message);
        }
        if (message.contains("email")) {
            return new ImportRowProcessingException("INVALID_EMAIL", message);
        }
        return new ImportRowProcessingException("BUSINESS_VALIDATION_FAILED", message);
    }

    private ImportRowProcessingException mapDataIntegrityViolation(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String message = mostSpecificCause == null ? ex.getMessage() : mostSpecificCause.getMessage();
        if (message != null && message.toLowerCase().contains("username")) {
            return new ImportRowProcessingException("DUPLICATE_USERNAME", "username already exists in tenant");
        }
        return new ImportRowProcessingException("ROW_PERSISTENCE_FAILED", "failed to persist import row");
    }
}
