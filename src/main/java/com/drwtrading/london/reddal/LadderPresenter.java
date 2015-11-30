package com.drwtrading.london.reddal;

import com.drwtrading.eeif.md.remote.SubscribeMarketData;
import com.drwtrading.eeif.md.remote.UnsubscribeMarketData;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommand;
import com.drwtrading.london.photons.reddal.CenterToPrice;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.photons.reddal.SymbolAvailable;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.data.DisplaySymbol;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderentry.ServerDisconnected;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.orderentry.UpdateFromServer;
import com.drwtrading.london.util.Struct;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.photons.ladder.LastTrade;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
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


    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;
    private final Publisher<StatsMsg> statsPublisher;

    private final Map<Publisher<WebSocketOutboundData>, LadderView> viewBySocket = new HashMap<>();
    private final Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, LadderView> viewsByUser = HashMultimap.create();
    private final Map<String, Runnable> marketDataUnsubscribers = new HashMap<>();
    private final Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(WorkingOrdersForSymbol::new);
    private final Map<String, OrderUpdatesForSymbol> eeifOrdersBySymbol = new MapMaker().makeComputingMap(OrderUpdatesForSymbol::new);
    private final Map<String, ExtraDataForSymbol> dataBySymbol = new MapMaker().makeComputingMap(ExtraDataForSymbol::new);
    private final Map<String, Map<String, LadderPrefsForSymbolUser>> ladderPrefsForUserBySymbol;
    private final Map<OrderEntryClient.SymbolOrder, Publisher<OrderEntryCommand>> orderEntryMap = new HashMap<>();
    private final Map<String, MarketDataForSymbol> marketDataForSymbolMap;

    private final TradingStatusForAll tradingStatusForAll = new TradingStatusForAll();
    private final Publisher<LadderView.HeartbeatRoundtrip> roundtripPublisher;
    private final Publisher<ReddalMessage> commandPublisher;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<SubscribeMarketData> subscribeToMarketData;
    private final Publisher<UnsubscribeMarketData> unsubscribeFromMarketData;
    private final Publisher<Jsonable> trace;

    private final Fiber fiber;
    private Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher;

    public LadderPresenter(final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer, final LadderOptions ladderOptions,
                           final Publisher<StatsMsg> statsPublisher, final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher,
                           final Publisher<LadderView.HeartbeatRoundtrip> roundtripPublisher, final Publisher<ReddalMessage> commandPublisher,
                           final Publisher<SubscribeMarketData> subscribeToMarketData, final Publisher<UnsubscribeMarketData> unsubscribeFromMarketData,
                           final Publisher<RecenterLaddersForUser> recenterLaddersForUser, final Fiber fiber, final Publisher<Jsonable> trace,
                           final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher, final Publisher<UserCycleRequest> userCycleContractPublisher, Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher) {
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
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.orderEntryCommandToServerPublisher = orderEntryCommandToServerPublisher;
        ladderPrefsForUserBySymbol = new MapMaker().makeComputingMap(symbol -> new MapMaker().makeComputingMap(user -> new LadderPrefsForSymbolUser(symbol, user, storeLadderPrefPublisher)));
        marketDataForSymbolMap = new MapMaker().makeComputingMap(symbol -> subscribeToMarketDataForSymbol(symbol, fiber));
    }

    private MarketDataForSymbol subscribeToMarketDataForSymbol(final String symbol, final Fiber fiber) {
        // Subscribe to channel for this symbol
        final MemoryChannel<MarketDataEvent> marketDataEventMemoryChannel = new MemoryChannel<>();
        final MarketDataForSymbol marketDataForSymbol = new MarketDataForSymbol(symbol);
        marketDataEventMemoryChannel.subscribe(new BatchSubscriber<>(fiber, (message) -> {
            marketDataForSymbol.onMarketDataBatch(message);
            fastFlushSymbol(symbol);
        }, 0, TimeUnit.MILLISECONDS));
        marketDataUnsubscribers.put(symbol, () -> unsubscribeFromMarketData.publish(new UnsubscribeMarketData(symbol, marketDataEventMemoryChannel)));
        subscribeToMarketData.publish(new SubscribeMarketData(symbol, marketDataEventMemoryChannel));
        return marketDataForSymbol;
    }

    private void unsubscribeFromMarketDataForSymbol(final String symbol) {
        marketDataForSymbolMap.remove(symbol);
        marketDataUnsubscribers.remove(symbol).run();
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        final View view = new WebSocketOutputDispatcher<>(View.class).wrap(uiPipe.evalPublisher());
        final LadderView ladderView =
                new LadderView(connected.getClient(), uiPipe, view, remoteOrderCommandByServer, ladderOptions, statsPublisher,
                        tradingStatusForAll, roundtripPublisher, commandPublisher, recenterLaddersForUser, trace,
                        ladderClickTradingIssuePublisher, userCycleContractPublisher, orderEntryMap, orderEntryCommandToServerPublisher);
        viewBySocket.put(connected.getOutboundChannel(), ladderView);
        viewsByUser.put(connected.getClient().getUserName(), ladderView);
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        final LadderView view = viewBySocket.remove(disconnected.getOutboundChannel());
        if (view != null && view.symbol != null) {
            final String symbol = view.symbol;
            viewsBySymbol.remove(symbol, view);
            final String user = disconnected.getClient().getUserName();
            viewsByUser.remove(user, view);
            if (viewsBySymbol.get(symbol).size() == 0) {
                unsubscribeFromMarketDataForSymbol(symbol);
            }
        }
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        final String data = msg.getData();
        final String[] args = data.split("\0");
        final String cmd = args[0];
        final LadderView view = viewBySocket.get(msg.getOutboundChannel());
        if (view != null) {
            if (cmd.equals("ladder-subscribe")) {
                final String symbol = args[1];
                final int levels = Integer.parseInt(args[2]);
                final MarketDataForSymbol marketDataForSymbol = marketDataForSymbolMap.get(symbol);
                view.subscribeToSymbol(symbol, levels, marketDataForSymbol, ordersBySymbol.get(symbol), dataBySymbol.get(symbol),
                        ladderPrefsForUserBySymbol.get(symbol).get(msg.getClient().getUserName()), eeifOrdersBySymbol.get(symbol));
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
                view.fastInputFlush();
            }
        }
        trace.publish(new LadderView.InboundDataTrace(msg.getClient().getHost(), msg.getClient().getUserName(), args, UiPipeImpl.getDataArg(args)));
    }

    @Subscribe
    public void on(final LadderClickTradingIssue ladderClickTradingIssue) {
        final Collection<LadderView> views = viewsBySymbol.get(ladderClickTradingIssue.symbol);
        for (final LadderView view : views) {
            view.clickTradingIssue(ladderClickTradingIssue);
            fiber.schedule(() -> view.clickTradingIssue(new LadderClickTradingIssue(ladderClickTradingIssue.symbol, "")), 5000, TimeUnit.MILLISECONDS);
        }

    }

    @Subscribe
    public void on(final Main.WorkingOrderUpdateFromServer workingOrderUpdate) {
        ordersBySymbol.get(workingOrderUpdate.value.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
    }

    @Subscribe
    public void on(final LaserLine laserLine) {
        dataBySymbol.get(laserLine.getSymbol()).onLaserLine(laserLine);
    }

    @Subscribe
    public void on(final DeskPosition deskPosition) {
        dataBySymbol.get(deskPosition.getSymbol()).onDeskPosition(deskPosition);
    }

    @Subscribe
    public void on(final InfoOnLadder infoOnLadder) {
        dataBySymbol.get(infoOnLadder.getSymbol()).onInfoOnLadder(infoOnLadder);
    }

    @Subscribe
    public void on(final LadderText ladderText) {
        if ("execution".equals(ladderText.getCell())) {
            on(new LadderClickTradingIssue(ladderText.getSymbol(), ladderText.getText()));
        } else {
            dataBySymbol.get(ladderText.getSymbol()).onLadderText(ladderText);
        }
    }

    @Subscribe
    public void on(final LastTrade lastTrade) {
        dataBySymbol.get(lastTrade.getSymbol()).onLastTrade(lastTrade);
    }

    @Subscribe
    public void on(final SpreadContractSet spreadContractSet) {
        dataBySymbol.get(spreadContractSet.front).onFuturesContractSet(spreadContractSet);
        if (spreadContractSet.back != null) {
            dataBySymbol.get(spreadContractSet.back).onFuturesContractSet(spreadContractSet);
        }
        if (spreadContractSet.spread != null) {
            dataBySymbol.get(spreadContractSet.spread).onFuturesContractSet(spreadContractSet);
        }
    }

    @Subscribe
    public void on(final Position position) {
        dataBySymbol.get(position.getSymbol()).onDayPosition(position);
    }

    @Subscribe
    public void on(final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus) {
        tradingStatusForAll.on(serverTradingStatus);
        if (serverTradingStatus.workingOrderStatus == TradingStatusWatchdog.Status.NOT_OK) {
            for (final WorkingOrdersForSymbol ordersForSymbol : ordersBySymbol.values()) {
                for (final Iterator<Main.WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByKey.values().iterator(); iter.hasNext(); ) {
                    final Main.WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
                for (final Iterator<Main.WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByPrice.values().iterator(); iter.hasNext(); ) {
                    final Main.WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    @Subscribe
    public void on(final LadderSettings.LadderPrefLoaded ladderPrefLoaded) {
        final LadderSettings.LadderPref pref = ladderPrefLoaded.pref;
        ladderPrefsForUserBySymbol.get(pref.symbol).get(pref.user).on(ladderPrefLoaded);
    }

    @Subscribe
    public void on(final CenterToPrice centerToPrice) {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.recenterLadderForUser(centerToPrice);
        }
    }

    @Subscribe
    public void on(final SymbolAvailable symbolAvailable) {
        dataBySymbol.get(symbolAvailable.getSymbol()).setSymbolAvailable();
    }

    @Subscribe
    public void on(final DisplaySymbol displaySymbol) {
        dataBySymbol.get(displaySymbol.marketDataSymbol).setDisplaySymbol(displaySymbol);
    }

    @Subscribe
    public void on(final RecenterLaddersForUser recenterLaddersForUser) {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.recenterLadderForUser(recenterLaddersForUser);
        }
    }

    @Subscribe
    public void on(final OrdersPresenter.SingleOrderCommand singleOrderCommand) {
        for (final LadderView view : viewsByUser.get(singleOrderCommand.getUsername())) {
            if (view.symbol.equals(singleOrderCommand.getSymbol())) {
                view.onSingleOrderCommand(singleOrderCommand);
                return;
            }
        }
    }

    @Subscribe
    public void on(final ReplaceCommand replaceCommand) {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.replaceSymbol(replaceCommand);
        }
    }

    @Subscribe
    public void on(final UserCycleRequest request) {
        viewsByUser.get(request.username).forEach(LadderView::nextContract);
    }

    @Subscribe
    public void on(OrderEntryClient.SymbolOrderChannel symbolOrderChannel) {
        orderEntryMap.put(symbolOrderChannel.symbolOrder, symbolOrderChannel.publisher);
    }


    @Subscribe
    public void on(UpdateFromServer update) {
        eeifOrdersBySymbol.get(update.getSymbol()).onUpdate(update);
    }

    @Subscribe
    public void on(ServerDisconnected disconnected) {
        eeifOrdersBySymbol.forEach((s, orderUpdatesForSymbol) -> {
            orderUpdatesForSymbol.onDisconnected(disconnected);
        });
    }

    public void flushAllLadders() {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.flush();
        }
    }


    public void fastFlushSymbol(String symbol) {
        for (final LadderView ladderView : viewsBySymbol.get(symbol)) {
            ladderView.fastMdFlush();
        }
    }

    public void sendAllHeartbeats() {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.sendHeartbeat();
        }
    }

    public static class RecenterLaddersForUser extends Struct {
        public final String user;

        public RecenterLaddersForUser(final String user) {
            this.user = user;
        }
    }

    public interface View {
        void draw(int levels);

        void trading(boolean tradingEnabled, Collection<String> orderTypesLeft, Collection<String> orderTypesRight);

        void selecta(boolean enabled);

        void goToSymbol(String symbol);

        void popUp(String url, String name, int width, int height);

        void launchBasket(String symbol);
    }
}
