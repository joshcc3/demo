package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelThreeMonitor;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelTwoMonitor;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import org.jetlang.core.Callback;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;

public class PullerBookSubscriber {

    private final HashMap<String, IBook<?>> knownBooks = new HashMap<>();
    private final HashMap<String, Callback<IBook<?>>> callbacks = new HashMap<>();
    private final EnumMap<MDSource, MDTransportClient> clientMap = new EnumMap<>(MDSource.class);
    private Callback<IBook<?>> createdCallback = message -> {
    };

    public void setCreatedCallback(final Callback<IBook<?>> createdCallback) {
        this.createdCallback = createdCallback;
    }

    public IBook<?> subscribeToSymbol(final String symbol, final Callback<IBook<?>> callback) {
        callbacks.put(symbol, callback);
        final IBook<?> book = knownBooks.get(symbol);
        if (null != book) {
            clientMap.get(book.getSourceExch()).subscribeToInst(book.getLocalID());
            callback.onMessage(book);
        }
        return book;
    }

    private void bookCreated(final IBook<?> book) {
        knownBooks.put(book.getSymbol(), book);
        final Callback<IBook<?>> callback = callbacks.get(book.getSymbol());
        if (null != callback) {
            clientMap.get(book.getSourceExch()).subscribeToInst(book.getLocalID());
            callback.onMessage(book);
        }
        createdCallback.onMessage(book);
    }

    private void bookChanged(final IBook<?> book) {
        final Callback<IBook<?>> iBookCallback = callbacks.get(book.getSymbol());
        if (null != iBookCallback) {
            iBookCallback.onMessage(book);
        }
    }

    public void setClient(final MDSource source, final MDTransportClient client) {
        clientMap.put(source, client);
    }

    public IBookLevelTwoMonitor getL2() {
        return new IBookLevelTwoMonitor() {

            HashSet<IBook<?>> changed = new HashSet<>();

            @Override
            public void addLevel(final IBook<IBookLevel> book, final IBookLevel level) {
                changed.add(book);
            }

            @Override
            public void modifyLevel(final IBook<IBookLevel> book, final IBookLevel level, final long oldQty) {
                changed.add(book);

            }

            @Override
            public void modifyLevel(final IBook<IBookLevel> book, final IBookLevel level, final boolean wasTopOfBook, final long oldPrice,
                    final long oldQty) {
                changed.add(book);

            }

            @Override
            public void deleteLevel(final IBook<IBookLevel> book, final IBookLevel level) {
                changed.add(book);

            }

            @Override
            public void bookCreated(final IBook<IBookLevel> book) {
                PullerBookSubscriber.this.bookCreated(book);
            }

            @Override
            public void clearBook(final IBook<IBookLevel> book) {
                changed.add(book);

            }

            @Override
            public void impliedQty(final IBook<IBookLevel> book, final IBookLevel level) {
                changed.add(book);

            }

            @Override
            public void trade(final IBook<IBookLevel> book, final long execID, final AggressorSide side, final long price, final long qty) {
                changed.add(book);

            }

            @Override
            public void statusUpdate(final IBook<IBookLevel> book) {
                changed.add(book);

            }

            @Override
            public void referencePrice(final IBook<IBookLevel> book, final IBookReferencePrice referencePriceData) {

            }

            @Override
            public void bookValidated(final IBook<IBookLevel> book) {
                changed.add(book);

            }

            @Override
            public void logErrorMsg(final String msg) {

            }

            @Override
            public void logErrorMsg(final String msg, final Throwable t) {

            }

            @Override
            public void batchComplete() {
                changed.forEach(PullerBookSubscriber.this::bookChanged);
                changed.clear();
            }
        };
    }

    public IBookLevelThreeMonitor getL3() {
        return new IBookLevelThreeMonitor() {

            @Override
            public void addOrder(final IBook<IBookLevelWithOrders> book, final IBookOrder order) {
                changed.add(book);

            }

            @Override
            public void modifyOrder(final IBook<IBookLevelWithOrders> book, final IBookLevelWithOrders oldLevel, final IBookOrder order) {
                changed.add(book);

            }

            @Override
            public void deleteOrder(final IBook<IBookLevelWithOrders> book, final IBookOrder order) {
                changed.add(book);

            }

            HashSet<IBook<?>> changed = new HashSet<>();

            @Override
            public void bookCreated(final IBook<IBookLevelWithOrders> book) {
                PullerBookSubscriber.this.bookCreated(book);

            }

            @Override
            public void clearBook(final IBook<IBookLevelWithOrders> book) {
                changed.add(book);

            }

            @Override
            public void impliedQty(final IBook<IBookLevelWithOrders> book, final IBookLevelWithOrders level) {
                changed.add(book);

            }

            @Override
            public void trade(final IBook<IBookLevelWithOrders> book, final long execID, final AggressorSide side, final long price,
                    final long qty) {

            }

            @Override
            public void statusUpdate(final IBook<IBookLevelWithOrders> book) {
                changed.add(book);

            }

            @Override
            public void referencePrice(final IBook<IBookLevelWithOrders> book, final IBookReferencePrice referencePriceData) {

            }

            @Override
            public void bookValidated(final IBook<IBookLevelWithOrders> book) {
                changed.add(book);

            }

            @Override
            public void logErrorMsg(final String msg) {

            }

            @Override
            public void logErrorMsg(final String msg, final Throwable t) {

            }

            @Override
            public void batchComplete() {
                changed.forEach(PullerBookSubscriber.this::bookChanged);
                changed.clear();
            }
        };
    }
}
