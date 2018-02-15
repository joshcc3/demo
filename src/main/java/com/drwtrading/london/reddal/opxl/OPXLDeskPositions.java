package com.drwtrading.london.reddal.opxl;

import java.util.Map;

public class OPXLDeskPositions {

    public final Map<String, Long> positions;

    OPXLDeskPositions(final Map<String, Long> positions) {

        this.positions = positions;
    }
}
