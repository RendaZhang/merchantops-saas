package com.renda.merchantops.api.importjob;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.user.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ImportJobOperatorValidator {

    private final UserQueryUseCase userQueryUseCase;

    void requireOperatorInTenant(Long tenantId, Long operatorId) {
        if (tenantId == null || operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        try {
            userQueryUseCase.getUserDetail(tenantId, operatorId);
        } catch (BizException ex) {
            if (ex.getErrorCode() == ErrorCode.NOT_FOUND) {
                throw new BizException(ErrorCode.BAD_REQUEST, "operator does not belong to tenant");
            }
            throw ex;
        }
    }
}
