package org.apache.logging.log4j;

/**
 * Stub Logger interface for Apache POI Android compatibility.
 * Provides comprehensive no-op logging to prevent NoClassDefFoundError.
 */
public interface Logger {
    // Debug level
    void debug(String msg);
    void debug(String msg, Object... params);
    void debug(String msg, Throwable t);
    boolean isDebugEnabled();

    // Info level
    void info(String msg);
    void info(String msg, Object... params);
    void info(String msg, Throwable t);
    boolean isInfoEnabled();

    // Warn level
    void warn(String msg);
    void warn(String msg, Object... params);
    void warn(String msg, Throwable t);
    boolean isWarnEnabled();

    // Error level
    void error(String msg);
    void error(String msg, Object... params);
    void error(String msg, Throwable t);
    boolean isErrorEnabled();

    // Fatal level
    void fatal(String msg);
    void fatal(String msg, Object... params);
    void fatal(String msg, Throwable t);
    boolean isFatalEnabled();

    // Trace level
    void trace(String msg);
    void trace(String msg, Object... params);
    void trace(String msg, Throwable t);
    boolean isTraceEnabled();

    // Logger identity
    String getName();
}
