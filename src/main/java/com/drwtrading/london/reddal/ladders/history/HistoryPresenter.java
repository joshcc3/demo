package com.drwtrading.london.reddal.ladders.history;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

public class HistoryPresenter {

    private static final int MAX_QUEUE_SIZE = 20;

    private final UILogger webLog;

    private final WebSocketViews<IHistoryView> views;

    private final Map<String, IHistoryView> userViews;
    private final Map<String, LinkedHashSet<String>> userHistory;

    public HistoryPresenter(final UILogger webLog) {

        this.webLog = webLog;

        this.views = new WebSocketViews<>(IHistoryView.class, this);

        this.userViews = new HashMap<>();
        this.userHistory = new HashMap<>();
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {

        final IHistoryView view = views.register(connected);
        final String username = connected.getClient().getUserName();

        userViews.put(username, view);

        final LinkedHashSet<String> previousSymbols = MapUtils.getMappedLinkedSet(userHistory, username);
        for (final String symbol : previousSymbols) {
            view.addSymbol(symbol);
        }
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {

        final String username = disconnected.getClient().getUserName();
        userViews.remove(username);
    }

    @Subscribe
    public void on(final WebSocketInboundData data) {
        webLog.write("historyPresenter", data);
        views.invoke(data);
    }

    public void addSymbol(final String username, final String[] subscriptionArgs) {

        final IHistoryView view = userViews.get(username);

        final LinkedHashSet<String> previousSymbols = MapUtils.getMappedLinkedSet(userHistory, username);

        final String symbol;
        if (3 < subscriptionArgs.length && "S".equals(subscriptionArgs[3])) {
            symbol = subscriptionArgs[1] + ";S";
        } else {
            symbol = subscriptionArgs[1];
        }

        previousSymbols.remove(symbol);
        previousSymbols.add(symbol);

        if (null != view) {
            view.addSymbol(symbol);
        }

        if (MAX_QUEUE_SIZE < previousSymbols.size()) {

            final Iterator<String> historyIterator = previousSymbols.iterator();
            final String oldestSymbol = historyIterator.next();
            historyIterator.remove();

            if (null != view) {
                view.removeSymbol(oldestSymbol);
            }
        }
    }
}
