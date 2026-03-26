package com.renda.merchantops.api.importjob.messaging;

record ImportJobStartResult(
        ImportJobStartAction action,
        ImportJobExecutionContext context
) {
    static ImportJobStartResult started(ImportJobExecutionContext context) {
        return new ImportJobStartResult(ImportJobStartAction.STARTED, context);
    }

    static ImportJobStartResult ignore() {
        return new ImportJobStartResult(ImportJobStartAction.IGNORE, null);
    }

    static ImportJobStartResult requeue() {
        return new ImportJobStartResult(ImportJobStartAction.REQUEUE, null);
    }
}
