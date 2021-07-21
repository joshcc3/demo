package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.icepie.transport.io.LadderTextNumberUnits;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.HTML;

import java.util.HashMap;
import java.util.Map;

public enum PricingMode {
    BPS(CSSClass.BPS, HTML.PRICING_BPS),
    EFP(CSSClass.EFP, HTML.PRICING_EFP),
    RAW(CSSClass.RAW, HTML.PRICING_RAW);

    private static final Map<String, PricingMode> BUTTON_TO_MODE = new HashMap<>();

    static {
        for (final PricingMode mode : values()) {
            BUTTON_TO_MODE.put(mode.htmlButton, mode);
        }
    }

    public final CSSClass cssClass;
    public final String htmlButton;

    PricingMode(final CSSClass cssClass, final String htmlButton) {
        this.cssClass = cssClass;
        this.htmlButton = htmlButton;
    }

    public static PricingMode getFromHtml(final String htmlButton) {
        return BUTTON_TO_MODE.get(htmlButton);
    }
}
