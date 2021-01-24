package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.data.SweepSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSymbolSideKey;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;

public class YodaSweepClient implements ITransportCacheListener<YodaSymbolSideKey, SweepSignal> {

    private final long millisAtMidnight;

    private final Publisher<StockAlert> stockAlerts;

    private final SimpleDateFormat sdf;

    public YodaSweepClient(final long millisAtMidnight, final Publisher<StockAlert> stockAlerts) {

        this.millisAtMidnight = millisAtMidnight;

        this.stockAlerts = stockAlerts;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
    }

    @Override
    public boolean initialValue(final int transportID, final SweepSignal item) {
        return updateValue(transportID, item);
    }

    @Override
    public boolean updateValue(final int transportID, final SweepSignal signal) {

        if (0 < signal.numLevels) {
            final String timestamp = sdf.format(millisAtMidnight + signal.milliSinceMidnight);
            final String action;
            switch (signal.key.side) {
                case ASK: {
                    action = "Sells ";
                    break;
                }
                case BID: {
                    action = "Buys ";
                    break;
                }
                case BOTH: {
                    action = "Both ";
                    break;
                }
                default: {
                    action = "UNKNOWN ";
                    break;
                }
            }
            final String msg = action + signal.numLevels + " levels [qty:" + signal.qty + "].";

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
