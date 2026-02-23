package com.rstmdb.client.exception;

import java.util.Map;

/**
 * Unchecked exception carrying structured error information from the rstmdb server.
 */
public class RstmdbException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;
    private final Map<String, Object> details;

    public RstmdbException(String errorCode, String message, boolean retryable, Map<String, Object> details) {
        super(message != null ? message : errorCode);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public String toString() {
        String msg = getMessage();
        if (msg != null && !msg.equals(errorCode)) {
            return "rstmdb: " + errorCode + ": " + msg;
        }
        return "rstmdb: " + errorCode;
    }

    public static boolean isNotFound(Throwable ex) {
        return hasCode(ex, ErrorCodes.NOT_FOUND);
    }

    public static boolean isInstanceNotFound(Throwable ex) {
        return hasCode(ex, ErrorCodes.INSTANCE_NOT_FOUND);
    }

    public static boolean isMachineNotFound(Throwable ex) {
        return hasCode(ex, ErrorCodes.MACHINE_NOT_FOUND);
    }

    public static boolean isInvalidTransition(Throwable ex) {
        return hasCode(ex, ErrorCodes.INVALID_TRANSITION);
    }

    public static boolean isGuardFailed(Throwable ex) {
        return hasCode(ex, ErrorCodes.GUARD_FAILED);
    }

    public static boolean isConflict(Throwable ex) {
        return hasCode(ex, ErrorCodes.CONFLICT);
    }

    private static boolean hasCode(Throwable ex, String code) {
        if (ex instanceof RstmdbException) {
            return code.equals(((RstmdbException) ex).getErrorCode());
        }
        if (ex != null && ex.getCause() instanceof RstmdbException) {
            return code.equals(((RstmdbException) ex.getCause()).getErrorCode());
        }
        return false;
    }
}
