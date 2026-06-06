package org.apache.logging.log4j;

/**
 * Stub LogManager to prevent NoClassDefFoundError when Apache POI tries to access log4j.
 * This is a minimal no-op implementation for Android compatibility.
 */
public class LogManager {

    private static final Logger LOGGER = new Logger() {
        @Override
        public void debug(String msg) {
            // No-op
        }

        @Override
        public void info(String msg) {
            // No-op
        }

        @Override
        public void warn(String msg) {
            // No-op
        }

        @Override
        public void error(String msg) {
            // No-op
        }

        @Override
        public void error(String msg, Throwable t) {
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
    };

    public static Logger getLogger(Class<?> clazz) {
        return LOGGER;
    }

    public static Logger getLogger(String name) {
        return LOGGER;
    }
}
