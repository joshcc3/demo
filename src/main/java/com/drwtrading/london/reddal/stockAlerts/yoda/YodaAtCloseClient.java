package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.yoda.transport.data.AtCloseSignal;
import com.drwtrading.london.eeif.yoda.transport.data.YodaSymbolSideKey;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class YodaAtCloseClient implements ITransportCacheListener<YodaSymbolSideKey, AtCloseSignal> {

    private final Publisher<StockAlert> stockAlerts;

    private final SimpleDateFormat sdf;
    private final DecimalFormat priceDF;

    public YodaAtCloseClient(final Publisher<StockAlert> stockAlerts) {

        this.stockAlerts = stockAlerts;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 5);
    }

    @Override
    public boolean setKey(final int localID, final YodaSymbolSideKey key) {
        return true;
    }

    @Override
    public boolean setValue(final int localID, final AtCloseSignal signal) {

        if (0 < signal.milliSinceMidnight) {
            final String timestamp = sdf.format(signal.milliSinceMidnight);
            final String side = BookSide.BID == signal.getKey().side ? "Buy " : "Sell ";
            final String closePrice = priceDF.format(signal.closePrice / (double) Constants.NORMALISING_FACTOR);
            final String theoPrice = priceDF.format(signal.theoPrice / (double) Constants.NORMALISING_FACTOR);

            final String msg = side + " closed [" + closePrice + "] against theo [" + theoPrice + "].";

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
