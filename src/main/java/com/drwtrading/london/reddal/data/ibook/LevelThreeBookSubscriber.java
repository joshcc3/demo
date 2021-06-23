package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelThreeMonitor;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.stockAlerts.RfqAlert;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.symbols.SymbolReferencePrice;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelThreeBookSubscriber implements IBookLevelThreeMonitor {

    private final IFuseBox<ReddalComponents> monitor;

    private final Channel<SearchResult> searchResults;
    private final Channel<SymbolReferencePrice> symbolRefPrices;
    private final Publisher<RfqAlert> stockAlertChannel;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Map<String, IBook<IBookLevelWithOrders>> books;
    private final Map<String, MDForSymbol> mdForSymbols;

    private final LongMap<IBook<?>> dirtyBooks;
    private final LongMap<MDForSymbol> mdCallbacks;

    public LevelThreeBookSubscriber(final IFuseBox<ReddalComponents> monitor, final Channel<SearchResult> searchResults,
            final Channel<SymbolReferencePrice> symbolRefPrices, final Publisher<RfqAlert> stockAlertChannel) {

        this.monitor = monitor;
        this.searchResults = searchResults;
        this.symbolRefPrices = symbolRefPrices;
        this.stockAlertChannel = stockAlertChannel;

        this.mdClients = new EnumMap<>(MDSource.class);
        this.books = new HashMap<>();
        this.mdForSymbols = new HashMap<>();

        this.dirtyBooks = new LongMap<>();
        this.mdCallbacks = new LongMap<>();
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
            if (mdForSymbol.isListeningForUpdates()) {
                mdCallbacks.put(book.getLocalID(), mdForSymbol);
            }
        }
        final SearchResult searchResult = new SearchResult(book);
        searchResults.publish(searchResult);
    }

    @Override
    public void referencePrice(final IBook<IBookLevelWithOrders> book, final IBookReferencePrice refPrice) {

        final long refPriceValue = refPrice.getPrice();
        final ReferencePoint referencePoint = refPrice.getReferencePoint();
        final boolean refPriceIsValid = refPrice.isValid();

        if (refPriceIsValid) {
            switch (referencePoint) {
                case YESTERDAY_CLOSE: {
                    final SymbolReferencePrice symbolRefPrice = new SymbolReferencePrice(book, refPriceValue);
                    symbolRefPrices.publish(symbolRefPrice);
                    break;
                }
                case RFQ: {

                    if (InstType.ETF != book.getInstType()) {

                        final long refPriceReceivedNanos = refPrice.getReceivedNanoSinceMidnight();
                        final long milliSinceMidnight = refPriceReceivedNanos / DateTimeUtil.NANOS_IN_MILLIS;

                        final IBookReferencePrice yestClose = book.getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);

                        final IBookLevel bestBid = book.getBestBid();
                        final IBookLevel bestAsk = book.getBestAsk();

                        final long price;
                        if (null != bestAsk && null != bestBid) {

                            price = (bestBid.getPrice() >> 1) + (bestAsk.getPrice() >> 1);

                        } else if (yestClose.isValid()) {

                            price = yestClose.getPrice();

                        } else {

                            price = 0;
                        }

                        final long refPriceQty = refPrice.getQty();

                        final RfqAlert rfqAlert = new RfqAlert(milliSinceMidnight, book.getSymbol(), price, refPriceQty, book.getCCY());
                        stockAlertChannel.publish(rfqAlert);
                    }
                    break;
                }
            }
        }
    }

    void addUpdateCallback(final MDForSymbol mdForSymbol) {

        final IBook<?> book = books.get(mdForSymbol.symbol);
        if (null != book) {
            mdCallbacks.put(book.getLocalID(), mdForSymbol);
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
    public void bookValidated(final IBook<IBookLevelWithOrders> book) {

    }

    @Override
    public void statusUpdate(final IBook<IBookLevelWithOrders> book) {

    }

    @Override
    public void clearBook(final IBook<IBookLevelWithOrders> book) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void impliedQty(final IBook<IBookLevelWithOrders> book, final IBookLevelWithOrders level) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void addOrder(final IBook<IBookLevelWithOrders> book, final IBookOrder order) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void modifyOrder(final IBook<IBookLevelWithOrders> book, final IBookLevelWithOrders oldLevel, final IBookOrder order) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void deleteOrder(final IBook<IBookLevelWithOrders> book, final IBookOrder order) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void batchComplete() {

        for (final LongMapNode<IBook<?>> dirtyBook : dirtyBooks) {

            final IBook<?> book = dirtyBook.getValue();
            final MDForSymbol callback = mdCallbacks.get(book.getLocalID());
            if (null != callback) {
                callback.bookUpdated();
            }
        }
        dirtyBooks.clear();
    }

    void closed() {

        for (final MDForSymbol mdForSymbol : mdForSymbols.values()) {
            mdForSymbol.unsubscribed();
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
