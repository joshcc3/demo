package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelThreeMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.SelectIOMDForSymbol;

import java.util.HashMap;
import java.util.Map;

public class LevelThreeBookHandler extends BookLevelThreeMonitorAdaptor {

    private final IResourceMonitor<ReddalComponents> monitor;

    private final LongMap<IBook<IBookLevelWithOrders>> batch;
    private final Map<String, IBook<IBookLevelWithOrders>> books;
    private final Map<String, SelectIOMDForSymbol> listeners;

    public LevelThreeBookHandler(final IResourceMonitor<ReddalComponents> monitor) {

        this.monitor = monitor;

        this.batch = new LongMap<>();
        this.books = new HashMap<>();
        this.listeners = new HashMap<>();
    }

    public void subscribe(final String symbol, final SelectIOMDForSymbol listener) {

        listeners.put(symbol, listener);
        final IBook<IBookLevelWithOrders> book = books.get(symbol);
        if (null != book) {
            listener.setBook(book);
        }
    }

    @Override
    public void bookCreated(final IBook<IBookLevelWithOrders> book) {

        books.put(book.getSymbol(), book);
        final SelectIOMDForSymbol listener = listeners.get(book.getSymbol());
        if (null != listener) {
            listener.setBook(book);
        }
    }

    @Override
    public void clearBook(final IBook<IBookLevelWithOrders> book) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void impliedQty(final IBook<IBookLevelWithOrders> book, final IBookLevelWithOrders level) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void addOrder(final IBook<IBookLevelWithOrders> book, final IBookOrder addOrder) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void modifyOrder(final IBook<IBookLevelWithOrders> book, final IBookLevelWithOrders oldLevel, final IBookOrder order) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void deleteOrder(final IBook<IBookLevelWithOrders> book, final IBookOrder order) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void statusUpdate(final IBook<IBookLevelWithOrders> book) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void bookValidated(final IBook<IBookLevelWithOrders> book) {
        batch.put(book.getLocalID(), book);
    }

    @Override
    public void trade(final IBook<IBookLevelWithOrders> book, final long execID, final AggressorSide side, final long price,
            final long qty) {

        // no-op
    }

    @Override
    public void referencePrice(final IBook<IBookLevelWithOrders> book, final IBookReferencePrice referencePriceData) {
        // no-op
    }

    @Override
    public void logErrorMsg(final String msg) {
        monitor.logError(ReddalComponents.MD_L3_HANDLER, msg);
    }

    @Override
    public void logErrorMsg(final String msg, final Throwable t) {
        monitor.logError(ReddalComponents.MD_L3_HANDLER, msg, t);
    }

    @Override
    public void batchComplete() {
        try {
            for (final LongMapNode<IBook<IBookLevelWithOrders>> bookNode : batch) {
                final IBook<IBookLevelWithOrders> book = bookNode.getValue();
                final SelectIOMDForSymbol listener = listeners.get(book.getSymbol());
                if (null != listener) {
                    listener.bookUpdated();
                }
            }
            monitor.setOK(ReddalComponents.MD_L3_HANDLER);
        } catch (final Exception e) {
            monitor.logError(ReddalComponents.MD_L3_HANDLER, "Failed to complete batch.", e);
        } finally {
            batch.clear();
        }
    }
}
