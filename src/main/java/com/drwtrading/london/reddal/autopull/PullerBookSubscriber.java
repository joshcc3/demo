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

    final HashMap<String, IBook<?>> knownBooks = new HashMap<>();
    final HashMap<String, Callback<IBook<?>>> callbacks = new HashMap<>();
    final EnumMap<MDSource, MDTransportClient> clientMap = new EnumMap<MDSource, MDTransportClient>(MDSource.class);
    Callback<IBook<?>> createdCallback = message -> {
    };

    public void setCreatedCallback(Callback<IBook<?>> createdCallback) {
        this.createdCallback = createdCallback;
    }

    public IBook<?> subscribeToSymbol(String symbol, Callback<IBook<?>> callback) {
        callbacks.put(symbol, callback);
        IBook<?> book = knownBooks.get(symbol);
        if (null != book) {
            clientMap.get(book.getSourceExch()).subscribeToInst(book.getLocalID());
            callback.onMessage(book);
        }
        return book;
    }

    public void bookCreated(IBook<?> book) {
        knownBooks.put(book.getSymbol(), book);
        Callback<IBook<?>> callback = callbacks.get(book.getSymbol());
        if (null != callback) {
            clientMap.get(book.getSourceExch()).subscribeToInst(book.getLocalID());
            callback.onMessage(book);
        }
        createdCallback.onMessage(book);
    }

    public void bookChanged(IBook<?> book) {
        Callback<IBook<?>> iBookCallback = callbacks.get(book.getSymbol());
        if (null != iBookCallback) {
            iBookCallback.onMessage(book);
        }
    }

    public void setClient(MDSource source, MDTransportClient client) {
        clientMap.put(source, client);
    }

    public IBookLevelTwoMonitor getL2() {
        return new IBookLevelTwoMonitor() {

            HashSet<IBook<?>> changed = new HashSet<>();

            @Override
            public void addLevel(IBook<IBookLevel> book, IBookLevel level) {
                changed.add(book);
            }

            @Override
            public void modifyLevel(IBook<IBookLevel> book, IBookLevel level, long oldQty) {
                changed.add(book);

            }

            @Override
            public void modifyLevel(IBook<IBookLevel> book, IBookLevel level, boolean wasTopOfBook, long oldPrice, long oldQty) {
                changed.add(book);

            }

            @Override
            public void deleteLevel(IBook<IBookLevel> book, IBookLevel level) {
                changed.add(book);

            }

            @Override
            public void bookCreated(IBook<IBookLevel> book) {
                PullerBookSubscriber.this.bookCreated(book);
            }

            @Override
            public void clearBook(IBook<IBookLevel> book) {
                changed.add(book);

            }

            @Override
            public void impliedQty(IBook<IBookLevel> book, IBookLevel level) {
                changed.add(book);

            }

            @Override
            public void trade(IBook<IBookLevel> book, long execID, AggressorSide side, long price, long qty) {
                changed.add(book);

            }

            @Override
            public void statusUpdate(IBook<IBookLevel> book) {
                changed.add(book);

            }

            @Override
            public void referencePrice(IBook<IBookLevel> book, IBookReferencePrice referencePriceData) {

            }

            @Override
            public void bookValidated(IBook<IBookLevel> book) {
                changed.add(book);

            }

            @Override
            public void logErrorMsg(String msg) {

            }

            @Override
            public void logErrorMsg(String msg, Throwable t) {

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
            public void addOrder(IBook<IBookLevelWithOrders> book, IBookOrder order) {
                changed.add(book);

            }

            @Override
            public void modifyOrder(IBook<IBookLevelWithOrders> book, IBookLevelWithOrders oldLevel, IBookOrder order) {
                changed.add(book);

            }

            @Override
            public void deleteOrder(IBook<IBookLevelWithOrders> book, IBookOrder order) {
                changed.add(book);

            }

            HashSet<IBook<?>> changed = new HashSet<>();


            @Override
            public void bookCreated(IBook<IBookLevelWithOrders> book) {
                PullerBookSubscriber.this.bookCreated(book);

            }

            @Override
            public void clearBook(IBook<IBookLevelWithOrders> book) {
                changed.add(book);

            }

            @Override
            public void impliedQty(IBook<IBookLevelWithOrders> book, IBookLevelWithOrders level) {
                changed.add(book);

            }

            @Override
            public void trade(IBook<IBookLevelWithOrders> book, long execID, AggressorSide side, long price, long qty) {

            }

            @Override
            public void statusUpdate(IBook<IBookLevelWithOrders> book) {
                changed.add(book);

            }

            @Override
            public void referencePrice(IBook<IBookLevelWithOrders> book, IBookReferencePrice referencePriceData) {

            }

            @Override
            public void bookValidated(IBook<IBookLevelWithOrders> book) {
                changed.add(book);

            }

            @Override
            public void logErrorMsg(String msg) {

            }

            @Override
            public void logErrorMsg(String msg, Throwable t) {

            }

            @Override
            public void batchComplete() {
                changed.forEach(PullerBookSubscriber.this::bookChanged);
                changed.clear();
            }
        };
    }


}
