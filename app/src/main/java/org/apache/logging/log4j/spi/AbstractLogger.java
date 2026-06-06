package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Stub AbstractLogger for Apache POI Android compatibility.
 */
public abstract class AbstractLogger implements Logger {

    @Override
    public void debug(String msg) {
        // No-op
    }

    @Override
    public void debug(String msg, Object... params) {
        // No-op
    }

    @Override
    public void debug(String msg, Throwable t) {
        // No-op
    }

    @Override
    public void info(String msg) {
        // No-op
    }

    @Override
    public void info(String msg, Object... params) {
        // No-op
    }

    @Override
    public void info(String msg, Throwable t) {
        // No-op
    }

    @Override
    public void warn(String msg) {
        // No-op
    }

    @Override
    public void warn(String msg, Object... params) {
        // No-op
    }

    @Override
    public void warn(String msg, Throwable t) {
        // No-op
    }

    @Override
    public void error(String msg) {
        // No-op
    }

    @Override
    public void error(String msg, Object... params) {
        // No-op
    }

    @Override
    public void error(String msg, Throwable t) {
        // No-op
    }

    @Override
    public void fatal(String msg) {
        // No-op
    }

    @Override
    public void fatal(String msg, Object... params) {
        // No-op
    }

    @Override
    public void fatal(String msg, Throwable t) {
        // No-op
    }

    @Override
    public void trace(String msg) {
        // No-op
    }

    @Override
    public void trace(String msg, Object... params) {
        // No-op
    }

    @Override
    public void trace(String msg, Throwable t) {
        // No-op
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public boolean isFatalEnabled() {
        return false;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public String getName() {
        return "AbstractLogger";
    }

    public Level getLevel() {
        return Level.OFF;
    }

    public boolean isEnabled(Level level) {
        return false;
    }
}
