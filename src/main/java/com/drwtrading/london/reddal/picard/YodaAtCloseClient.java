package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.YodaTransportComponents;
import com.drwtrading.london.eeif.yoda.transport.data.AtCloseSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSymbolSideKey;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class YodaAtCloseClient implements ITransportCacheListener<YodaSymbolSideKey, AtCloseSignal> {

    private final long milliAtMidnightUTC;
    private final IFuseBox<YodaTransportComponents> fuseBox;
    private final Publisher<PicardRow> rowPublisher;

    private final DecimalFormat priceDF;

    private final Map<String, PicardRow> rowBySymbol;

    public YodaAtCloseClient(final IClock clock, final IFuseBox<YodaTransportComponents> monitor, final Publisher<PicardRow> rowPublisher) {

        this.milliAtMidnightUTC = clock.getMillisAtMidnightUTC();
        this.fuseBox = monitor;
        this.rowPublisher = rowPublisher;

        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 5);

        this.rowBySymbol = new HashMap<>();
    }

    @Override
    public boolean initialValue(final int transportID, final AtCloseSignal signal) {
        return updateValue(transportID, signal);
    }

    @Override
    public boolean updateValue(final int transportID, final AtCloseSignal signal) {

        final String symbol = signal.key.symbol;

        final PicardRow oldRow = rowBySymbol.get(symbol);
        final boolean isNewRow = null == oldRow;

        if (0 < signal.milliSinceMidnight) {

            final long signalMilliSinceUTC = milliAtMidnightUTC + signal.milliSinceMidnight;
            final String closePrice = priceDF.format(signal.closePrice / (double) Constants.NORMALISING_FACTOR);

            final BookSide oppositeSide;

            switch (signal.getKey().side) {
                case BID: {
                    oppositeSide = BookSide.ASK;
                    break;
                }
                case ASK: {
                    oppositeSide = BookSide.BID;
                    break;
                }
                default: {
                    fuseBox.logError(YodaTransportComponents.AT_CLOSE_CACHE,
                            "Unexpected atClose signal, side [" + signal.getKey().side + "].");
                    oppositeSide = null;
                }
            }

            if (null != oppositeSide) {

                final PicardRow row =
                        new PicardRow(signalMilliSinceUTC, signal.key.symbol, InstType.EQUITY, null, oppositeSide, signal.closePrice,
                                closePrice, PicardSpotter.getBPSThrough(signal.theoPrice, signal.closePrice),
                                Math.abs(signal.theoPrice - signal.closePrice), PicardRowState.LIVE, "AT_CLOSE", false, isNewRow);

                rowBySymbol.put(symbol, row);
                rowPublisher.publish(row);
            }

        } else if (!isNewRow) {
            final PicardRow row = new PicardRow(oldRow, PicardRowState.DEAD);
            rowBySymbol.remove(symbol);
            rowPublisher.publish(row);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        // no-op
    }
}
