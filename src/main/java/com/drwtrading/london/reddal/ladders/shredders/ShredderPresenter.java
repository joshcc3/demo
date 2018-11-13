package com.drwtrading.london.reddal.ladders.shredders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.stacks.IStackPresenterCallback;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByID;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class ShredderPresenter implements IStackPresenterCallback {

    private static final long BATCH_FLUSH_INTERVAL_MS = 1000 / 5;
    private static final long HEARTBEAT_INTERVAL_MS = 1000;

    private final Map<Publisher<WebSocketOutboundData>, ShredderView> viewsBySocket = new HashMap<>();
    private final Multimap<String, ShredderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, ShredderView> viewsByUser = HashMultimap.create();
    private final IMDSubscriber bookSubscriber;
    private final Map<String, SymbolStackData> stackBySymbol;
    private final Map<String, WorkingOrdersByID> workingOrdersBySymbol;

    public ShredderPresenter(final IMDSubscriber depthBookSubscriber) {

        this.bookSubscriber = depthBookSubscriber;

        this.stackBySymbol = new MapMaker().makeComputingMap(symbol -> new SymbolStackData(symbol, Constants::NO_OP, Constants::NO_OP));
        this.workingOrdersBySymbol = new MapMaker().makeComputingMap(symbol -> new WorkingOrdersByID());
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

                final SymbolStackData stackData = stackBySymbol.get(symbol);

                view.subscribeToSymbol(symbol, levels, mdForSymbol, workingOrdersBySymbol.get(symbol), stackData);

                viewsBySymbol.put(symbol, view);
            } else {
                view.onRawInboundData(data);
                view.refreshAndFlush();
            }
        }
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final WorkingOrdersByID workingOrders = workingOrdersBySymbol.get(sourcedOrder.order.getSymbol());
        workingOrders.setWorkingOrder(sourcedOrder);
    }

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final WorkingOrdersByID workingOrders = workingOrdersBySymbol.get(sourcedOrder.order.getSymbol());
        workingOrders.removeWorkingOrder(sourcedOrder);
    }

    public void setTheo(final TheoValue theoValue) {
        final SymbolStackData stackData = stackBySymbol.get(theoValue.getSymbol());
        stackData.setTheoValue(theoValue);
    }

    public void setSpreadnoughtTheo(final SpreadnoughtTheo theo) {

        final SymbolStackData stackData = stackBySymbol.get(theo.getSymbol());
        stackData.setSpreadnoughtTheo(theo);
    }

    public void overrideLaserLine(final LaserLineValue laserLine) {

        final SymbolStackData stackData = stackBySymbol.get(laserLine.symbol);
        stackData.overrideStackData(laserLine);
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

    @Override
    public void stacksConnectionLost(final String remoteAppName) {

        for (final SymbolStackData stackData : stackBySymbol.values()) {
            stackData.stackConnectionLost(remoteAppName);
        }
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup, final StackClientHandler stackClientHandler) {

        final String symbol = stackGroup.getSymbol();
        final SymbolStackData stackData = stackBySymbol.get(symbol);
        stackData.setStackClientHandler(stackClientHandler);

        stackGroupUpdated(stackGroup);
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup) {

        final String symbol = stackGroup.getSymbol();
        final SymbolStackData stackData = stackBySymbol.get(symbol);
        if (BookSide.BID == stackGroup.getSide()) {
            stackData.setBidGroup(stackGroup);
        } else {
            stackData.setAskGroup(stackGroup);
        }
    }
}
