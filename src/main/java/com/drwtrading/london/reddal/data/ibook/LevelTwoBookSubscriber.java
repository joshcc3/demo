package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelTwoMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.SelectIOMDForSymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Channel;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LevelTwoBookSubscriber extends BookLevelTwoMonitorAdaptor implements IBookSubscriber {

    private final IResourceMonitor<ReddalComponents> monitor;

    private final Map<MDSource, MDTransportClient> mdClients;
    private final Channel<SearchResult> searchResults;

    private final Map<String, IBook<IBookLevel>> books;
    private final Map<String, SelectIOMDForSymbol> listeners;

    public LevelTwoBookSubscriber(final IResourceMonitor<ReddalComponents> monitor, final Channel<SearchResult> searchResults) {

        this.monitor = monitor;
        this.searchResults = searchResults;

        this.mdClients = new EnumMap<>(MDSource.class);

        this.books = new HashMap<>();
        this.listeners = new HashMap<>();
    }

    public void setMDClient(final MDSource mdSource, final MDTransportClient client) {

        if (null != mdClients.put(mdSource, client)) {
            monitor.logError(ReddalComponents.MD_L2_HANDLER, "Duplicate client for MDSource [" + mdSource + "].");
        }
    }

    @Override
    public void bookCreated(final IBook<IBookLevel> book) {

        books.put(book.getSymbol(), book);
        final SelectIOMDForSymbol listener = listeners.get(book.getSymbol());
        if (null != listener) {
            bookSubscribe(listener, book);
        }
        final SearchResult searchResult = new SearchResult(book);
        searchResults.publish(searchResult);
    }

    @Override
    public void subscribeForMD(final String symbol, final SelectIOMDForSymbol listener) {

        listeners.put(symbol, listener);
        final IBook<?> book = books.get(symbol);
        if (null != book) {
            bookSubscribe(listener, book);
        }
    }

    private void bookSubscribe(final SelectIOMDForSymbol listener, final IBook<?> book) {

        listener.setBook(book);
        final MDTransportClient client = mdClients.get(book.getSourceExch());
        client.subscribeToInst(book.getLocalID());
        monitor.setOK(ReddalComponents.MD_L2_HANDLER);
    }

    @Override
    public void unsubscribeForMD(final String symbol) {

        listeners.remove(symbol);
        final IBook<IBookLevel> book = books.get(symbol);
        if (null != book) {
            final MDTransportClient client = mdClients.get(book.getSourceExch());
            client.unsubscribeToInst(book.getLocalID());
        }

    }

    @Override
    public void trade(final IBook<IBookLevel> book, final long execID, final AggressorSide side, final long price,
            final long qty) {

        final SelectIOMDForSymbol listener = listeners.get(book.getSymbol());
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
