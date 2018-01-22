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
import com.drwtrading.london.reddal.stacks.opxl.StackRefPriceDetail;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Channel;

import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelTwoBookSubscriber extends BookLevelTwoMonitorAdaptor {

    private final IResourceMonitor<ReddalComponents> monitor;

    private final Channel<SearchResult> searchResults;
    private final Channel<StockAlert> stockAlertChannel;
    private final Channel<StackRefPriceDetail> stackRefPriceDetails;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Map<String, IBook<IBookLevel>> books;
    private final Map<String, MDForSymbol> mdForSymbols;

    private final SimpleDateFormat sdf;
    private final long timezoneOffsetMillis;

    public LevelTwoBookSubscriber(final IResourceMonitor<ReddalComponents> monitor, final Channel<SearchResult> searchResults,
            final Channel<StockAlert> stockAlertChannel, final Channel<StackRefPriceDetail> stackRefPriceDetails) {

        this.monitor = monitor;
        this.searchResults = searchResults;
        this.stockAlertChannel = stockAlertChannel;
        this.stackRefPriceDetails = stackRefPriceDetails;

        this.mdClients = new EnumMap<>(MDSource.class);
        this.books = new HashMap<>();
        this.mdForSymbols = new HashMap<>();

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
        final MDForSymbol mdForSymbol = mdForSymbols.get(book.getSymbol());
        if (null != mdForSymbol) {
            bookSubscribe(mdForSymbol, book);
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

    public void subscribeForMD(final MDForSymbol mdForSymbol) {

        final MDForSymbol prevMDForSymbol = mdForSymbols.putIfAbsent(mdForSymbol.symbol, mdForSymbol);

        if (null == prevMDForSymbol) {
            final IBook<?> book = books.get(mdForSymbol.symbol);
            if (null != book) {
                bookSubscribe(mdForSymbol, book);
            }
        } else {
            throw new IllegalArgumentException(
                    "Should not be subscribing more than one MDForSymbol to symbol [" + mdForSymbol.symbol + "].");
        }
    }

    private void bookSubscribe(final MDForSymbol mdForSymbol, final IBook<?> book) {

        mdForSymbol.setBook(book);

        final MDTransportClient client = mdClients.get(book.getSourceExch());
        client.subscribeToInst(book.getLocalID());
        monitor.setOK(ReddalComponents.MD_L2_HANDLER);
    }

    public void unsubscribeForMD(final MDForSymbol mdForSymbol) {

        if (null != mdForSymbols.remove(mdForSymbol.symbol)) {
            final IBook<IBookLevel> book = books.get(mdForSymbol.symbol);
            if (null != book) {
                final MDTransportClient client = mdClients.get(book.getSourceExch());
                client.unsubscribeToInst(book.getLocalID());
                mdForSymbol.unsubscribed();
            }
        }
    }

    @Override
    public void trade(final IBook<IBookLevel> book, final long execID, final AggressorSide side, final long price, final long qty) {

        final MDForSymbol mdForSymbol = mdForSymbols.get(book.getSymbol());
        if (null != mdForSymbol) {
            mdForSymbol.trade(price, qty);
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
