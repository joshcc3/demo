package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelTwoMonitor;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.transport.IMDSubscriber;
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

public class LevelTwoBookSubscriber implements IBookLevelTwoMonitor {

    private final IFuseBox<ReddalComponents> monitor;

    private final Channel<SearchResult> searchResults;
    private final Channel<SymbolReferencePrice> symbolRefPrices;
    private final Publisher<RfqAlert> stockAlertChannel;

    private final Map<MDSource, IMDSubscriber> mdClients;
    private final Map<String, IBook<IBookLevel>> books;
    private final Map<String, MDForSymbol> mdForSymbols;

    private final LongMap<IBook<?>> dirtyBooks;
    private final LongMap<MDForSymbol> mdCallbacks;

    public LevelTwoBookSubscriber(final IFuseBox<ReddalComponents> monitor, final Channel<SearchResult> searchResults,
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

    public void setMDClient(final MDSource mdSource, final IMDSubscriber client) {

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
            if (mdForSymbol.isListeningForUpdates()) {
                mdCallbacks.put(book.getLocalID(), mdForSymbol);
            }
        }
        final SearchResult searchResult = new SearchResult(book);
        searchResults.publish(searchResult);
    }

    @Override
    public void referencePrice(final IBook<IBookLevel> book, final IBookReferencePrice refPrice) {

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
                    final long refPriceReceivedNanos = refPrice.getReceivedNanoSinceMidnight();
                    final long refPriceQty = refPrice.getQty();

                    final IBookReferencePrice yestClose = book.getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
                    final long yestCloseValue = yestClose.getPrice();

                    final boolean yestCloseIsValid = yestClose.isValid();

                    final long milliSinceMidnight = refPriceReceivedNanos / DateTimeUtil.NANOS_IN_MILLIS;
                    final boolean isETF = book.getInstType() == InstType.ETF;

                    final long price;
                    final IBookLevel bestBid = book.getBestBid();
                    final IBookLevel bestAsk = book.getBestAsk();
                    if (null != bestAsk && null != bestBid) {
                        price = (bestBid.getPrice() >> 1) + (bestAsk.getPrice() >> 1);
                    } else if (yestCloseIsValid) {
                        price = yestCloseValue;
                    } else {
                        price = 0;
                    }

                    final RfqAlert rfqAlert = new RfqAlert(milliSinceMidnight, book.getSymbol(), price, refPriceQty, book.getCCY(), isETF);
                    stockAlertChannel.publish(rfqAlert);
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

        final IMDSubscriber client = mdClients.get(book.getSourceExch());
        client.subscribeToInst(book.getLocalID());
        monitor.setOK(ReddalComponents.MD_L2_HANDLER);
    }

    public void unsubscribeForMD(final MDForSymbol mdForSymbol) {

        if (null != mdForSymbols.remove(mdForSymbol.symbol)) {
            final IBook<IBookLevel> book = books.get(mdForSymbol.symbol);
            if (null != book) {
                final IMDSubscriber client = mdClients.get(book.getSourceExch());
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
    public void bookValidated(final IBook<IBookLevel> book) {

    }

    @Override
    public void statusUpdate(final IBook<IBookLevel> book) {

    }

    @Override
    public void clearBook(final IBook<IBookLevel> book) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void impliedQty(final IBook<IBookLevel> book, final IBookLevel level) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void addLevel(final IBook<IBookLevel> book, final IBookLevel level) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void modifyLevel(final IBook<IBookLevel> book, final IBookLevel level, final long oldQty) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void modifyLevel(final IBook<IBookLevel> book, final IBookLevel level, final boolean wasTopOfBook, final long oldPrice,
            final long oldQty) {

        dirtyBooks.put(book.getLocalID(), book);
    }

    @Override
    public void deleteLevel(final IBook<IBookLevel> book, final IBookLevel level) {

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
        monitor.logError(ReddalComponents.MD_L2_HANDLER, msg);
    }

    @Override
    public void logErrorMsg(final String msg, final Throwable t) {
        monitor.logError(ReddalComponents.MD_L2_HANDLER, msg, t);
    }
}
