package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.data.TWAPSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSymbolSideKey;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class YodaTWAPClient implements ITransportCacheListener<YodaSymbolSideKey, TWAPSignal> {

    private final Publisher<StockAlert> stockAlerts;

    private final SimpleDateFormat sdf;
    private final DecimalFormat df;

    public YodaTWAPClient(final Publisher<StockAlert> stockAlerts) {

        this.stockAlerts = stockAlerts;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
        this.df = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 1);
    }

    @Override
    public boolean setKey(final int localID, final YodaSymbolSideKey key) {
        return true;
    }

    @Override
    public boolean setValue(final int localID, final TWAPSignal signal) {

        if (0 < signal.volumeBucketMax) {
            final String timestamp = sdf.format(signal.milliSinceMidnight);
            final String period = df.format(signal.twapPeriodMillis / 1000d);
            final String duration = df.format(signal.twapDurationMillis / 1000d);
            final String action;
            if (BookSide.BID == signal.key.side) {
                action = "Selling every ";
            } else {
                action = "Buying every ";
            }
            final String msg =
                    action + period + " for " + duration + " seconds [Bucket " + signal.volumeBucketMin + ", " +
                            signal.volumeBucketMax + "].";

            final StockAlert alert = new StockAlert(timestamp, signal.key.signal.name(), signal.key.symbol, msg);
            stockAlerts.publish(alert);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        // no-op
    }
}
