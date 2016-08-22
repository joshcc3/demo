package com.drwtrading.london.reddal.stockAlerts;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.util.LinkedList;
import java.util.Queue;

public class StockAlertPresenter {

    private static final int MAX_HISTORY = 15;

    private final UILogger webLog;

    private final WebSocketViews<IStockAlertsView> views;
    private final Queue<StockAlert> alerts;

    public StockAlertPresenter(final UILogger webLog) {

        this.webLog = webLog;

        this.views = WebSocketViews.create(IStockAlertsView.class, this);
        this.alerts = new LinkedList<>();
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final IStockAlertsView view = views.register(connected);
        for (final StockAlert update : alerts) {
            view.stockAlert(update.timestamp, update.type, update.symbol, update.msg, false);
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        webLog.write("stockAlerts", msg);
    }

    public void addAlert(final StockAlert stockAlert) {

        alerts.add(stockAlert);
        views.all().stockAlert(stockAlert.timestamp, stockAlert.type, stockAlert.symbol, stockAlert.msg, true);

        if (MAX_HISTORY < alerts.size()) {
            alerts.poll();
        }
    }
}
