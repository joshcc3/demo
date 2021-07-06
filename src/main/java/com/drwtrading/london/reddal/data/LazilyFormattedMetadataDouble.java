package com.drwtrading.london.reddal.data;

import java.text.DecimalFormat;

public class LazilyFormattedMetadataDouble {

    private boolean isInitialised;
    private boolean isDirty;
    private DecimalFormat formatter;
    private double value;
    private String formattedValue;

    public LazilyFormattedMetadataDouble() {
        this.isInitialised = false;
        this.isDirty = true;
        this.formattedValue = "";
    }

    public void updateValue(final DecimalFormat formatter, final double value) {

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

}
