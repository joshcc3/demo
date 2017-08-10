package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelThreeMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.stacks.opxl.StackRefPriceDetail;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Channel;

import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelThreeBookSubscriber extends BookLevelThreeMonitorAdaptor {

    private final boolean isPrimary;

    private final IResourceMonitor<ReddalComponents> monitor;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Channel<SearchResult> searchResults;
    private final Channel<StockAlert> stockAlertChannel;
    private final Channel<StackRefPriceDetail> stackRefPriceDetails;

    private final Map<String, IBook<IBookLevelWithOrders>> books;
    private final Map<String, MDForSymbol> listeners;

    private final SimpleDateFormat sdf;
    private final long timezoneOffsetMillis;

    public LevelThreeBookSubscriber(final boolean isPrimary, final IResourceMonitor<ReddalComponents> monitor,
            final Channel<SearchResult> searchResults, final Channel<StockAlert> stockAlertChannel,
            final Channel<StackRefPriceDetail> stackRefPriceDetails) {

        this.isPrimary = isPrimary;

        this.monitor = monitor;
        this.searchResults = searchResults;
        this.stockAlertChannel = stockAlertChannel;
        this.stackRefPriceDetails = stackRefPriceDetails;

        this.mdClients = new EnumMap<>(MDSource.class);

        this.books = new HashMap<>();
        this.listeners = new HashMap<>();

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.timezoneOffsetMillis = DateTimeUtil.LONDON_TIME_ZONE.getOffset(System.currentTimeMillis());
    }

    public void setMDClient(final MDSource mdSource, final MDTransportClient client) {

        if (null != mdClients.put(mdSource, client)) {
            monitor.logError(ReddalComponents.MD_L3_HANDLER, "Duplicate client for MDSource [" + mdSource + "].");
        }
    }

    @Override
    public void bookCreated(final IBook<IBookLevelWithOrders> book) {

        books.put(book.getSymbol(), book);
        final MDForSymbol listener = listeners.get(book.getSymbol());
        if (null != listener) {
            bookSubscribe(listener, book);
        }
        if (isPrimary) {
            final SearchResult searchResult = new SearchResult(book);
            searchResults.publish(searchResult);
        }
    }

    @Override
    public void referencePrice(final IBook<IBookLevelWithOrders> book, final IBookReferencePrice refPrice) {

        if (isPrimary && refPrice.isValid()) {
            switch (refPrice.getReferencePoint()) {
                case RFQ: {
                    if (book.isValid()) {
                        final String timestamp =
                                sdf.format(timezoneOffsetMillis + (refPrice.getReceivedNanoSinceMidnight() / DateTimeUtil.NANOS_IN_MILLIS));
                        final StockAlert stockAlert = new StockAlert(timestamp, "RFQ", book.getSymbol(), "Qty: " + refPrice.getQty());
                        stockAlertChannel.publish(stockAlert);
                    }
                    break;
                }
                case YESTERDAY_CLOSE: {
                    final String symbol = book.getSymbol();
                    final StackRefPriceDetail refPriceDetail = new StackRefPriceDetail(symbol, refPrice.getPrice(), book.getTickTable());
                    stackRefPriceDetails.publish(refPriceDetail);
                }
            }
        }
    }

    public void subscribeForMD(final String symbol, final MDForSymbol listener) {

        listeners.put(symbol, listener);
        final IBook<?> book = books.get(symbol);
        if (null != book) {
            bookSubscribe(listener, book);
        }
    }

    private void bookSubscribe(final MDForSymbol listener, final IBook<?> book) {

        listener.setBook(book);
        final MDTransportClient client = mdClients.get(book.getSourceExch());
        client.subscribeToInst(book.getLocalID());
        monitor.setOK(ReddalComponents.MD_L3_HANDLER);
    }

    public void unsubscribeForMD(final String symbol) {

        listeners.remove(symbol);
        final IBook<IBookLevelWithOrders> book = books.get(symbol);
        if (null != book) {
            final MDTransportClient client = mdClients.get(book.getSourceExch());
            client.unsubscribeToInst(book.getLocalID());
        }

    }

    @Override
    public void trade(final IBook<IBookLevelWithOrders> book, final long execID, final AggressorSide side, final long price,
            final long qty) {

        final MDForSymbol listener = listeners.get(book.getSymbol());
        if (null != listener) {
            listener.trade(price, qty);
        }
    }

    @Override
    public void logErrorMsg(final String msg) {
        monitor.logError(ReddalComponents.MD_L3_HANDLER, msg);
    }

    @Override
    public void logErrorMsg(final String msg, final Throwable t) {
        monitor.logError(ReddalComponents.MD_L3_HANDLER, msg, t);
    }
}
