package com.drwtrading.london.reddal.data;

import java.text.DecimalFormat;

public class LazilyFormattedMetadataLong {

    private boolean isInitialised;
    private boolean isDirty;
    private DecimalFormat formatter;
    private long value;
    private String formattedValue;

    public LazilyFormattedMetadataLong() {
        this.isInitialised = false;
        this.isDirty = true;
    }

    public boolean isInitialised() {
        return this.isInitialised;
    }

    public void updateValue(final DecimalFormat formatter, final long value) {

        this.isDirty = this.value == value || null == formattedValue;
        this.formatter = formatter;
        this.value = value;
        this.isInitialised = true;

    }

    public String getFormattedValue() {
        if (isDirty && isInitialised) {
            this.formattedValue = DataUtils.formatPosition(formatter, value);
            this.isDirty = false;
        }
        return this.formattedValue;
    }

    public long getValue() {
        return value;
    }
}
