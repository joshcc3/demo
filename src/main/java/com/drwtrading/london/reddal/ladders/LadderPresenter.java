package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.KeyedBatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TradableInstrument;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.UserCycleRequest;
import com.drwtrading.london.reddal.data.InstrumentMetaData;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.LaserLine;
import com.drwtrading.london.reddal.data.NibblerLastTradeDataForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsPref;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsPrefLoaded;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsStoreLadderPref;
import com.drwtrading.london.reddal.opxl.ISINsGoingEx;
import com.drwtrading.london.reddal.opxl.LadderTextUpdate;
import com.drwtrading.london.reddal.opxl.OPXLDeskPositions;
import com.drwtrading.london.reddal.orderManagement.NibblerTransportConnected;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntrySymbolChannel;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.picard.IPicardSpotter;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.pks.PKSExposures;
import com.drwtrading.london.reddal.premium.IPremiumCalc;
import com.drwtrading.london.reddal.stacks.IStackPresenterCallback;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.symbols.ChixSymbolPair;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.symbols.SymbolIndyData;
import com.drwtrading.london.reddal.trades.MrChillTrade;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByPrice;
import com.drwtrading.london.reddal.workspace.HostWorkspaceRequest;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import drw.eeif.fees.FeesCalc;
import drw.eeif.photons.mrchill.Position;
import org.jetlang.channels.Converter;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LadderPresenter implements IStackPresenterCallback {

    private static final InstrumentMetaData EMPTY_META_DATA = new InstrumentMetaData(new InstrumentID("000000000000", CCY.ZAR, MIC.XOFF));

    private static final long BATCH_FLUSH_INTERVAL_MS = 1000 / 5;
    private static final long HEARTBEAT_INTERVAL_MS = 1000;

    private final IFuseBox<ReddalComponents> monitor;
    private final IMDSubscriber bookSubscriber;
    private final String ewokBaseURL;

    private final Publisher<IOrderCmd> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;

    private final FXCalc<?> fxCalc;
    private final FeesCalc feesCalc;
    private final DecimalFormat feeDF;

    private final DecimalFormat oneDP;

    private final Map<Publisher<WebSocketOutboundData>, LadderView> viewBySocket = new HashMap<>();
    private final Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, LadderView> viewsByUser = HashMultimap.create();
    private final Map<String, Set<OrderType>> supportedOrderTypesBySymbol =
            new MapMaker().makeComputingMap(symbol -> EnumSet.noneOf(OrderType.class));
    private final Map<String, Set<AlgoType>> supportedAlgoTypesBySymbol =
            new MapMaker().makeComputingMap(symbol -> EnumSet.noneOf(AlgoType.class));
    private final Map<String, WorkingOrdersByPrice> ordersBySymbol = new MapMaker().makeComputingMap(symbol -> new WorkingOrdersByPrice());
    private final Map<String, OrderUpdatesForSymbol> eeifOrdersBySymbol = new MapMaker().makeComputingMap(OrderUpdatesForSymbol::new);
    private final Map<String, NibblerLastTradeDataForSymbol> lastTradeBySymbolForNibbler =
            new MapMaker().makeComputingMap(NibblerLastTradeDataForSymbol::new);
    private final Map<String, JasperLastTradeDataForSymbol> lastTradeBySymbolForJasper =
            new MapMaker().makeComputingMap(JasperLastTradeDataForSymbol::new);
    private final Map<String, LadderMetaData> metaDataBySymbol = new MapMaker().makeComputingMap(LadderMetaData::new);
    private final Map<InstrumentID, InstrumentMetaData> instrumentMetaData = new MapMaker().makeComputingMap(InstrumentMetaData::new);

    private final Map<String, Map<String, LadderPrefsForSymbolUser>> ladderPrefsForUserBySymbol;
    private final Map<String, SymbolStackData> stackBySymbol;
    private final Map<String, OrderEntrySymbolChannel> orderEntryMap;
    private final Map<String, SearchResult> refData;

    private final TradingStatusForAll tradingStatusForAll = new TradingStatusForAll();
    private final Publisher<LadderSettingsStoreLadderPref> storeLadderPrefPublisher;
    private final Publisher<HeartbeatRoundtrip> roundTripPublisher;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher;
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher;
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher;

    private final Fiber fiber;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher;
    private final Publisher<HostWorkspaceRequest> userWorkspaceRequests;

    private Set<String> isinsGoingEx;

    public LadderPresenter(final IFuseBox<ReddalComponents> monitor, final IMDSubscriber bookSubscriber, final String ewokBaseURL,
            final Publisher<IOrderCmd> remoteOrderCommandByServer, final LadderOptions ladderOptions, final IPicardSpotter picardSpotter,
            final IPremiumCalc premiumCalc, final FXCalc<?> fxCalc, final Publisher<LadderSettingsStoreLadderPref> storeLadderPrefPublisher,
            final Publisher<HeartbeatRoundtrip> roundTripPublisher, final Publisher<RecenterLaddersForUser> recenterLaddersForUser,
            final Fiber fiber, final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher,
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

    public void addTradableInstrument(final TradableInstrument tradableInstrument) {

        final Set<OrderType> symbolOrderTypes = supportedOrderTypesBySymbol.get(tradableInstrument.getSymbol());
        symbolOrderTypes.addAll(tradableInstrument.getSupportedOrderTypes());

        final Set<AlgoType> symbolAlgoTypes = supportedAlgoTypesBySymbol.get(tradableInstrument.getSymbol());
        symbolAlgoTypes.addAll(tradableInstrument.getSupportedAlgoTypes());
    }

    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByPrice workingOrders = ordersBySymbol.get(workingOrder.order.getSymbol());
        workingOrders.setWorkingOrder(workingOrder);
    }

    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByPrice workingOrders = ordersBySymbol.get(workingOrder.order.getSymbol());
        workingOrders.removeWorkingOrder(workingOrder);
    }

    public void setLastTradeForNibbler(final LastTrade lastTrade) {

        final NibblerLastTradeDataForSymbol data = lastTradeBySymbolForNibbler.get(lastTrade.getSymbol());
        data.setLastTrade(lastTrade);
    }

    public void setLastTradeForJasper(final MrChillTrade lastTrade) {

        final JasperLastTradeDataForSymbol data = lastTradeBySymbolForJasper.get(lastTrade.symbol);
        data.setLastTrade(lastTrade);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {

        final UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        final ILadderUI view = new WebSocketOutputDispatcher<>(ILadderUI.class).wrap(msg -> uiPipe.eval(msg.getData()));
        final LadderView ladderView =
                new LadderView(monitor, connected.getClient(), uiPipe, view, ewokBaseURL, remoteOrderCommandByServer, ladderOptions, fxCalc,
                        feesCalc, feeDF, tradingStatusForAll, roundTripPublisher, recenterLaddersForUser, ladderClickTradingIssuePublisher,
                        userCycleContractPublisher, userWorkspaceRequests, orderEntryMap, orderEntryCommandToServerPublisher,
                        increaseParentOffsetPublisher, increaseChildOffsetCmdPublisher, disableSiblingsCmdPublisher, refData);

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

                final Set<OrderType> supportedOrderTypes = supportedOrderTypesBySymbol.get(symbol);
                final Set<AlgoType> supportedAlgoTypes = supportedAlgoTypesBySymbol.get(symbol);
                final WorkingOrdersByPrice workingOrders = ordersBySymbol.get(symbol);
                final LadderMetaData ladderMetaData = metaDataBySymbol.get(symbol);
                final NibblerLastTradeDataForSymbol lastTradeDataForNibbler = lastTradeBySymbolForNibbler.get(symbol);
                final JasperLastTradeDataForSymbol lastTradeDataForJasper = lastTradeBySymbolForJasper.get(symbol);
                final SymbolStackData stackData = stackBySymbol.get(symbol);
                final OrderUpdatesForSymbol orderUpdates = eeifOrdersBySymbol.get(symbol);

                final InstrumentMetaData instMetaData = getInstMetaDataForSymbol(mdForSymbol);

                final String userName = msg.getClient().getUserName();
                view.subscribeToSymbol(symbol, levels, supportedOrderTypes, mdForSymbol, workingOrders, ladderMetaData, instMetaData,
                        lastTradeDataForNibbler, lastTradeDataForJasper, stackData, getLadderPrefsForSymbolUser(symbol, userName),
                        orderUpdates);

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
    }

    private InstrumentMetaData getInstMetaDataForSymbol(final MDForSymbol mdForSymbol) {

        if (mdForSymbol.getBook() != null) {
            return instrumentMetaData.get(mdForSymbol.getBook().getInstID());
        } else {
            final SearchResult searchResult = refData.get(mdForSymbol.symbol);
            if (searchResult != null) {
                return instrumentMetaData.get(searchResult.instID);
            } else {
                return EMPTY_META_DATA;
            }
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

    public void overrideLaserLine(final LaserLine laserLine) {

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
        final CCY ccy = CCY.valueOf(position.getCcy());
        final MIC mic = MIC.valueOf(position.getMic());
        final InstrumentID instId = new InstrumentID(position.getIsin(), ccy, mic);

        final InstrumentMetaData metaData = instrumentMetaData.get(instId);
        metaData.setMrPhilPosition(oneDP, position);
    }

    public void setPKSExposures(final PKSExposures positions) {

        for (final PKSExposure position : positions.exposures) {
            for (final String symbol : position.symbols) {
                metaDataBySymbol.get(symbol).onPKSExposure(oneDP, position);
            }
        }
    }

    @Subscribe
    public void on(final LadderText ladderText) {
        if ("execution".equals(ladderText.getCell())) {
            displayTradeIssue(ladderText.getSymbol(), ladderText.getText());
        } else {
            metaDataBySymbol.get(ladderText.getSymbol()).onLadderText(ladderText);
        }
    }

    public void setLadderText(final Collection<LadderTextUpdate> ladderTexts) {

        for (final LadderTextUpdate ladderText : ladderTexts) {
            final LadderMetaData metaData = metaDataBySymbol.get(ladderText.symbol);
            metaData.setLadderText(ladderText);
        }
    }

    public void displayTradeIssue(final LadderClickTradingIssue issue) {
        displayTradeIssue(issue.symbol, issue.issue);
    }

    public void displayTradeIssue(final String symbol, final String text) {

        final Collection<LadderView> views = viewsBySymbol.get(symbol);
        for (final LadderView view : views) {
            view.clickTradingIssue(text);
            fiber.schedule(() -> view.clickTradingIssue(""), 5000, TimeUnit.MILLISECONDS);
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

    public void setNibblerConnected(final NibblerTransportConnected serverTradingStatus) {
        tradingStatusForAll.setNibblerConnected(serverTradingStatus);
    }

    @Subscribe
    public void on(final LadderSettingsPrefLoaded ladderPrefLoaded) {
        final LadderSettingsPref pref = ladderPrefLoaded.pref;
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
    public void on(final ISingleOrderCommand singleOrderCommand) {
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
    public void on(final OrderEntrySymbolChannel symbolOrderChannel) {
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

    public void setISINsGoingEx(final ISINsGoingEx isinsGoingEx) {
        this.isinsGoingEx = isinsGoingEx.isins;
        viewBySocket.values().forEach(ladderView -> ladderView.setIsinsGoingEx(isinsGoingEx.isins));
    }

    @Subscribe
    public void on(final SymbolIndyData indyData) {
        final InstrumentMetaData instrumentMetaData = this.instrumentMetaData.get(indyData.instrumentID);
        instrumentMetaData.setSymbolIndyData(indyData);
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
