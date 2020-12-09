package com.drwtrading.london.reddal.data;

import com.drwtrading.london.icepie.transport.data.LaserLineType;

public class LaserLine {

    public final String symbol;

    private final LaserLineType type;

    private boolean isValid;
    private long value;

    public LaserLine(final String symbol, final LaserLineType type, final long value) {

        this(symbol, type);
        setValue(value);
    }

    public LaserLine(final String symbol, final LaserLineType type) {

        this.symbol = symbol;
        this.type = type;
    }

    void set(final LaserLine other) {

        this.isValid = other.isValid;
        this.value = other.value;
    }

    public LaserLineType getType() {
        return type;
    }

    public void setInvalid() {

        this.isValid = false;
    }

    public void setValue(final long value) {

        this.isValid = true;
        this.value = value;
    }

    public boolean isValid() {
        return isValid;
    }

    public long getValue() {
        return value;
    }
}
