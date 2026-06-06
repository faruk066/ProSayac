package org.apache.logging.log4j.message;

/**
 * Stub Message interface for Apache POI Android compatibility.
 */
public interface Message {
    String getFormattedMessage();
    String getFormat();
    Object[] getParameters();
    Throwable getThrowable();
}
