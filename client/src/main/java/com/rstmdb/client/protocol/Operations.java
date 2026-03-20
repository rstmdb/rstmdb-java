package com.rstmdb.client.protocol;

/**
 * RCP protocol operation string constants.
 */
public final class Operations {
    public static final String HELLO = "HELLO";
    public static final String AUTH = "AUTH";
    public static final String PING = "PING";
    public static final String BYE = "BYE";
    public static final String INFO = "INFO";
    public static final String PUT_MACHINE = "PUT_MACHINE";
    public static final String GET_MACHINE = "GET_MACHINE";
    public static final String LIST_MACHINES = "LIST_MACHINES";
    public static final String CREATE_INSTANCE = "CREATE_INSTANCE";
    public static final String GET_INSTANCE = "GET_INSTANCE";
    public static final String LIST_INSTANCES = "LIST_INSTANCES";
    public static final String DELETE_INSTANCE = "DELETE_INSTANCE";
    public static final String APPLY_EVENT = "APPLY_EVENT";
    public static final String BATCH = "BATCH";
    public static final String WATCH_INSTANCE = "WATCH_INSTANCE";
    public static final String WATCH_ALL = "WATCH_ALL";
    public static final String UNWATCH = "UNWATCH";
    public static final String SNAPSHOT_INSTANCE = "SNAPSHOT_INSTANCE";
    public static final String WAL_READ = "WAL_READ";
    public static final String WAL_STATS = "WAL_STATS";
    public static final String COMPACT = "COMPACT";
    public static final String FLUSH_ALL = "FLUSH_ALL";

    private Operations() {}
}
