package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.types.LaserLineType;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.util.FastUtilCollections;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class ExtraDataForSymbol {

    public final String symbol;

    public final Map<String, com.drwtrading.photons.ladder.LaserLine> laserLineByName;

    private final Map<LaserLineType, LaserLine> laserLines;

    private TheoValue theoValue;

    private LastTrade bidLastTrade;
    private LastTrade askLastTrade;

    public ExtraDataForSymbol(final String symbol) {

        this.symbol = symbol;
        this.laserLineByName = FastUtilCollections.newFastMap();

        this.laserLines = new EnumMap<>(LaserLineType.class);
    }

    public void onLaserLine(final com.drwtrading.photons.ladder.LaserLine laserLine) {
        laserLineByName.put(laserLine.getId(), laserLine);
    }

    public void setTheoValue(final TheoValue theoValue) {

        if (null == this.theoValue || this.theoValue.getTheoType().ordinal() <= theoValue.getTheoType().ordinal()) {
            this.theoValue = theoValue;
        }
    }

    public TheoValue getTheoValue() {
        return theoValue;
    }

    public void setLaserLine(final LaserLine laserLine) {
        laserLines.put(laserLine.getType(), laserLine);
    }

    public Collection<LaserLine> getLaserLines() {
        return laserLines.values();
    }

    public void setLastTrade(final LastTrade lastTrade) {

        if (BookSide.BID == lastTrade.getSide()) {
            bidLastTrade = lastTrade;
        } else {
            askLastTrade = lastTrade;
        }
    }

    public boolean hasTraded() {
        return null != bidLastTrade || null != askLastTrade;
    }

    public boolean isLastBuy(final long price) {
        return null != bidLastTrade && bidLastTrade.getPrice() == price;
    }

    public boolean isLastSell(final long price) {
        return null != askLastTrade && askLastTrade.getPrice() == price;
    }
}