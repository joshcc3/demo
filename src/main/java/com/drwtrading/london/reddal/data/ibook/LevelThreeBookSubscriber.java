package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelThreeMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.stockAlerts.RfqAlert;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelThreeBookSubscriber extends BookLevelThreeMonitorAdaptor {

    private final IResourceMonitor<ReddalComponents> monitor;

    private final Channel<SearchResult> searchResults;
    private final Publisher<RfqAlert> stockAlertChannel;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Map<String, IBook<IBookLevelWithOrders>> books;
    private final Map<String, MDForSymbol> mdForSymbols;


    public LevelThreeBookSubscriber(final IResourceMonitor<ReddalComponents> monitor, final Channel<SearchResult> searchResults,
            final Publisher<RfqAlert> stockAlertChannel) {

        this.monitor = monitor;
        this.searchResults = searchResults;
        this.stockAlertChannel = stockAlertChannel;

        this.mdClients = new EnumMap<>(MDSource.class);
        this.books = new HashMap<>();
        this.mdForSymbols = new HashMap<>();
    }

    public void setMDClient(final MDSource mdSource, final MDTransportClient client) {

        if (null != mdClients.put(mdSource, client)) {
            monitor.logError(ReddalComponents.MD_L3_HANDLER, "Duplicate client for MDSource [" + mdSource + "].");
        }
    }

    @Override
    public void bookCreated(final IBook<IBookLevelWithOrders> book) {

        books.put(book.getSymbol(), book);
        final MDForSymbol mdForSymbol = mdForSymbols.get(book.getSymbol());
        if (null != mdForSymbol) {
            bookSubscribe(mdForSymbol, book);
        }
        final SearchResult searchResult = new SearchResult(book);
        searchResults.publish(searchResult);
    }

    @Override
    public void referencePrice(final IBook<IBookLevelWithOrders> book, final IBookReferencePrice refPrice) {

        if (refPrice.isValid()) {
            switch (refPrice.getReferencePoint()) {
                case RFQ: {
                    final long milliSinceMidnight = refPrice.getReceivedNanoSinceMidnight() / DateTimeUtil.NANOS_IN_MILLIS;
                    final boolean isETF = book.getInstType() == InstType.ETF;

                    final IBookReferencePrice yestClose = book.getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
                    final long price;
                    if (refPrice.isValid() && yestClose.isValid()) {
                        price = yestClose.getPrice();
                    } else {
                        price = 0;
                    }

                    final RfqAlert rfqAlert =
                            new RfqAlert(milliSinceMidnight, book.getSymbol(), price, refPrice.getQty(), book.getCCY(), isETF);
                    stockAlertChannel.publish(rfqAlert);
                    break;
                }
            }
        }
    }

    void subscribeForMD(final MDForSymbol mdForSymbol) {

        final MDForSymbol prevMDForSymbol = mdForSymbols.putIfAbsent(mdForSymbol.symbol, mdForSymbol);

        if (null == prevMDForSymbol) {
            final IBook<IBookLevelWithOrders> book = books.get(mdForSymbol.symbol);
            if (null != book) {
                bookSubscribe(mdForSymbol, book);
            }
        } else {
            throw new IllegalArgumentException(
                    "Should not be subscribing more than one MDForSymbol to symbol [" + mdForSymbol.symbol + "].");
        }
    }

    private void bookSubscribe(final MDForSymbol mdForSymbol, final IBook<IBookLevelWithOrders> book) {

        mdForSymbol.setL3Book(book);

        final MDTransportClient client = mdClients.get(book.getSourceExch());
        client.subscribeToInst(book.getLocalID());
        monitor.setOK(ReddalComponents.MD_L3_HANDLER);
    }

    void unsubscribeForMD(final MDForSymbol mdForSymbol) {

        if (null != mdForSymbols.remove(mdForSymbol.symbol)) {

            final IBook<IBookLevelWithOrders> book = books.get(mdForSymbol.symbol);
            if (null != book) {
                final MDTransportClient client = mdClients.get(book.getSourceExch());
                client.unsubscribeToInst(book.getLocalID());
                mdForSymbol.unsubscribed();
            }
        }
    }

    @Override
    public void trade(final IBook<IBookLevelWithOrders> book, final long execID, final AggressorSide side, final long price,
            final long qty) {

        final MDForSymbol mdForSymbol = mdForSymbols.get(book.getSymbol());
        if (null != mdForSymbol) {
            mdForSymbol.trade(price, qty);
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
