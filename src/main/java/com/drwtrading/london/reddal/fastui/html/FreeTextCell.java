package com.drwtrading.london.reddal.fastui.html;

import java.util.HashMap;
import java.util.Map;

public enum FreeTextCell {

    R1C1(HTML.R1C1),
    R1C2(HTML.R1C2),
    R1C3(HTML.R1C3),
    R1C4(HTML.R1C4),

    R2C1(HTML.R2C1),
    R2C3(HTML.R2C3),
    R2C5(HTML.R2C5),

    R3C2(HTML.R3C2),
    R3C3(HTML.R3C3),
    R3C4(HTML.R3C4),

    R4C1(HTML.R4C1),
    R4C2(HTML.R4C2),
    R4C3(HTML.R4C3),
    R4C4(HTML.R4C4),
    R4C5(HTML.R4C5);

    private final String cellID;
    public final String htmlID;

    private FreeTextCell(final String cellID) {

        this.cellID = cellID;
        this.htmlID = HTML.TEXT_PREFIX + cellID;
    }

    private static final Map<String, FreeTextCell> LOOKUP;

    static {
        LOOKUP = new HashMap<>();
        for (final FreeTextCell cell : FreeTextCell.values()) {
            LOOKUP.put(cell.cellID, cell);
        }
    }

    public static FreeTextCell getCell(final String id) {
        return LOOKUP.get(id);
    }
}
