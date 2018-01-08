package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelTwoMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.london.reddal.stacks.opxl.StackRefPriceDetail;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.google.common.collect.HashMultimap;
import org.jetlang.channels.Channel;

import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelTwoBookSubscriber extends BookLevelTwoMonitorAdaptor {

    private final IResourceMonitor<ReddalComponents> monitor;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Channel<SearchResult> searchResults;
    private final Channel<StockAlert> stockAlertChannel;
    private final Channel<StackRefPriceDetail> stackRefPriceDetails;

    private final Map<String, IBook<IBookLevel>> books;
    private final HashMultimap<String, MDForSymbol> listeners;

    private final SimpleDateFormat sdf;
    private final long timezoneOffsetMillis;
    private final HashMap<String, TradeTracker> tradeTrackers = new HashMap<>();

    public LevelTwoBookSubscriber(final IResourceMonitor<ReddalComponents> monitor, final Channel<SearchResult> searchResults,
            final Channel<StockAlert> stockAlertChannel, final Channel<StackRefPriceDetail> stackRefPriceDetails) {

        this.monitor = monitor;
        this.searchResults = searchResults;
        this.stockAlertChannel = stockAlertChannel;
        this.stackRefPriceDetails = stackRefPriceDetails;

        this.mdClients = new EnumMap<>(MDSource.class);

        this.books = new HashMap<>();
        this.listeners = HashMultimap.create();

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.timezoneOffsetMillis = DateTimeUtil.LONDON_TIME_ZONE.getOffset(System.currentTimeMillis());
    }

    public void setMDClient(final MDSource mdSource, final MDTransportClient client) {

        if (null != mdClients.put(mdSource, client)) {
            monitor.logError(ReddalComponents.MD_L2_HANDLER, "Duplicate client for MDSource [" + mdSource + "].");
        }
    }

    @Override
    public void bookCreated(final IBook<IBookLevel> book) {
        books.put(book.getSymbol(), book);
        for (final MDForSymbol listener : listeners.get(book.getSymbol())) {
            bookSubscribe(listener, book);
        }
        final SearchResult searchResult = new SearchResult(book);
        searchResults.publish(searchResult);
    }

    @Override
    public void referencePrice(final IBook<IBookLevel> book, final IBookReferencePrice refPrice) {

        if (refPrice.isValid()) {
            switch (refPrice.getReferencePoint()) {
                case RFQ: {
                    final long milliSinceMidnight = refPrice.getReceivedNanoSinceMidnight() / DateTimeUtil.NANOS_IN_MILLIS;
                    final String timestamp = sdf.format(timezoneOffsetMillis + milliSinceMidnight);
                    final StockAlert stockAlert =
                            new StockAlert(milliSinceMidnight, timestamp, "RFQ", book.getSymbol(), "Qty: " + refPrice.getQty());
                    stockAlertChannel.publish(stockAlert);
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

        final TradeTracker tradeTracker;
        if (!tradeTrackers.containsKey(book.getSymbol())) {
            tradeTracker = new TradeTracker();
            tradeTrackers.put(book.getSymbol(), tradeTracker);
        } else {
            tradeTracker = tradeTrackers.get(book.getSymbol());
        }
        listener.setTradeTracker(tradeTracker);

        final MDTransportClient client = mdClients.get(book.getSourceExch());
        client.subscribeToInst(book.getLocalID());
        monitor.setOK(ReddalComponents.MD_L2_HANDLER);
    }

    public void unsubscribeForMD(final String symbol, final MDForSymbol listener) {
        listeners.remove(symbol, listener);
        if (!listeners.containsKey(symbol)) {
            final IBook<IBookLevel> book = books.get(symbol);
            if (null != book) {
                final MDTransportClient client = mdClients.get(book.getSourceExch());
                client.unsubscribeToInst(book.getLocalID());
            }
        }
    }

    @Override
    public void trade(final IBook<IBookLevel> book, final long execID, final AggressorSide side, final long price, final long qty) {
        final TradeTracker tradeTracker = tradeTrackers.get(book.getSymbol());
        tradeTracker.addTrade(price, qty);
    }

    @Override
    public void logErrorMsg(final String msg) {
        monitor.logError(ReddalComponents.MD_L2_HANDLER, msg);
    }

    @Override
    public void logErrorMsg(final String msg, final Throwable t) {
        monitor.logError(ReddalComponents.MD_L2_HANDLER, msg, t);
    }
}
