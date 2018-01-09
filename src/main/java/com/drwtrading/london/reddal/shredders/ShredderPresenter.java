package com.drwtrading.london.reddal.shredders;

import com.drwtrading.jetlang.autosubscribe.KeyedBatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.ladders.LaserLineStringConverter;
import com.drwtrading.london.reddal.safety.ServerTradingStatus;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersPresenter;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShredderPresenter {

    private static final long BATCH_FLUSH_INTERVAL_MS = 1000 / 5;
    private static final long HEARTBEAT_INTERVAL_MS = 1000;

    private final Map<Publisher<WebSocketOutboundData>, ShredderView> viewsBySocket = new HashMap<>();
    private final Multimap<String, ShredderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, ShredderView> viewsByUser = HashMultimap.create();
    private final IMDSubscriber bookSubscriber;
    private final Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(WorkingOrdersForSymbol::new);
    private final Map<String, ExtraDataForSymbol> dataBySymbol = new MapMaker().makeComputingMap(ExtraDataForSymbol::new);

    public ShredderPresenter(final IMDSubscriber depthBookSubscriber) {
        this.bookSubscriber = depthBookSubscriber;
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        final IShredderUI view = new WebSocketOutputDispatcher<>(IShredderUI.class).wrap(msg -> uiPipe.eval(msg.getData()));
        final ShredderView shredderView = new ShredderView(view, uiPipe);

        viewsBySocket.put(connected.getOutboundChannel(), shredderView);
        viewsByUser.put(connected.getClient().getUserName(), shredderView);
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        final ShredderView view = viewsBySocket.remove(disconnected.getOutboundChannel());
        if (view != null && view.symbol != null) {
            final String symbol = view.symbol;
            viewsBySymbol.remove(symbol, view);
            final String user = disconnected.getClient().getUserName();
            viewsByUser.remove(user, view);
            if (viewsBySymbol.get(symbol).isEmpty()) {
                bookSubscriber.unsubscribeForMD(symbol, this);
            }
        }
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        final String data = msg.getData();
        final String[] args = data.split("\0");
        final String cmd = args[0];
        final ShredderView view = viewsBySocket.get(msg.getOutboundChannel());

        if (null != view) {
            if ("shredder-subscribe".equals(cmd)) {
                final String symbol = args[1];
                final int levels = Integer.parseInt(args[2]);
                final MDForSymbol mdForSymbol = bookSubscriber.subscribeForMD(symbol, this);

                view.subscribeToSymbol(symbol, levels, mdForSymbol, ordersBySymbol.get(symbol), dataBySymbol.get(symbol));

                viewsBySymbol.put(symbol, view);
            } else {
                view.onRawInboundData(data);
                view.refreshAndFlush();
            }
        }
    }

    @Subscribe
    public void on(final ServerTradingStatus serverTradingStatus) {
        if (!serverTradingStatus.isWorkingOrderConnected) {
            for (final WorkingOrdersForSymbol ordersForSymbol : ordersBySymbol.values()) {
                for (final Iterator<WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByKey.values().iterator();
                     iter.hasNext(); ) {
                    final WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
                for (final Iterator<WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByPrice.values().iterator();
                     iter.hasNext(); ) {
                    final WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    @KeyedBatchSubscriber(converter = WorkingOrdersPresenter.WOConverter.class, flushInterval = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Subscribe
    public void onWorkingOrders(final Map<String, WorkingOrderUpdateFromServer> workingOrderUpdates) {
        for (WorkingOrderUpdateFromServer workingOrderUpdate : workingOrderUpdates.values()) {
            ordersBySymbol.get(workingOrderUpdate.workingOrderUpdate.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
        }
    }

    @KeyedBatchSubscriber(converter = LaserLineStringConverter.class, flushInterval = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Subscribe
    public void onLaserLines(final Map<String, LaserLine> laserLines) {
        for (final LaserLine laserLine : laserLines.values()) {
            dataBySymbol.get(laserLine.getSymbol()).onLaserLine(laserLine);
        }
    }

    public void setLaserLine(final com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine laserLine) {
        final ExtraDataForSymbol data = dataBySymbol.get(laserLine.getSymbol());
        data.setLaserLine(laserLine);
    }

    public void setTheo(final TheoValue theoValue) {

        final ExtraDataForSymbol data = dataBySymbol.get(theoValue.getSymbol());
        data.setTheoValue(theoValue);
    }

    public long flushAllShredders() {
        for (final ShredderView shredderView : viewsBySocket.values()) {
            shredderView.timedRefresh();
        }
        return BATCH_FLUSH_INTERVAL_MS;
    }

    public long sendAllHeartbeats() {
        for (final ShredderView shredderView : viewsBySocket.values()) {
            shredderView.sendHeartbeat();
        }
        return HEARTBEAT_INTERVAL_MS;
    }
}
