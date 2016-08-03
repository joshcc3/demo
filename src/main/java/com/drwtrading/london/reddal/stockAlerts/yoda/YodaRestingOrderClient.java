package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.data.RestingOrderSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSignalKey;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;

public class YodaRestingOrderClient implements ITransportCacheListener<YodaSignalKey, RestingOrderSignal> {

    private final Publisher<StockAlert> stockAlerts;

    private final SimpleDateFormat sdf;

    public YodaRestingOrderClient(final Publisher<StockAlert> stockAlerts) {

        this.stockAlerts = stockAlerts;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
    }

    @Override
    public boolean setKey(final int localID, final YodaSignalKey key) {
        return true;
    }

    @Override
    public boolean setValue(final int localID, final RestingOrderSignal signal) {

        final String timestamp = sdf.format(signal.milliSinceMidnight);
        final String period = sdf.format(signal.timePlacedMilliSinceMidnight);
        final String msg = signal.qty + " @ " + signal.price + " [" + signal.depthOriginallyPlaced + " deep at " + period + "].";

        final StockAlert alert = new StockAlert(timestamp, signal.key.signal.name(), signal.key.symbol, msg);
        stockAlerts.publish(alert);
        return true;
    }

    @Override
    public void batchComplete() {
        // no-op
    }
}
