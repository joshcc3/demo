package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.fastui.html.HTML;

public enum LaserLineType {

    NAV(HTML.LASER_NAV),

    GREEN(HTML.LASER_GREEN),
    WHITE(HTML.LASER_WHITE),

    BID(HTML.LASER_BID),
    ASK(HTML.LASER_ASK);

    public final String htmlKey;

    LaserLineType(final String htmlKey) {
        this.htmlKey = htmlKey;
    }
}
