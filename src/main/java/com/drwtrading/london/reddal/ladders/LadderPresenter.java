package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.photons.reddal.CenterToPrice;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.photons.reddal.SymbolAvailable;
import com.drwtrading.london.reddal.ChixSymbolPair;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.SpreadContractSet;
import com.drwtrading.london.reddal.UserCycleRequest;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.SymbolMetaData;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.data.ibook.DepthBookSubscriber;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderentry.ServerDisconnected;
import com.drwtrading.london.reddal.orderentry.UpdateFromServer;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
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
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LadderPresenter {

    public static final long BATCH_FLUSH_INTERVAL_MS = 1000 / 5;
    public static final long HEARTBEAT_INTERVAL_MS = 1000;

    private final DepthBookSubscriber bookHandler;
    private final String ewokBaseURL;

    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;
    private final Publisher<StatsMsg> statsPublisher;

    private final Map<Publisher<WebSocketOutboundData>, LadderView> viewBySocket = new HashMap<>();
    private final Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, LadderView> viewsByUser = HashMultimap.create();
    private final Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(WorkingOrdersForSymbol::new);
    private final Map<String, OrderUpdatesForSymbol> eeifOrdersBySymbol = new MapMaker().makeComputingMap(OrderUpdatesForSymbol::new);
    private final Map<String, ExtraDataForSymbol> dataBySymbol = new MapMaker().makeComputingMap(ExtraDataForSymbol::new);
    private final Map<String, SymbolStackData> stackBySymbol = new MapMaker().makeComputingMap(SymbolStackData::new);
    private final Map<String, SymbolMetaData> metaDataBySymbol = new MapMaker().makeComputingMap(SymbolMetaData::new);
    private final Map<String, Map<String, LadderPrefsForSymbolUser>> ladderPrefsForUserBySymbol;
    private final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap = new HashMap<>();
    private final Map<String, MDForSymbol> marketDataForSymbolMap;
    private final Set<String> existingSymbols = new HashSet<>();

    private final TradingStatusForAll tradingStatusForAll = new TradingStatusForAll();
    private final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher;
    private final Publisher<HeartbeatRoundtrip> roundTripPublisher;
    private final Publisher<ReddalMessage> commandPublisher;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<Jsonable> trace;

    private final Fiber fiber;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher;
    private OpxlExDateSubscriber.IsinsGoingEx isinsGoingEx;

    public LadderPresenter(final DepthBookSubscriber bookHandler, final String ewokBaseURL,
                           final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer, final LadderOptions ladderOptions,
                           final Publisher<StatsMsg> statsPublisher, final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher,
                           final Publisher<HeartbeatRoundtrip> roundTripPublisher, final Publisher<ReddalMessage> commandPublisher,
                           final Publisher<RecenterLaddersForUser> recenterLaddersForUser, final Fiber fiber, final Publisher<Jsonable> trace,
                           final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher,
                           final Publisher<UserCycleRequest> userCycleContractPublisher,
                           final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher) {

        this.bookHandler = bookHandler;
        this.ewokBaseURL = ewokBaseURL;

        this.remoteOrderCommandByServer = remoteOrderCommandByServer;
        this.ladderOptions = ladderOptions;
        this.statsPublisher = statsPublisher;
        this.storeLadderPrefPublisher = storeLadderPrefPublisher;
        this.roundTripPublisher = roundTripPublisher;
        this.commandPublisher = commandPublisher;
        this.recenterLaddersForUser = recenterLaddersForUser;
        this.fiber = fiber;
        this.trace = trace;
        this.ladderClickTradingIssuePublisher = ladderClickTradingIssuePublisher;
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.orderEntryCommandToServerPublisher = orderEntryCommandToServerPublisher;
        this.ladderPrefsForUserBySymbol = new HashMap<>();
        this.marketDataForSymbolMap = new MapMaker().makeComputingMap(this::subscribeToMarketDataForSymbol);
    }

    private MDForSymbol subscribeToMarketDataForSymbol(final String symbol) {
        return new MDForSymbol(bookHandler, symbol);
    }

    private void unsubscribeFromMarketDataForSymbol(final String symbol) {

        final MDForSymbol md = marketDataForSymbolMap.get(symbol);
        md.unsubscribeForMD();
    }

    @Subscribe
    public void onSearchResult(final SearchResult searchResult) {
        existingSymbols.add(searchResult.symbol);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        final ILadderUI view = new WebSocketOutputDispatcher<>(ILadderUI.class).wrap(msg -> uiPipe.eval(msg.getData()));
        final LadderView ladderView =
                new LadderView(connected.getClient(), uiPipe, view, ewokBaseURL, remoteOrderCommandByServer, ladderOptions, statsPublisher,
                        tradingStatusForAll, roundTripPublisher, commandPublisher, recenterLaddersForUser, trace,
                        ladderClickTradingIssuePublisher, userCycleContractPublisher, orderEntryMap, orderEntryCommandToServerPublisher,
                        existingSymbols::contains);
        if (null != isinsGoingEx) {
            ladderView.setIsinsGoingEx(isinsGoingEx);
        }
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
            if (viewsBySymbol.get(symbol).isEmpty()) {
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
        if (null != view) {
            if ("ladder-subscribe".equals(cmd)) {
                final String symbol = args[1];
                final int levels = Integer.parseInt(args[2]);
                final MDForSymbol mdForSymbol = marketDataForSymbolMap.get(symbol);
                mdForSymbol.subscribeForMD();
                String userName = msg.getClient().getUserName();
                view.subscribeToSymbol(symbol, levels, mdForSymbol, ordersBySymbol.get(symbol), metaDataBySymbol.get(symbol),
                        dataBySymbol.get(symbol), stackBySymbol.get(symbol),
                        getLadderPrefsForSymbolUser(symbol, userName), eeifOrdersBySymbol.get(symbol));
                if (3 < args.length) {
                    if ("S".equals(args[3])) {
                        view.setStackView();
                    } else {
                        try {
                            final long price = (long) (Constants.NORMALISING_FACTOR * Double.parseDouble(args[3]));
                            view.setCenterPrice(price);
                        } catch (final NumberFormatException ignored) {
                            // Ignore price request.
                        }
                    }
                }
                viewsBySymbol.put(symbol, view);
            } else {
                view.onRawInboundData(data);
                view.fastInputFlush();
            }
        }
        if (!"heartbeat".equals(cmd)) {
            trace.publish(
                    new InboundDataTrace(msg.getClient().getHost(), msg.getClient().getUserName(), args, UiPipeImpl.getDataArg(args)));
        }
    }

    private LadderPrefsForSymbolUser getLadderPrefsForSymbolUser(String symbol, String userName) {
        Map<String, LadderPrefsForSymbolUser> symbolToPrefs = ladderPrefsForUserBySymbol.computeIfAbsent(userName, k -> new HashMap<>());
        LadderPrefsForSymbolUser prefs = symbolToPrefs.get(symbol);
        if (null == prefs) {
            MDForSymbol mdForSymbol = marketDataForSymbolMap.get(symbol);
            if (null != mdForSymbol && null != mdForSymbol.getBook()) {
                if (mdForSymbol.getBook().getInstType() == InstType.FUTURE) {
                    FutureConstant futureFromSymbol = FutureConstant.getFutureFromSymbol(symbol);
                    FutureExpiryCalc expiryCalc = new FutureExpiryCalc();
                    for (int i = 0; i > -3; i--) {
                        String existingSymbol = expiryCalc.getFutureCode(futureFromSymbol, i);
                        LadderPrefsForSymbolUser existingPrefs = symbolToPrefs.get(existingSymbol);
                        if (null != existingPrefs) {
                            prefs = existingPrefs.withSymbol(symbol);
                            break;
                        }
                    }
                } else if (mdForSymbol.getBook().getInstType() == InstType.FUTURE_SPREAD) {
                    String[] legs = symbol.split("-");
                    FutureConstant futureFromSymbol = FutureConstant.getFutureFromSymbol(legs[0]);
                    FutureExpiryCalc expiryCalc = new FutureExpiryCalc();
                    int firstExp = getRollsHence(legs[0], futureFromSymbol, expiryCalc);
                    int secondExp = getRollsHence(legs[1], futureFromSymbol, expiryCalc);
                    if (firstExp >= 0 && secondExp >= 0) {
                        for (int i = 0; i < 3; i++) {
                            String existingSymbol = expiryCalc.getFutureCode(futureFromSymbol, firstExp - i) +
                                    "-" + expiryCalc.getFutureCode(futureFromSymbol, secondExp - i);
                            LadderPrefsForSymbolUser existingPrefs = symbolToPrefs.get(existingSymbol);
                            if (null != existingPrefs) {
                                prefs = existingPrefs.withSymbol(symbol);
                                break;
                            }
                        }
                    }
                }
            }
            if (null == prefs) {
                prefs = new LadderPrefsForSymbolUser(symbol, userName, storeLadderPrefPublisher);
            }
            symbolToPrefs.put(symbol, prefs);
        }
        return prefs;
    }

    private int getRollsHence(String leg, FutureConstant futureFromSymbol, FutureExpiryCalc expiryCalc) {
        for (int firstExp = 0; firstExp < 8; firstExp++) {
            String firstSymbol = expiryCalc.getFutureCode(futureFromSymbol, firstExp);
            if (firstSymbol.equals(leg)) {
                return firstExp;
            }
        }
        return -1;
    }

    @Subscribe
    public void on(final LadderClickTradingIssue ladderClickTradingIssue) {
        final Collection<LadderView> views = viewsBySymbol.get(ladderClickTradingIssue.symbol);
        for (final LadderView view : views) {
            view.clickTradingIssue(ladderClickTradingIssue);
            fiber.schedule(() -> view.clickTradingIssue(new LadderClickTradingIssue(ladderClickTradingIssue.symbol, "")), 5000,
                    TimeUnit.MILLISECONDS);
        }

    }

    @Subscribe
    public void on(final WorkingOrderUpdateFromServer workingOrderUpdate) {
        ordersBySymbol.get(workingOrderUpdate.value.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
    }

    @Subscribe
    public void on(final LaserLine laserLine) {
        dataBySymbol.get(laserLine.getSymbol()).onLaserLine(laserLine);
    }

    @Subscribe
    public void on(final DeskPosition deskPosition) {
        metaDataBySymbol.get(deskPosition.getSymbol()).onDeskPosition(deskPosition);
    }

    @Subscribe
    public void on(final InfoOnLadder infoOnLadder) {
        metaDataBySymbol.get(infoOnLadder.getSymbol()).onInfoOnLadder(infoOnLadder);
    }

    @Subscribe
    public void on(final LadderText ladderText) {
        if ("execution".equals(ladderText.getCell())) {
            on(new LadderClickTradingIssue(ladderText.getSymbol(), ladderText.getText()));
        } else {
            metaDataBySymbol.get(ladderText.getSymbol()).onLadderText(ladderText);
        }
    }

    @Subscribe
    public void on(final LastTrade lastTrade) {
        dataBySymbol.get(lastTrade.getSymbol()).onLastTrade(lastTrade);
    }

    @Subscribe
    public void on(final SpreadContractSet spreadContractSet) {

        metaDataBySymbol.get(spreadContractSet.front).onFuturesContractSet(spreadContractSet);
        if (spreadContractSet.back != null) {
            metaDataBySymbol.get(spreadContractSet.back).onFuturesContractSet(spreadContractSet);
        }
        if (spreadContractSet.spread != null) {
            metaDataBySymbol.get(spreadContractSet.spread).onFuturesContractSet(spreadContractSet);
        }
    }

    @Subscribe
    public void on(final ChixSymbolPair chixSymbolPair) {

        metaDataBySymbol.get(chixSymbolPair.primarySymbol).setChixSwitchSymbol(chixSymbolPair.chixSymbol);
        metaDataBySymbol.get(chixSymbolPair.chixSymbol).setChixSwitchSymbol(chixSymbolPair.primarySymbol);
    }

    @Subscribe
    public void on(final Position position) {
        metaDataBySymbol.get(position.getSymbol()).onDayPosition(position);
    }

    public void setPKSExposure(final PKSExposure position) {
        metaDataBySymbol.get(position.symbol).onPKSExposure(position);
    }

    @Subscribe
    public void on(final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus) {
        tradingStatusForAll.on(serverTradingStatus);
        if (serverTradingStatus.workingOrderStatus == TradingStatusWatchdog.Status.NOT_OK) {
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

    @Subscribe
    public void on(final LadderSettings.LadderPrefLoaded ladderPrefLoaded) {
        final LadderSettings.LadderPref pref = ladderPrefLoaded.pref;
        getLadderPrefsForSymbolUser(pref.symbol, pref.user).on(ladderPrefLoaded);
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
        metaDataBySymbol.get(displaySymbol.marketDataSymbol).setDisplaySymbol(displaySymbol);
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
    public void on(final OrderEntryClient.SymbolOrderChannel symbolOrderChannel) {
        orderEntryMap.put(symbolOrderChannel.symbol, symbolOrderChannel);
    }

    @Subscribe
    public void on(final UpdateFromServer update) {
        eeifOrdersBySymbol.get(update.symbol).onUpdate(update);
    }

    @Subscribe
    public void on(final ServerDisconnected disconnected) {
        eeifOrdersBySymbol.forEach((s, orderUpdatesForSymbol) -> orderUpdatesForSymbol.onDisconnected(disconnected));
    }

    @Subscribe
    public void on(final OpxlExDateSubscriber.IsinsGoingEx isinsGoingEx) {
        this.isinsGoingEx = isinsGoingEx;
        viewBySocket.values().forEach(l -> l.setIsinsGoingEx(isinsGoingEx));
    }

    public long flushAllLadders() {
        try {
            for (final LadderView ladderView : viewBySocket.values()) {
                ladderView.timedRefresh();
            }
        } catch (final Throwable t) {
            statsPublisher.publish(new AdvisoryStat("Reddal", AdvisoryStat.Level.WARNING, "Failed to flush [" + t.getMessage() + "]."));
            t.printStackTrace();
        }
        return BATCH_FLUSH_INTERVAL_MS;
    }

    public long sendAllHeartbeats() {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.sendHeartbeat();
        }
        return HEARTBEAT_INTERVAL_MS;
    }

    public void stacksConnectionLost(final String remoteAppName) {

        for (final SymbolStackData stackData : stackBySymbol.values()) {
            stackData.stackConnectionLost(remoteAppName);
        }
    }

    public void stackGroupCreated(final StackGroup stackGroup, final StackClientHandler stackClientHandler) {

        final String symbol = stackGroup.getSymbol();
        final SymbolStackData stackData = stackBySymbol.get(symbol);
        stackData.setStackClientHandler(stackClientHandler);

        stackGroupUpdated(stackGroup);
    }

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
