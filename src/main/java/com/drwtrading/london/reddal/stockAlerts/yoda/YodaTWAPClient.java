package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.data.TWAPSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSymbolSideKey;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class YodaTWAPClient implements ITransportCacheListener<YodaSymbolSideKey, TWAPSignal> {

    private final long millisAtMidnight;

    private final Publisher<StockAlert> stockAlerts;

    private final SimpleDateFormat sdf;
    private final DecimalFormat df;

    public YodaTWAPClient(final long millisAtMidnight, final Publisher<StockAlert> stockAlerts) {

        this.millisAtMidnight = millisAtMidnight;

        this.stockAlerts = stockAlerts;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
        this.df = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 1);
    }

    @Override
    public boolean initialValue(final int transportID, final TWAPSignal item) {
        return updateValue(transportID, item);
    }

    @Override
    public boolean updateValue(final int transportID, final TWAPSignal signal) {

        if (0 < signal.volumeBucketMax) {
            final String timestamp = sdf.format(millisAtMidnight + signal.milliSinceMidnight);
            final String period = df.format(signal.twapPeriodMillis / 1000d);
            final String duration = df.format(signal.twapDurationMillis / 1000d);
            final String action;

            switch (signal.key.side) {
                case BID: {
                    action = "Selling every ";
                    break;
                }
                case ASK: {
                    action = "Buying every ";
                    break;

                }
                case BOTH: {
                    action = "Selling and buying every ";
                    break;
                }
                default: {
                    action = "UNKNOWN every ";
                }
            }

            final String msg =
                    action + period + " for " + duration + " seconds [Bucket " + signal.volumeBucketMin + ", " + signal.volumeBucketMax +
                            "].";

            final StockAlert alert = new StockAlert(signal.milliSinceMidnight, timestamp, signal.key.signal.name(), signal.key.symbol, msg);
            stockAlerts.publish(alert);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        // no-op
    }
}
