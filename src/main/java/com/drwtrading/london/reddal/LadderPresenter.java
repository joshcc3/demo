package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.photons.reddal.CenterToPrice;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.photons.reddal.SymbolAvailable;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.data.*;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.util.Struct;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.photons.ladder.*;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import drw.london.json.Jsonable;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LadderPresenter {

    public static final int MD_FLUSH_MILLIS = 40;
    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;
    private final Publisher<StatsMsg> statsPublisher;

    private final Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, LadderView> viewsByUser = HashMultimap.create();
    private final Map<Publisher<WebSocketOutboundData>, LadderView> viewBySocket = new HashMap<>();
    private final Map<String, Runnable> marketDataUnsubscribers = new HashMap<>();
    private final Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(new Function<String, WorkingOrdersForSymbol>() {
        @Override
        public WorkingOrdersForSymbol apply(java.lang.String from) {
            return new WorkingOrdersForSymbol(from);
        }
    });
    private final Map<String, ExtraDataForSymbol> dataBySymbol = new MapMaker().makeComputingMap(new Function<String, ExtraDataForSymbol>() {
        @Override
        public ExtraDataForSymbol apply(java.lang.String from) {
            return new ExtraDataForSymbol(from);
        }
    });
    private final Map<String, Map<String, LadderPrefsForSymbolUser>> ladderPrefsForUserBySymbol;
    private final Map<String, MarketDataForSymbol> marketDataForSymbolMap;

    private final TradingStatusForAll tradingStatusForAll = new TradingStatusForAll();
    private final Publisher<LadderView.HeartbeatRoundtrip> roundtripPublisher;
    private final Publisher<ReddalMessage> commandPublisher;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<SubscribeToMarketData> subscribeToMarketData;
    private final Publisher<UnsubscribeFromMarketData> unsubscribeFromMarketData;
    private final Publisher<Jsonable> trace;

    private final Fiber fiber;
    private Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;

    public LadderPresenter(Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer, LadderOptions ladderOptions, Publisher<StatsMsg> statsPublisher, final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher, Publisher<LadderView.HeartbeatRoundtrip> roundtripPublisher, Publisher<ReddalMessage> commandPublisher, final Publisher<SubscribeToMarketData> subscribeToMarketData, Publisher<UnsubscribeFromMarketData> unsubscribeFromMarketData, final Publisher<RecenterLaddersForUser> recenterLaddersForUser, final Fiber fiber, Publisher<Jsonable> trace, Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher) {
        this.remoteOrderCommandByServer = remoteOrderCommandByServer;
        this.ladderOptions = ladderOptions;
        this.statsPublisher = statsPublisher;
        this.roundtripPublisher = roundtripPublisher;
        this.commandPublisher = commandPublisher;
        this.recenterLaddersForUser = recenterLaddersForUser;
        this.fiber = fiber;
        this.subscribeToMarketData = subscribeToMarketData;
        this.unsubscribeFromMarketData = unsubscribeFromMarketData;
        this.trace = trace;
        this.ladderClickTradingIssuePublisher = ladderClickTradingIssuePublisher;
        ladderPrefsForUserBySymbol = new MapMaker().makeComputingMap(new Function<String, Map<String, LadderPrefsForSymbolUser>>() {
            @Override
            public Map<String, LadderPrefsForSymbolUser> apply(final String symbol) {
                return new MapMaker().makeComputingMap(new Function<String, LadderPrefsForSymbolUser>() {
                    @Override
                    public LadderPrefsForSymbolUser apply(String user) {
                        return new LadderPrefsForSymbolUser(symbol, user, storeLadderPrefPublisher);
                    }
                });
            }
        });
        marketDataForSymbolMap = new MapMaker().makeComputingMap(new Function<String, MarketDataForSymbol>() {
            @Override
            public MarketDataForSymbol apply(final String symbol) {
                return subscribeToMarketDataForSymbol(symbol, fiber);
            }
        });

    }

    private MarketDataForSymbol subscribeToMarketDataForSymbol(final String symbol, final Fiber fiber) {
        // Subscribe to channel for this symbol
        final MemoryChannel<MarketDataEvent> marketDataEventMemoryChannel = new MemoryChannel<>();
        MarketDataForSymbol marketDataForSymbol = new MarketDataForSymbol(symbol);
        marketDataEventMemoryChannel.subscribe(new BatchSubscriber<>(fiber, marketDataForSymbol.onMarketDataBatchCallback(), MD_FLUSH_MILLIS, TimeUnit.MILLISECONDS));
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                unsubscribeFromMarketData.publish(new UnsubscribeFromMarketData(symbol, marketDataEventMemoryChannel));
            }
        };
        marketDataUnsubscribers.put(symbol, runnable);
        subscribeToMarketData.publish(new SubscribeToMarketData(symbol, marketDataEventMemoryChannel));
        return marketDataForSymbol;
    }

    private void unsubscribeFromMarketDataForSymbol(final String symbol) {
        marketDataForSymbolMap.remove(symbol);
        marketDataUnsubscribers.remove(symbol).run();
    }

    @Subscribe
    public void onConnected(WebSocketConnected connected) {
        UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        View view = new WebSocketOutputDispatcher<View>(View.class).wrap(uiPipe.evalPublisher());
        LadderView ladderView = new LadderView(connected.getClient(), uiPipe, view, remoteOrderCommandByServer, ladderOptions, statsPublisher, tradingStatusForAll, roundtripPublisher, commandPublisher, recenterLaddersForUser, trace, ladderClickTradingIssuePublisher);
        viewBySocket.put(connected.getOutboundChannel(), ladderView);
        viewsByUser.put(connected.getClient().getUserName(), ladderView);
    }

    @Subscribe
    public void onDisconnected(WebSocketDisconnected disconnected) {
        LadderView view = viewBySocket.remove(disconnected.getOutboundChannel());
        if (view != null && view.symbol != null) {
            String symbol = view.symbol;
            viewsBySymbol.remove(symbol, view);
            viewsByUser.remove(disconnected.getClient().getUserName(), view);
            if (viewsBySymbol.get(symbol).size() == 0) {
                unsubscribeFromMarketDataForSymbol(symbol);
            }
        }
    }

    @Subscribe
    public void onMessage(WebSocketInboundData msg) {
        String data = msg.getData();
        String[] args = data.split("\0");
        String cmd = args[0];
        LadderView view = viewBySocket.get(msg.getOutboundChannel());
        if (view != null) {
            if (cmd.equals("ladder-subscribe")) {
                final String symbol = args[1];
                final int levels = Integer.parseInt(args[2]);
                MarketDataForSymbol marketDataForSymbol = marketDataForSymbolMap.get(symbol);
                view.subscribeToSymbol(symbol, levels, marketDataForSymbol, ordersBySymbol.get(symbol), dataBySymbol.get(symbol),
                        ladderPrefsForUserBySymbol.get(symbol).get(msg.getClient().getUserName()));
                if (3 < args.length) {
                    try {
                        final long price = (long) (Constants.NORMALISING_FACTOR * Double.parseDouble(args[3]));
                        view.setCenterPrice(price);
                    } catch (final NumberFormatException nfe) {
                        // Ignore price request.
                    }
                }
                viewsBySymbol.put(symbol, view);
            } else {
                view.onRawInboundData(data);
            }
        }
    }

    @Subscribe
    public void on(final LadderClickTradingIssue ladderClickTradingIssue){
        Collection<LadderView> views = viewsBySymbol.get(ladderClickTradingIssue.symbol);
        for (final LadderView view : views) {
            view.clickTradingIssue(ladderClickTradingIssue);
            fiber.schedule(new Runnable() {
                @Override
                public void run() {
                    view.clickTradingIssue(new LadderClickTradingIssue(ladderClickTradingIssue.symbol, ""));
                }
            }, 2000, TimeUnit.MILLISECONDS);
        }

    }
    @Subscribe
    public void on(Main.WorkingOrderUpdateFromServer workingOrderUpdate) {
        ordersBySymbol.get(workingOrderUpdate.value.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
    }

    @Subscribe
    public void on(LaserLine laserLine) {
        dataBySymbol.get(laserLine.getSymbol()).onLaserLine(laserLine);
    }

    @Subscribe
    public void on(DeskPosition deskPosition) {
        dataBySymbol.get(deskPosition.getSymbol()).onDeskPosition(deskPosition);
    }

    @Subscribe
    public void on(InfoOnLadder infoOnLadder) {
        dataBySymbol.get(infoOnLadder.getSymbol()).onInfoOnLadder(infoOnLadder);
    }

    @Subscribe
    public void on(final LadderText ladderText) {
        if ("execution".equals(ladderText.getCell())) {
            on(new LadderClickTradingIssue(ladderText.getSymbol(), ladderText.getText()));
        }else {
            dataBySymbol.get(ladderText.getSymbol()).onLadderText(ladderText);
        }
    }

    @Subscribe
    public void on(LastTrade lastTrade) {
        dataBySymbol.get(lastTrade.getSymbol()).onLastTrade(lastTrade);
    }

    @Subscribe
    public void on(SpreadContractSet spreadContractSet) {
        dataBySymbol.get(spreadContractSet.front).onFuturesContractSet(spreadContractSet);
        if (spreadContractSet.back != null) {
            dataBySymbol.get(spreadContractSet.back).onFuturesContractSet(spreadContractSet);
        }
        if (spreadContractSet.spread != null) {
            dataBySymbol.get(spreadContractSet.spread).onFuturesContractSet(spreadContractSet);
        }
    }

    @Subscribe
    public void on(Position position) {
        dataBySymbol.get(position.getSymbol()).onDayPosition(position);
    }

    @Subscribe
    public void on(TradingStatusWatchdog.ServerTradingStatus serverTradingStatus) {
        tradingStatusForAll.on(serverTradingStatus);
        if (serverTradingStatus.workingOrderStatus == TradingStatusWatchdog.Status.NOT_OK) {
            for (WorkingOrdersForSymbol ordersForSymbol : ordersBySymbol.values()) {
                for (Iterator<Main.WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByKey.values().iterator(); iter.hasNext(); ) {
                    Main.WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
                for (Iterator<Main.WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByPrice.values().iterator(); iter.hasNext(); ) {
                    Main.WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    @Subscribe
    public void on(LadderSettings.LadderPrefLoaded ladderPrefLoaded) {
        LadderSettings.LadderPref pref = ladderPrefLoaded.pref;
        ladderPrefsForUserBySymbol.get(pref.symbol).get(pref.user).on(ladderPrefLoaded);
    }

    @Subscribe
    public void on(CenterToPrice centerToPrice) {
        for (LadderView ladderView : viewBySocket.values()) {
            ladderView.recenterLadderForUser(centerToPrice);
        }
    }

    @Subscribe
    public void on(SymbolAvailable symbolAvailable) {
        dataBySymbol.get(symbolAvailable.getSymbol()).setSymbolAvailable();
    }

    @Subscribe
    public void on(DisplaySymbol displaySymbol) {
        dataBySymbol.get(displaySymbol.marketDataSymbol).setDisplaySymbol(displaySymbol);
    }

    @Subscribe
    public void on(RecenterLaddersForUser recenterLaddersForUser) {
        for (LadderView ladderView : viewBySocket.values()) {
            ladderView.recenterLadderForUser(recenterLaddersForUser);
        }
    }

    @Subscribe
    public void onSingleOrderCommand(OrdersPresenter.SingleOrderCommand singleOrderCommand) {
        for (LadderView view : viewsByUser.get(singleOrderCommand.getUsername())) {
            if (view.symbol.equals(singleOrderCommand.getSymbol())) {
                view.onSingleOrderCommand(singleOrderCommand);
                return;
            }
        }
    }

    public Runnable flushBatchedData() {
        return new Runnable() {
            @Override
            public void run() {
                flushAllLadders();
            }
        };
    }

    private void flushAllLadders() {
        for (LadderView ladderView : viewBySocket.values()) {
            ladderView.flush();
        }
    }

    public Runnable sendHeartbeats() {
        return new Runnable() {
            @Override
            public void run() {
                for (LadderView ladderView : viewBySocket.values()) {
                    ladderView.sendHeartbeat();
                }
            }
        };
    }


    public static class RecenterLaddersForUser extends Struct {
        public final String user;

        public RecenterLaddersForUser(final String user) {
            this.user = user;
        }
    }

    public interface View {
        public void draw(int levels);

        public void trading(boolean tradingEnabled, Collection<String> orderTypesLeft, Collection<String> orderTypesRight);

        public void selecta(boolean enabled);

        public void goToSymbol(String symbol);

        public void popUp(String url, String name, int width, int height);
    }
}
