package com.renda.merchantops.api.importjob.messaging;

import java.util.List;

record ImportJobChunkRow(
        int rowNumber,
        List<String> columns,
        String rawPayload
) {
}
