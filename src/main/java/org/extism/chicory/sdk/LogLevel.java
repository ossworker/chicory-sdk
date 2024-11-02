package org.extism.chicory.sdk;

import com.dylibso.chicory.log.Logger;

/**
 * @author workoss
 */

public enum LogLevel {
    /**
     * trace
     */
    TRACE,
    /**
     * debug
     */
    DEBUG,
    /**
     * info
     */
    INFO,
    /**
     * warn
     */
    WARN,
    /**
     * error
     */
    ERROR;

    Logger.Level toChicoryLogLevel() {
        switch (this) {
            case TRACE:
                return Logger.Level.TRACE;
            case DEBUG:
                return Logger.Level.DEBUG;
            case INFO:
                return Logger.Level.INFO;
            case WARN:
                return Logger.Level.WARNING;
            case ERROR:
                return Logger.Level.ERROR;
            default:
                throw new IllegalArgumentException("unknown type " + this);
        }
    }
}

