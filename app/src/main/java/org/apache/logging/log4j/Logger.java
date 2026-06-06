package org.apache.logging.log4j;

/**
 * Stub Logger interface for Apache POI Android compatibility.
 * Provides no-op logging to prevent NoClassDefFoundError.
 */
public interface Logger {
    void debug(String msg);
    void info(String msg);
    void warn(String msg);
    void error(String msg);
    void error(String msg, Throwable t);
    boolean isDebugEnabled();
    boolean isInfoEnabled();
}
