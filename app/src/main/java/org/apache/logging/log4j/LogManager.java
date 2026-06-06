package org.apache.logging.log4j;

/**
 * Stub LogManager to prevent NoClassDefFoundError when Apache POI tries to access log4j.
 * This is a comprehensive no-op implementation for Android compatibility.
 */
public class LogManager {

    private static final Logger LOGGER = new NoOpLogger();

    public static Logger getLogger(Class<?> clazz) {
        return LOGGER;
    }

    public static Logger getLogger(String name) {
        return LOGGER;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Logger getRootLogger() {
        return LOGGER;
    }

    public static void shutdown() {
        // No-op
    }

    /**
     * No-op Logger implementation
     */
    private static class NoOpLogger implements Logger {
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
            return "NoOpLogger";
        }
    }
}
