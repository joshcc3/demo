package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.KeyedBatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.UserCycleRequest;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.LastTradeDataForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.opxl.OPXLDeskPositions;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.picard.IPicardSpotter;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.premium.IPremiumCalc;
import com.drwtrading.london.reddal.safety.ServerTradingStatus;
import com.drwtrading.london.reddal.stacks.IStackPresenterCallback;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.symbols.ChixSymbolPair;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.symbols.SymbolDescription;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersPresenter;
import com.drwtrading.london.reddal.workspace.HostWorkspaceRequest;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import drw.eeif.fees.FeesCalc;
import drw.london.json.Jsonable;
import org.jetlang.channels.Converter;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LadderPresenter implements IStackPresenterCallback {

    private static final long BATCH_FLUSH_INTERVAL_MS = 1000 / 5;
    private static final long HEARTBEAT_INTERVAL_MS = 1000;

    private final IResourceMonitor<ReddalComponents> monitor;
    private final IMDSubscriber bookSubscriber;
    private final String ewokBaseURL;

    private final Publisher<RemoteOrderCommandToServer> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;

    private final FXCalc<?> fxCalc;
    private final FeesCalc feesCalc;
    private final DecimalFormat feeDF;

    private final DecimalFormat oneDP;

    private final Map<Publisher<WebSocketOutboundData>, LadderView> viewBySocket = new HashMap<>();
    private final Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, LadderView> viewsByUser = HashMultimap.create();
    private final Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(WorkingOrdersForSymbol::new);
    private final Map<String, OrderUpdatesForSymbol> eeifOrdersBySymbol = new MapMaker().makeComputingMap(OrderUpdatesForSymbol::new);
    private final Map<String, LastTradeDataForSymbol> lastTradeBySymbol = new MapMaker().makeComputingMap(LastTradeDataForSymbol::new);
    private final Map<String, LadderMetaData> metaDataBySymbol = new MapMaker().makeComputingMap(LadderMetaData::new);

    private final Map<String, Map<String, LadderPrefsForSymbolUser>> ladderPrefsForUserBySymbol;
    private final Map<String, SymbolStackData> stackBySymbol;
    private final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap;
    private final Map<String, SearchResult> refData;
    private final Map<String, String> symbolDesc = new HashMap<>();

    private final TradingStatusForAll tradingStatusForAll = new TradingStatusForAll();
    private final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher;
    private final Publisher<HeartbeatRoundtrip> roundTripPublisher;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<Jsonable> trace;
    private final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher;
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher;
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher;

    private final Fiber fiber;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher;
    private final Publisher<HostWorkspaceRequest> userWorkspaceRequests;
    private OpxlExDateSubscriber.IsinsGoingEx isinsGoingEx;

    public LadderPresenter(final IResourceMonitor<ReddalComponents> monitor, final IMDSubscriber bookSubscriber, final String ewokBaseURL,
            final Publisher<RemoteOrderCommandToServer> remoteOrderCommandByServer, final LadderOptions ladderOptions,
            final IPicardSpotter picardSpotter, final IPremiumCalc premiumCalc, final FXCalc<?> fxCalc,
            final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher,
            final Publisher<HeartbeatRoundtrip> roundTripPublisher, final Publisher<RecenterLaddersForUser> recenterLaddersForUser,
            final Fiber fiber, final Publisher<Jsonable> trace, final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher,
            final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher,
            final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher,
            final Publisher<UserCycleRequest> userCycleContractPublisher,
            final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher,
            final Publisher<HostWorkspaceRequest> userWorkspaceRequests) {

        this.monitor = monitor;
        this.bookSubscriber = bookSubscriber;
        this.ewokBaseURL = ewokBaseURL;

        this.remoteOrderCommandByServer = remoteOrderCommandByServer;
        this.ladderOptions = ladderOptions;

        this.fxCalc = fxCalc;
        this.feesCalc = new FeesCalc(msg -> monitor.logError(ReddalComponents.FEES_CALC, msg), fxCalc);
        this.feeDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 5);

        this.oneDP = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 1);

        this.stackBySymbol = new MapMaker().makeComputingMap(symbol -> new SymbolStackData(symbol, picardSpotter, premiumCalc));
        this.ladderPrefsForUserBySymbol = new HashMap<>();
        this.orderEntryMap = new HashMap<>();
        this.refData = new HashMap<>();

        this.storeLadderPrefPublisher = storeLadderPrefPublisher;
        this.roundTripPublisher = roundTripPublisher;
        this.recenterLaddersForUser = recenterLaddersForUser;
        this.fiber = fiber;
        this.trace = trace;
        this.increaseParentOffsetPublisher = increaseParentOffsetPublisher;
        this.increaseChildOffsetCmdPublisher = increaseChildOffsetCmdPublisher;
        this.disableSiblingsCmdPublisher = disableSiblingsCmdPublisher;

        this.ladderClickTradingIssuePublisher = ladderClickTradingIssuePublisher;
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.orderEntryCommandToServerPublisher = orderEntryCommandToServerPublisher;
        this.userWorkspaceRequests = userWorkspaceRequests;
    }

    @Subscribe
    public void onSearchResult(final SearchResult searchResult) {

        refData.put(searchResult.symbol, searchResult);
    }

    public void setTheo(final TheoValue theoValue) {

        final SymbolStackData stackData = stackBySymbol.get(theoValue.getSymbol());
        stackData.setTheoValue(theoValue);
    }

    public void setSpreadnoughtTheo(final SpreadnoughtTheo theo) {

        final SymbolStackData stackData = stackBySymbol.get(theo.getSymbol());
        stackData.setSpreadnoughtTheo(theo);
    }

    public void setLastTrade(final LastTrade lastTrade) {

        final LastTradeDataForSymbol data = lastTradeBySymbol.get(lastTrade.getSymbol());
        data.setLastTrade(lastTrade);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        final ILadderUI view = new WebSocketOutputDispatcher<>(ILadderUI.class).wrap(msg -> uiPipe.eval(msg.getData()));
        final LadderView ladderView =
                new LadderView(monitor, connected.getClient(), uiPipe, view, ewokBaseURL, remoteOrderCommandByServer, ladderOptions, fxCalc,
                        feesCalc, feeDF, tradingStatusForAll, roundTripPublisher, recenterLaddersForUser, trace,
                        ladderClickTradingIssuePublisher, userCycleContractPublisher, userWorkspaceRequests, orderEntryMap,
                        orderEntryCommandToServerPublisher, increaseParentOffsetPublisher, increaseChildOffsetCmdPublisher,
                        disableSiblingsCmdPublisher, refData::containsKey, symbolDesc);
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
                bookSubscriber.unsubscribeForMD(symbol, this);
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
                final MDForSymbol mdForSymbol = bookSubscriber.subscribeForMD(symbol, this);

                final WorkingOrdersForSymbol workingOrders = ordersBySymbol.get(symbol);
                final LadderMetaData ladderMetaData = metaDataBySymbol.get(symbol);
                final LastTradeDataForSymbol lastTradeData = lastTradeBySymbol.get(symbol);
                final SymbolStackData stackData = stackBySymbol.get(symbol);
                final OrderUpdatesForSymbol orderUpdates = eeifOrdersBySymbol.get(symbol);

                final String userName = msg.getClient().getUserName();
                view.subscribeToSymbol(symbol, levels, mdForSymbol, workingOrders, ladderMetaData, lastTradeData, stackData,
                        getLadderPrefsForSymbolUser(symbol, userName), orderUpdates);

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

    private LadderPrefsForSymbolUser getLadderPrefsForSymbolUser(final String symbol, final String userName) {

        final Map<String, LadderPrefsForSymbolUser> symbolToPrefs =
                ladderPrefsForUserBySymbol.computeIfAbsent(userName, k -> new HashMap<>());
        LadderPrefsForSymbolUser prefs = symbolToPrefs.get(symbol);
        if (null == prefs) {

            final SearchResult searchResult = refData.get(symbol);
            if (null != searchResult) {
                if (searchResult.instType == InstType.FUTURE) {
                    final FutureConstant futureFromSymbol = FutureConstant.getFutureFromSymbol(symbol);
                    if (null != futureFromSymbol) {
                        final FutureExpiryCalc expiryCalc = new FutureExpiryCalc();
                        for (int i = 0; i > -3; i--) {
                            final String existingSymbol = expiryCalc.getFutureCode(futureFromSymbol, i);
                            final LadderPrefsForSymbolUser existingPrefs = symbolToPrefs.get(existingSymbol);
                            if (null != existingPrefs) {
                                prefs = existingPrefs.withSymbol(symbol);
                                break;
                            }
                        }
                    }
                } else if (searchResult.instType == InstType.FUTURE_SPREAD) {
                    final String[] legs = symbol.split("-");
                    final FutureConstant futureFromSymbol = FutureConstant.getFutureFromSymbol(legs[0]);
                    if (null != futureFromSymbol) {
                        final FutureExpiryCalc expiryCalc = new FutureExpiryCalc();
                        final int firstExp = getRollsHence(legs[0], futureFromSymbol, expiryCalc);
                        final int secondExp = getRollsHence(legs[1], futureFromSymbol, expiryCalc);
                        if (firstExp >= 0 && secondExp >= 0) {
                            for (int i = 0; i < 3; i++) {
                                final String existingSymbol = expiryCalc.getFutureCode(futureFromSymbol, firstExp - i) + '-' +
                                        expiryCalc.getFutureCode(futureFromSymbol, secondExp - i);
                                final LadderPrefsForSymbolUser existingPrefs = symbolToPrefs.get(existingSymbol);
                                if (null != existingPrefs) {
                                    prefs = existingPrefs.withSymbol(symbol);
                                    break;
                                }
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

    private static int getRollsHence(final String leg, final FutureConstant futureFromSymbol, final FutureExpiryCalc expiryCalc) {
        for (int firstExp = 0; firstExp < 8; firstExp++) {
            final String firstSymbol = expiryCalc.getFutureCode(futureFromSymbol, firstExp);
            if (firstSymbol.equals(leg)) {
                return firstExp;
            }
        }
        return -1;
    }

    @KeyedBatchSubscriber(converter = WorkingOrdersPresenter.WOConverter.class, flushInterval = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Subscribe
    public void onWorkingOrderUpdates(final Map<String, WorkingOrderUpdateFromServer> workingOrderUpdates) {
        for (final WorkingOrderUpdateFromServer workingOrderUpdate : workingOrderUpdates.values()) {
            ordersBySymbol.get(workingOrderUpdate.workingOrderUpdate.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
        }
    }

    public void overrideLaserLine(final LaserLineValue laserLine) {

        final SymbolStackData stackData = stackBySymbol.get(laserLine.symbol);
        stackData.overrideStackData(laserLine);
    }

    public void setDeskPositions(final OPXLDeskPositions deskPositions) {

        for (final Map.Entry<String, Long> position : deskPositions.positions.entrySet()) {

            final String symbol = position.getKey();
            final LadderMetaData metaData = metaDataBySymbol.get(symbol);

            metaData.setDeskPosition(oneDP, position.getValue());
        }
    }

    @Subscribe
    public void setMrPhilPosition(final Position position) {
        metaDataBySymbol.get(position.getSymbol()).setMrPhilPosition(oneDP, position);
    }

    public void setPKSExposure(final PKSExposure position) {
        metaDataBySymbol.get(position.symbol).onPKSExposure(oneDP, position);
    }

    @Subscribe
    public void on(final LadderText ladderText) {
        if ("execution".equals(ladderText.getCell())) {
            displayTradeIssue(ladderText.getSymbol(), ladderText.getText());
        } else {
            metaDataBySymbol.get(ladderText.getSymbol()).onLadderText(ladderText);
        }
    }

    public void displayTradeIssue(final LadderClickTradingIssue issue) {
        displayTradeIssue(issue.symbol, issue.issue);
    }

    public void displayTradeIssue(final String symbol, final String text) {

        final LadderClickTradingIssue ladderClickTradingIssue = new LadderClickTradingIssue(symbol, text);
        final Collection<LadderView> views = viewsBySymbol.get(symbol);
        for (final LadderView view : views) {
            view.clickTradingIssue(ladderClickTradingIssue);
            fiber.schedule(() -> view.clickTradingIssue(new LadderClickTradingIssue(symbol, "")), 5000, TimeUnit.MILLISECONDS);
        }
    }

    @Subscribe
    public void on(final SpreadContractSet spreadContractSet) {

        metaDataBySymbol.get(spreadContractSet.symbol).onSpreadContractSet(spreadContractSet);
        if (spreadContractSet.nextContract != null) {
            metaDataBySymbol.get(spreadContractSet.nextContract).onSpreadContractSet(spreadContractSet);
        }
        if (spreadContractSet.contractAfterNext != null) {
            metaDataBySymbol.get(spreadContractSet.contractAfterNext).onSpreadContractSet(spreadContractSet);
        }
    }

    @Subscribe
    public void on(final ChixSymbolPair chixSymbolPair) {

        metaDataBySymbol.get(chixSymbolPair.primarySymbol).setChixSwitchSymbol(chixSymbolPair.chixSymbol);
        metaDataBySymbol.get(chixSymbolPair.chixSymbol).setChixSwitchSymbol(chixSymbolPair.primarySymbol);
    }

    @Subscribe
    public void on(final ServerTradingStatus serverTradingStatus) {
        tradingStatusForAll.on(serverTradingStatus);
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

    @Subscribe
    public void on(final LadderSettings.LadderPrefLoaded ladderPrefLoaded) {
        final LadderSettings.LadderPref pref = ladderPrefLoaded.pref;
        getLadderPrefsForSymbolUser(pref.symbol, pref.user).on(ladderPrefLoaded);
    }

    public void recenterLadder(final RecenterLadder recenterLadder) {

        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.recenterLadder(recenterLadder);
        }
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
        viewsByUser.get(request.username).forEach(ladderView -> ladderView.switchContract(request));
    }

    @Subscribe
    public void on(final OrderEntryClient.SymbolOrderChannel symbolOrderChannel) {
        orderEntryMap.put(symbolOrderChannel.symbol, symbolOrderChannel);
    }

    public static class UpdateFromServerConverter implements Converter<UpdateFromServer, String> {

        @Override
        public String convert(final UpdateFromServer msg) {
            return msg.key;
        }
    }

    @KeyedBatchSubscriber(converter = UpdateFromServerConverter.class, flushInterval = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Subscribe
    public void onEeifOEUpdates(final Map<String, UpdateFromServer> updates) {
        for (final UpdateFromServer update : updates.values()) {
            eeifOrdersBySymbol.get(update.symbol).onUpdate(update);
        }
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

    @Subscribe
    public void on(final SymbolDescription description) {
        symbolDesc.put(description.symbol, description.description);
    }

    public long flushAllLadders() {
        try {
            for (final LadderView ladderView : viewBySocket.values()) {
                ladderView.timedRefresh();
            }
            monitor.setOK(ReddalComponents.LADDER_PRESENTER);
        } catch (final Throwable t) {
            monitor.logError(ReddalComponents.LADDER_PRESENTER, "Failed to flush.", t);
        }
        return BATCH_FLUSH_INTERVAL_MS;
    }

    public long sendAllHeartbeats() {
        for (final LadderView ladderView : viewBySocket.values()) {
            ladderView.sendHeartbeat();
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
