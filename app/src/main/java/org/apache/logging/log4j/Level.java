package org.apache.logging.log4j;

/**
 * Stub Level enum for Apache POI Android compatibility.
 */
public enum Level {
    OFF(0),
    FATAL(100),
    ERROR(200),
    WARN(300),
    INFO(400),
    DEBUG(500),
    TRACE(600),
    ALL(Integer.MAX_VALUE);

    private final int intLevel;

    Level(int intLevel) {
        this.intLevel = intLevel;
    }

    public int intLevel() {
        return intLevel;
    }

    public boolean isMoreSpecificThan(Level level) {
        return this.intLevel >= level.intLevel;
    }

    public boolean isLessSpecificThan(Level level) {
        return this.intLevel <= level.intLevel;
    }
}
