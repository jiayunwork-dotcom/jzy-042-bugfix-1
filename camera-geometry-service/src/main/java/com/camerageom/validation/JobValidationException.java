package com.camerageom.validation;

import java.util.Map;

/**
 * Carries a typed, structured rejection of a job. Mapped to HTTP 400 by the
 * global exception handler; never leaves a job half-computed.
 */
public class JobValidationException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public JobValidationException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
