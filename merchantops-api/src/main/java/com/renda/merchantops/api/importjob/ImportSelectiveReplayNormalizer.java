package com.renda.merchantops.api.importjob;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ImportSelectiveReplayNormalizer {

    public List<String> normalizeSelectedErrorCodes(List<String> errorCodes) {
        if (errorCodes == null || errorCodes.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not be empty");
        }
        List<String> normalized = errorCodes.stream()
                .map(errorCode -> {
                    if (!StringUtils.hasText(errorCode)) {
                        throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not contain blank values");
                    }
                    return errorCode.trim();
                })
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not be empty");
        }
        return normalized;
    }
}
