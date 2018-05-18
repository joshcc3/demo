package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;

class PicardData {

    final String symbol;
    final MDForSymbol mdForSymbol;

    LaserLineValue bidLaserLine;
    LaserLineValue askLaserLine;

    PicardRow previousRow;

    PicardData(final String symbol, final MDForSymbol mdForSymbol) {

        this.symbol = symbol;
        this.mdForSymbol = mdForSymbol;
    }
}
