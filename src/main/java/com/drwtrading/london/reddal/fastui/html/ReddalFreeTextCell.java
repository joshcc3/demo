package com.drwtrading.london.reddal.fastui.html;

import com.drwtrading.london.icepie.transport.data.LadderTextCell;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum ReddalFreeTextCell {

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
    R4C5(HTML.R4C5),

    DESK_POSITION(HTML.DESK_POSITION);

    private final String cellID;
    public final String htmlID;

    public static final EnumMap<LadderTextCell, ReddalFreeTextCell> FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP;

    static {
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP = new EnumMap<>(LadderTextCell.class);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R1C1, R1C1);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R1C2, R1C2);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R1C3, R1C3);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R1C4, R1C4);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R2C1, R2C1);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R2C3, R2C3);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R2C5, R2C5);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R3C2, R3C2);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R3C3, R3C3);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R3C4, R3C4);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R4C1, R4C1);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R4C2, R4C2);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R4C3, R4C3);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R4C4, R4C4);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.R4C5, R4C5);
        FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.put(LadderTextCell.DESK_POSITION, DESK_POSITION);
    }

    private ReddalFreeTextCell(final String cellID) {

        this.cellID = cellID;
        this.htmlID = HTML.TEXT_PREFIX + cellID;
    }

    private static final Map<String, ReddalFreeTextCell> LOOKUP;

    static {
        LOOKUP = new HashMap<>();
        for (final ReddalFreeTextCell cell : ReddalFreeTextCell.values()) {
            LOOKUP.put(cell.cellID, cell);
        }
    }

    public static ReddalFreeTextCell getCell(final String id) {
        return LOOKUP.get(id);
    }

    public static ReddalFreeTextCell getFromTransportCell(final LadderTextCell freeTextCell) {
        return FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.get(freeTextCell);
    }
}
