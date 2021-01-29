package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.data.RestingOrderSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSymbolSideKey;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class YodaRestingOrderClient implements ITransportCacheListener<YodaSymbolSideKey, RestingOrderSignal> {

    private final long millisAtMidnight;

    private final Publisher<StockAlert> stockAlerts;

    private final SimpleDateFormat sdf;
    private final DecimalFormat priceDF;

    public YodaRestingOrderClient(final long millisAtMidnight, final Publisher<StockAlert> stockAlerts) {

        this.millisAtMidnight = millisAtMidnight;
        this.stockAlerts = stockAlerts;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 5);
    }

    @Override
    public boolean initialValue(final int transportID, final RestingOrderSignal item) {
        return updateValue(transportID, item);
    }

    @Override
    public boolean updateValue(final int transportID, final RestingOrderSignal signal) {

        if (0 < signal.price) {
            final String timestamp = sdf.format(millisAtMidnight + signal.milliSinceMidnight);
            final String start = sdf.format(signal.timePlacedMilliSinceMidnight);
            final String side;
            switch (signal.key.side) {
                case BID: {
                    side = "Buy ";
                    break;
                }

                case ASK: {
                    side = "Sell ";
                    break;
                }
                case BOTH: {
                    side = "Both ";
                    break;
                }
                default: {
                    side = "Unknown ";
                    break;
                }
            }
            final String price = priceDF.format(signal.price / (double) Constants.NORMALISING_FACTOR);
            final String msg = side + signal.qty + " @ " + price + " [at " + start + "].";

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
