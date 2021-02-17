package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.fastui.html.CSSClass;

public enum PricingMode {
    BPS(CSSClass.BPS),
    EFP(CSSClass.EFP),
    RAW(CSSClass.RAW);

    public final CSSClass cssClass;

    PricingMode(final CSSClass cssClass) {
        this.cssClass = cssClass;
    }
}
