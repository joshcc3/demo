package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelTwoMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Channel;

import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelTwoBookSubscriber extends BookLevelTwoMonitorAdaptor {

    private final boolean isPrimary;

    private final IResourceMonitor<ReddalComponents> monitor;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Channel<SearchResult> searchResults;
    private final Channel<StockAlert> stockAlertChannel;

    private final Map<String, IBook<IBookLevel>> books;
    private final Map<String, MDForSymbol> listeners;

    private final SimpleDateFormat sdf;

    public LevelTwoBookSubscriber(final boolean isPrimary, final IResourceMonitor<ReddalComponents> monitor,
            final Channel<SearchResult> searchResults, final Channel<StockAlert> stockAlertChannel) {

        this.isPrimary = isPrimary;

        this.monitor = monitor;
        this.searchResults = searchResults;
        this.stockAlertChannel = stockAlertChannel;

        this.mdClients = new EnumMap<>(MDSource.class);

        this.books = new HashMap<>();
        this.listeners = new HashMap<>();

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
    }

    public void setMDClient(final MDSource mdSource, final MDTransportClient client) {

        if (null != mdClients.put(mdSource, client)) {
            monitor.logError(ReddalComponents.MD_L2_HANDLER, "Duplicate client for MDSource [" + mdSource + "].");
        }
    }

    @Override
    public void bookCreated(final IBook<IBookLevel> book) {

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
    public void referencePrice(final IBook<IBookLevel> book, final IBookReferencePrice referencePriceData) {

        if (isPrimary && referencePriceData.isValid() && ReferencePoint.RFQ == referencePriceData.getReferencePoint() ) {
            final String timestamp = sdf.format(referencePriceData.getReceivedNanoSinceMidnight() / DateTimeUtil.NANOS_IN_MILLIS);
            final StockAlert stockAlert = new StockAlert(timestamp, "RFQ", book.getSymbol(), "Qty: " + referencePriceData.getQty());
            stockAlertChannel.publish(stockAlert);
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
        monitor.setOK(ReddalComponents.MD_L2_HANDLER);
    }

    public void unsubscribeForMD(final String symbol) {

        listeners.remove(symbol);
        final IBook<IBookLevel> book = books.get(symbol);
        if (null != book) {
            final MDTransportClient client = mdClients.get(book.getSourceExch());
            client.unsubscribeToInst(book.getLocalID());
        }

    }

    @Override
    public void trade(final IBook<IBookLevel> book, final long execID, final AggressorSide side, final long price, final long qty) {

        final MDForSymbol listener = listeners.get(book.getSymbol());
        if (null != listener) {
            listener.trade(price, qty);
        }
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
