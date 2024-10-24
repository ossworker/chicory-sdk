package org.extism.chicory.sdk;

import com.dylibso.chicory.log.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jWasmLogger implements Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("chicory");

    @Override
    public void log(Level level, String msg, Throwable throwable) {

        LOGGER.atLevel(toSlf4jLogLevel(level)).setCause(throwable).log(()->msg);
    }

    @Override
    public boolean isLoggable(Level level) {
        return LOGGER.isEnabledForLevel(toSlf4jLogLevel(level));
    }


    org.slf4j.event.Level toSlf4jLogLevel(Level level) {
        switch (level) {
            case ALL:
                return org.slf4j.event.Level.DEBUG;
            case TRACE:
                return org.slf4j.event.Level.TRACE;
            case DEBUG:
                return org.slf4j.event.Level.DEBUG;
            case INFO:
                return org.slf4j.event.Level.INFO;
            case WARNING:
                return org.slf4j.event.Level.WARN;
            case ERROR:
                return org.slf4j.event.Level.ERROR;
            default:
                throw new IllegalArgumentException("Unsupported logger level: " + level);
        }
    }
}
