package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.reddal.data.MDForSymbol;

class PicardData {

    final String symbol;
    final MDForSymbol mdForSymbol;

    LaserLine bidLaserLine;
    LaserLine askLaserLine;

    PicardRow previousRow;

    PicardData(final String symbol, final MDForSymbol mdForSymbol) {

        this.symbol = symbol;
        this.mdForSymbol = mdForSymbol;
    }
}
