package com.rstmdb.client.exception;

/**
 * Wire protocol error code constants.
 */
public final class ErrorCodes {
    public static final String UNSUPPORTED_PROTOCOL = "UNSUPPORTED_PROTOCOL";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String MACHINE_NOT_FOUND = "MACHINE_NOT_FOUND";
    public static final String MACHINE_VERSION_EXISTS = "MACHINE_VERSION_EXISTS";
    public static final String MACHINE_VERSION_LIMIT_EXCEEDED = "MACHINE_VERSION_LIMIT_EXCEEDED";
    public static final String INSTANCE_NOT_FOUND = "INSTANCE_NOT_FOUND";
    public static final String INSTANCE_EXISTS = "INSTANCE_EXISTS";
    public static final String INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String GUARD_FAILED = "GUARD_FAILED";
    public static final String CONFLICT = "CONFLICT";
    public static final String WAL_IO_ERROR = "WAL_IO_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String RATE_LIMITED = "RATE_LIMITED";

    private ErrorCodes() {}
}
