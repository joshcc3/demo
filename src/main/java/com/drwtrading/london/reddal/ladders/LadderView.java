package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.UserCycleRequest;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.LastTradeDataForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiEventHandler;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.FreeTextCell;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.workspace.HostWorkspaceRequest;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.websockets.WebSocketClient;
import drw.eeif.fees.FeesCalc;
import drw.london.json.Jsonable;
import eeif.execution.Side;
import eeif.execution.WorkingOrderType;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class LadderView implements UiEventHandler {

    private static final DecimalFormat MILLIONS_QTY_FORMAT = NumberFormatUtil.getDF(".0");

    private static final AtomicLong heartbeatSeqNo = new AtomicLong(0L);

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int RECENTER_TIME_MS = 11000;
    private static final int RECENTER_WARN_TIME_MS = 9000;

    private static final int TAB = 9;
    private static final int PG_UP = 33;
    private static final int PG_DOWN = 34;
    private static final int END_KEY = 35;
    private static final int HOME_KEY = 36;

    static final Map<InstType, Map<String, Integer>> INST_TYPE_BUTTON_QTIES;

    static {
        INST_TYPE_BUTTON_QTIES = new EnumMap<>(InstType.class);

        final Map<String, Integer> defaultQties = new HashMap<>();
        defaultQties.put("btn_qty_1", 1);
        defaultQties.put("btn_qty_2", 5);
        defaultQties.put("btn_qty_3", 10);
        defaultQties.put("btn_qty_4", 50);
        defaultQties.put("btn_qty_5", 100);
        defaultQties.put("btn_qty_6", 500);
        for (final InstType instType : InstType.values()) {
            INST_TYPE_BUTTON_QTIES.put(instType, defaultQties);
        }

        final Map<String, Integer> futuresQties = new HashMap<>();
        futuresQties.put("btn_qty_1", 1);
        futuresQties.put("btn_qty_2", 5);
        futuresQties.put("btn_qty_3", 10);
        futuresQties.put("btn_qty_4", 25);
        futuresQties.put("btn_qty_5", 50);
        futuresQties.put("btn_qty_6", 100);
        INST_TYPE_BUTTON_QTIES.put(InstType.FUTURE, futuresQties);
        INST_TYPE_BUTTON_QTIES.put(InstType.FUTURE_SPREAD, futuresQties);

        final Map<String, Integer> fxQties = new HashMap<>();
        fxQties.put("btn_qty_1", 100000);
        fxQties.put("btn_qty_2", 200000);
        fxQties.put("btn_qty_3", 500000);
        fxQties.put("btn_qty_4", 1000000);
        fxQties.put("btn_qty_5", 2500000);
        fxQties.put("btn_qty_6", 5000000);
        INST_TYPE_BUTTON_QTIES.put(InstType.FX, fxQties);

        final Map<String, Integer> equityQties = new HashMap<>();
        equityQties.put("btn_qty_1", 1);
        equityQties.put("btn_qty_2", 10);
        equityQties.put("btn_qty_3", 100);
        equityQties.put("btn_qty_4", 1000);
        equityQties.put("btn_qty_5", 5000);
        equityQties.put("btn_qty_6", 10000);
        INST_TYPE_BUTTON_QTIES.put(InstType.DR, equityQties);
        INST_TYPE_BUTTON_QTIES.put(InstType.EQUITY, equityQties);
        INST_TYPE_BUTTON_QTIES.put(InstType.ETF, equityQties);
    }

    private final IResourceMonitor<ReddalComponents> monitor;

    private final WebSocketClient client;
    private final ILadderUI view;
    private final String ewokBaseURL;
    private final Publisher<RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher;
    private final LadderOptions ladderOptions;
    private final FXCalc<?> fxCalc;
    private final FeesCalc feesCalc;
    private final DecimalFormat feeDF;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<Jsonable> trace;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap;
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer;
    private final UiPipeImpl ui;
    private final TradingStatusForAll tradingStatusForAll;
    private final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Publisher<HostWorkspaceRequest> userWorkspaceRequests;
    private final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher;
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher;
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher;
    private final Predicate<String> symbolExists;
    private final Map<String, String> symbolDesc;
    private final LadderHTMLTable ladderHTMLKeys;
    private final DecimalFormat twoDF;

    public String symbol;
    private int levels;
    private MDForSymbol marketData;
    private long lastCenteredTime = 0;
    private LadderMetaData metaData;
    private LastTradeDataForSymbol extraDataForSymbol;
    private SymbolStackData stackData;

    private ClientSpeedState clientSpeedState = ClientSpeedState.FINE;

    private boolean showTotalTraded = false;

    private Long lastHeartbeatSentMillis = null;
    private long lastHeartbeatRoundTripMillis = 0;

    private LadderBookView bookView;
    private ILadderBoard stackView;
    private ILadderBoard activeView;
    private Set<String> isinsGoingEx;
    private GoingExState exState = GoingExState.Unknown;

    LadderView(final IResourceMonitor<ReddalComponents> monitor, final WebSocketClient client, final UiPipeImpl ui, final ILadderUI view,
               final String ewokBaseURL, final Publisher<RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher,
               final LadderOptions ladderOptions, final FXCalc<?> fxCalc, final FeesCalc feesCalc, final DecimalFormat feeDF,
               final TradingStatusForAll tradingStatusForAll, final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher,
               final Publisher<RecenterLaddersForUser> recenterLaddersForUser, final Publisher<Jsonable> trace,
               final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher,
               final Publisher<UserCycleRequest> userCycleContractPublisher, final Publisher<HostWorkspaceRequest> userWorkspaceRequests,
               final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap,
               final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher,
               final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher,
               final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher,
               final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher, final Predicate<String> symbolExists,
               final Map<String, String> symbolDesc) {

        this.monitor = monitor;

        this.client = client;
        this.view = view;
        this.ewokBaseURL = ewokBaseURL;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.ladderOptions = ladderOptions;
        this.fxCalc = fxCalc;
        this.feesCalc = feesCalc;
        this.feeDF = feeDF;

        this.recenterLaddersForUser = recenterLaddersForUser;
        this.trace = trace;
        this.ladderClickTradingIssuePublisher = ladderClickTradingIssuePublisher;
        this.orderEntryMap = orderEntryMap;
        this.eeifCommandToServer = orderEntryCommandToServerPublisher;
        this.increaseParentOffsetPublisher = increaseParentOffsetPublisher;
        this.increaseChildOffsetCmdPublisher = increaseChildOffsetCmdPublisher;
        this.disableSiblingsCmdPublisher = disableSiblingsCmdPublisher;
        this.ui = ui;
        this.tradingStatusForAll = tradingStatusForAll;
        this.heartbeatRoundTripPublisher = heartbeatRoundTripPublisher;
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.userWorkspaceRequests = userWorkspaceRequests;
        this.symbolExists = symbolExists;
        this.symbolDesc = symbolDesc;
        this.ladderHTMLKeys = new LadderHTMLTable();
        this.twoDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2);

        this.ui.setHandler(this);
        this.activeView = LadderNoView.SINGLETON;
    }

    void replaceSymbol(final ReplaceCommand replaceCommand) {
        if (null != view) {
            view.replace(replaceCommand.from, replaceCommand.to);
        }
    }

    void subscribeToSymbol(final String symbol, final int levels, final MDForSymbol marketData,
            final WorkingOrdersForSymbol workingOrdersForSymbol, final LadderMetaData metaData,
            final LastTradeDataForSymbol extraDataForSymbol, final SymbolStackData stackData,
            final LadderPrefsForSymbolUser ladderPrefsForSymbolUser, final OrderUpdatesForSymbol orderUpdatesForSymbol) {

        this.symbol = symbol;
        this.levels = levels;
        this.ladderHTMLKeys.extendToLevels(levels);
        this.marketData = marketData;
        this.metaData = metaData;
        this.extraDataForSymbol = extraDataForSymbol;
        this.stackData = stackData;

        final boolean wasBookView = null == bookView || activeView == bookView;

        final long bookCenteredPrice = null == bookView ? 0 : bookView.getCenteredPrice();
        this.bookView =
                new LadderBookView(monitor, client.getUserName(), isTrader(), symbol, ui, view, ladderOptions, fxCalc, feesCalc, feeDF,
                        ladderPrefsForSymbolUser, ladderClickTradingIssuePublisher, remoteOrderCommandToServerPublisher,
                        eeifCommandToServer, tradingStatusForAll, marketData, workingOrdersForSymbol, extraDataForSymbol,
                        orderUpdatesForSymbol, levels, ladderHTMLKeys, stackData, metaData, increaseParentOffsetPublisher,
                        increaseChildOffsetCmdPublisher, disableSiblingsCmdPublisher, trace, orderEntryMap, bookCenteredPrice);

        final IBook<?> book = marketData.getBook();
        final Map<String, Integer> buttonQties;
        if (null == book) {
            buttonQties = INST_TYPE_BUTTON_QTIES.get(InstType.UNKNOWN);
        } else {
            buttonQties = INST_TYPE_BUTTON_QTIES.get(book.getInstType());
        }

        this.stackView =
                new LadderStackView(client.getUserName(), isTrader(), symbol, buttonQties, levels, ladderHTMLKeys, stackData, metaData,
                        increaseParentOffsetPublisher, increaseChildOffsetCmdPublisher, disableSiblingsCmdPublisher, ui, view,
                        ladderPrefsForSymbolUser, marketData);

        if (wasBookView) {
            setBookView();
        } else {
            setStackView();
        }
    }

    private void cycleView() {

        if (stackView == activeView) {
            setBookView();
        } else {
            setStackView();
        }
    }

    private void setBookView() {

        activeView = bookView;
        refreshAll();
    }

    void setStackView() {

        activeView = stackView;
        refreshAll();
    }

    private void refreshAll() {

        initialSetup();
        timedRefresh();
        sendHeartbeat();
    }

    private void initialSetup() {

        ui.clear();

        for (final CSSClass type : CSSClass.values()) {
            ui.cls(HTML.ORDER_TYPE_LEFT, type, false);
            ui.cls(HTML.ORDER_TYPE_RIGHT, type, false);
            ui.cls(HTML.WORKING_ORDER_TAG, type, false);
        }

        view.draw(levels);
        ui.txt(HTML.SYMBOL, symbol);
        view.setDescription(symbolDesc.getOrDefault(symbol, symbol));

        ui.cls(HTML.LASER + "BID", CSSClass.INVISIBLE, true);
        ui.cls(HTML.LASER + "GREEN", CSSClass.INVISIBLE, true);
        ui.cls(HTML.LASER + "WHITE", CSSClass.INVISIBLE, true);
        ui.cls(HTML.LASER + "ASK", CSSClass.INVISIBLE, true);

        activeView.switchedTo();

        ui.clickable('#' + HTML.SYMBOL);
        ui.clickable('#' + HTML.BOOK_VIEW_BUTTON);
        ui.clickable('#' + HTML.STACK_VIEW_BUTTON);
        ui.clickable('#' + HTML.CLOCK);
        ui.clickable('#' + HTML.POSITION);
        ui.clickable('#' + HTML.TOTAL_TRADED);
        ui.clickable('#' + HTML.AFTER_HOURS_WEIGHT);

        for (int i = 0; i < levels; i++) {
            ui.clickable('#' + HTML.PRICE + i);
        }
        ui.clickable(HTML.BUTTONS);
        ui.scrollable('#' + HTML.LADDER);

        ui.clickable('#' + HTML.TEXT_PREFIX + HTML.R1C2);
        ui.clickable('#' + HTML.TEXT_PREFIX + HTML.R1C3);
        ui.clickable('#' + HTML.TEXT_PREFIX + HTML.R1C4);

        final boolean isTrader = isTrader();
        if (isTrader) {

            for (int i = 0; i < levels; i++) {
                ui.clickable('#' + HTML.BID + i);
                ui.clickable('#' + HTML.OFFER + i);
                ui.clickable('#' + HTML.ORDER + i);

                ui.clickable('#' + HTML.STACK_BID_PICARD + i);
                ui.clickable('#' + HTML.STACK_BID_QUOTE + i);
                ui.clickable('#' + HTML.STACK_BID_OFFSET + i);

                ui.clickable('#' + HTML.STACK_ASK_PICARD + i);
                ui.clickable('#' + HTML.STACK_ASK_QUOTE + i);
                ui.clickable('#' + HTML.STACK_ASK_OFFSET + i);
            }

            ui.clickable('#' + HTML.BUY_QTY);
            ui.clickable('#' + HTML.SELL_QTY);
            ui.clickable('#' + HTML.BUY_OFFSET_UP);
            ui.clickable('#' + HTML.BUY_OFFSET_DOWN);
            ui.clickable('#' + HTML.SELL_OFFSET_UP);
            ui.clickable('#' + HTML.SELL_OFFSET_DOWN);
            ui.clickable('#' + HTML.START_BUY);
            ui.clickable('#' + HTML.STOP_BUY);
            ui.clickable('#' + HTML.START_SELL);
            ui.clickable('#' + HTML.STOP_SELL);

            ui.cls(HTML.OFFSET_CONTROL, CSSClass.INVISIBLE, false);
        }
    }

    void timedRefresh() {

        activeView.timedRefresh();
        recenterIfTimeoutElapsed();
        flushDynamicFeatures();
    }

    private void flushDynamicFeatures() {

        checkClientSpeed();
        drawClock();

        drawMetaData();

        activeView.refresh(getSymbol());
        ui.flush();
    }

    void fastInputFlush() {

        activeView.refresh(getSymbol());
        ui.flush();
    }

    private void drawClock() {
        ui.txt(HTML.CLOCK, SIMPLE_DATE_FORMAT.format(new Date()));
        ui.cls(HTML.CLOCK, CSSClass.SLOW, clientSpeedState == ClientSpeedState.SLOW);
        ui.cls(HTML.CLOCK, CSSClass.VERY_SLOW, clientSpeedState == ClientSpeedState.TOO_SLOW);
    }

    void clickTradingIssue(final LadderClickTradingIssue issue) {
        clickTradingIssue(ui, issue);
    }

    static void clickTradingIssue(final UiPipeImpl ui, final LadderClickTradingIssue issue) {
        ui.txt(HTML.CLICK_TRADING_ISSUES, issue.issue);
    }

    static void decorateUpDown(final UiPipeImpl ui, final String key, final Long value) {
        if (null != value) {
            ui.cls(key, CSSClass.POSITIVE, 0 < value);
            ui.cls(key, CSSClass.NEGATIVE, value < 0);
        }
    }

    private String getSymbol() {
        if (null != metaData && null != metaData.displaySymbol) {
            return metaData.displaySymbol;
        } else {
            return symbol;
        }
    }

    private void drawMetaData() {

        ui.cls(HTML.BOOK_VIEW_BUTTON, CSSClass.ACTIVE_MODE, activeView == bookView);
        ui.cls(HTML.STACK_VIEW_BUTTON, CSSClass.ACTIVE_MODE, activeView == stackView);

        final IBook<?> book = marketData.getBook();
        if (null != book) {
            ui.cls(HTML.SYMBOL, CSSClass.REVERSE_SPREAD,
                    InstType.FUTURE_SPREAD == book.getInstType() && book.getMIC().exchange.isReverseSpreadVenue);
        }

        if (null != metaData && null != extraDataForSymbol) {
            if (metaData.spreadContractSet != null) {
                ui.cls(HTML.SYMBOL, CSSClass.SPREAD, symbol.equals(metaData.spreadContractSet.contractAfterNext));
                ui.cls(HTML.SYMBOL, CSSClass.BACK, symbol.equals(metaData.spreadContractSet.nextContract));
            }
            // Desk position
            if (null != metaData.formattedDeskPosition) {
                ui.txt(HTML.DESK_POSITION, metaData.formattedDeskPosition);
                decorateUpDown(ui, HTML.DESK_POSITION, metaData.deskPosition);
            }
            // Day position
            if (null != metaData.formattedMrPhilNetPosition) {
                decorateUpDown(ui, HTML.POSITION, metaData.mrPhilNetPosition);
                ui.txt(HTML.POSITION, metaData.formattedMrPhilNetPosition);
                ui.txt(HTML.TOTAL_TRADED, metaData.formattedMrPhilVolume);

                ui.cls(HTML.POSITION, CSSClass.INVISIBLE, showTotalTraded);
                ui.cls(HTML.TOTAL_TRADED, CSSClass.INVISIBLE, !showTotalTraded);
            }

            if (null != metaData.pksExposure) {
                ui.txt(HTML.PKS_EXPOSURE, metaData.pksExposure);
                ui.txt(HTML.PKS_POSITION, metaData.pksPosition);
            }

            // Ladder text
            for (final Map.Entry<FreeTextCell, String> ladderText : metaData.freeTextCells.entrySet()) {

                final FreeTextCell cell = ladderText.getKey();
                final String text = ladderText.getValue();
                ui.txt(cell.htmlID, text);
            }

            setCellTest(HTML.BID_BEST_OFFSET_BPS, stackData.getBidTopOrderOffsetBPS());
            setCellTest(HTML.ASK_BEST_OFFSET_BPS, stackData.getAskTopOrderOffsetBPS());

            final TheoValue theoValue = stackData.getTheoValue();
            if (null != theoValue) {

                if (theoValue.isValid()) {
                    if (theoValue.getAfterHoursPct() < Constants.EPSILON) {
                        ui.txt(HTML.AFTER_HOURS_WEIGHT, "0");
                    } else {
                        ui.txt(HTML.AFTER_HOURS_WEIGHT, Math.ceil(theoValue.getAfterHoursPct()));
                    }
                    ui.txt(HTML.TEXT_PREFIX + "r2c5", (int) Math.ceil(theoValue.getRawAfterHoursPct()));
                } else {
                    ui.txt(HTML.AFTER_HOURS_WEIGHT, "XXX");
                    ui.txt(HTML.TEXT_PREFIX + "r2c5", "XXX");
                }
            }

            // Going ex
            checkGoingEx();
            ui.cls(HTML.TEXT, CSSClass.GOING_EX, exState == GoingExState.YES);
        }

        final double uppyDownyValue = stackData.getPriceOffsetTickSize();
        if (0 < uppyDownyValue) {
            final String bps = twoDF.format(uppyDownyValue) + "bps";
            ui.tooltip('#' + HTML.BUY_OFFSET_UP, bps);
            ui.tooltip('#' + HTML.SELL_OFFSET_UP, bps);
            ui.tooltip('#' + HTML.BUY_OFFSET_DOWN, '-' + bps);
            ui.tooltip('#' + HTML.SELL_OFFSET_DOWN, '-' + bps);
        } else {
            ui.tooltip('#' + HTML.BUY_OFFSET_UP, "");
            ui.tooltip('#' + HTML.SELL_OFFSET_UP, "");
            ui.tooltip('#' + HTML.BUY_OFFSET_DOWN, "");
            ui.tooltip('#' + HTML.SELL_OFFSET_DOWN, "");
        }
    }

    private void setCellTest(final String cellID, final double value) {

        if (Double.isNaN(value)) {
            ui.txt(cellID, "---");
        } else {
            ui.txt(cellID, twoDF.format(value));

        }
    }

    private void recenterIfTimeoutElapsed() {

        final long milliSinceLastCentered = System.currentTimeMillis() - lastCenteredTime;

        if (!activeView.canMoveTowardsCenter() || (RECENTER_TIME_MS < milliSinceLastCentered && activeView.moveTowardsCenter())) {
            resetLastCenteredTime();
        }
        ui.cls(HTML.LADDER, CSSClass.RECENTERING, 0 < lastCenteredTime && RECENTER_WARN_TIME_MS <= milliSinceLastCentered);
    }

    void recenterLadderForUser(final RecenterLaddersForUser recenterLaddersForUser) {

        if (client.getUserName().equals(recenterLaddersForUser.user)) {
            activeView.center();
            resetLastCenteredTime();
        }
    }

    void recenterLadder(final RecenterLadder recenterLadder) {

        if (client.getUserName().equals(recenterLadder.username) && symbol.equals(recenterLadder.symbol)) {

            activeView.setCenteredPrice(recenterLadder.price);
            resetLastCenteredTime();
        }
    }

    void setCenterPrice(final long price) {
        activeView.setCenteredPrice(price);
        resetLastCenteredTime();
    }

    private void resetLastCenteredTime() {
        lastCenteredTime = System.currentTimeMillis();
    }

    // Inbound

    void onRawInboundData(final String data) {
        ui.onInbound(data);
    }

    @Override
    public void onUpdate(final String label, final Map<String, String> dataArg) {

        String value = dataArg.get("value");
        if (value != null) {
            value = value.trim();
            if (HTML.INP_QTY.equals(label)) {
                activeView.setTradingBoxQty(Integer.valueOf(value));
            } else if (HTML.STACK_TICK_SIZE.equals(label)) {
                activeView.setStackTickSize(Double.valueOf(value));
            } else if (HTML.STACK_ALIGNMENT_TICK_TO_BPS.equals(label)) {
                activeView.setStackAlignmentTickToBPS(Double.valueOf(value));
            } else if (!activeView.setPersistencePreference(label, value)) {
                throw new IllegalArgumentException("Update for unknown value: " + label + ' ' + dataArg);
            }
        }
        flushDynamicFeatures();
    }

    @Override
    public void onScroll(final String direction) {

        resetLastCenteredTime();
        if ("up".equals(direction)) {
            activeView.scrollUp();
        } else if ("down".equals(direction)) {
            activeView.scrollDown();
        }
    }

    @Override
    public void onKeyDown(final int keyCode) {

        resetLastCenteredTime();
        switch (keyCode) {
            case TAB: {
                cycleView();
                break;
            }
            case PG_UP: {
                activeView.pageUp();
                break;
            }
            case PG_DOWN: {
                activeView.pageDown();
                break;
            }
            case HOME_KEY: {
                activeView.setBestAskCenter();
                break;
            }
            case END_KEY: {
                activeView.setBestBidCenter();
                break;
            }
        }
    }

    @Override
    public void onHeartbeat(final long sentTimeMillis) {

        final long returnTimeMillis = System.currentTimeMillis();
        if (lastHeartbeatSentMillis == sentTimeMillis) {
            lastHeartbeatSentMillis = null;
            lastHeartbeatRoundTripMillis = returnTimeMillis - sentTimeMillis;
            heartbeatRoundTripPublisher.publish(
                    new HeartbeatRoundtrip(client.getUserName(), symbol, sentTimeMillis, returnTimeMillis, lastHeartbeatRoundTripMillis));
        } else {
            throw new RuntimeException(
                    "Received heartbeat reply " + sentTimeMillis + " which does not match last sent heartbeat " + lastHeartbeatSentMillis);
        }
    }

    @Override
    public void onDblClick(final String label, final Map<String, String> dataArg) {

        resetLastCenteredTime();
        if (HTML.BUY_QTY.equals(label)) {
            activeView.cancelAllForSide(BookSide.BID);
        } else if (HTML.SELL_QTY.equals(label)) {
            activeView.cancelAllForSide(BookSide.ASK);
        } else if (HTML.STACK_TICK_SIZE.equals(label)) {
            activeView.setStackTickSizeToMatchQuote();
        }
        flushDynamicFeatures();
    }

    @Override
    public void onClick(final String label, final Map<String, String> data) {

        resetLastCenteredTime();
        final String button = data.get("button");
        if (label.startsWith(HTML.SYMBOL)) {
            switch (button) {
                case "left": {
                    nextContract();
                    break;
                }
                case "right": {
                    if (label.startsWith(HTML.SYMBOL) && null != ladderOptions.basketUrl) {
                        view.goToUrl(ladderOptions.basketUrl + '#' + symbol);
                    }
                    break;
                }
                case "middle": {
                    if (label.startsWith(HTML.SYMBOL)) {
                        final UserCycleRequest cycleRequest = new UserCycleRequest(client.getUserName());
                        userCycleContractPublisher.publish(cycleRequest);
                        return;
                    }
                    break;
                }
            }
        } else if (label.equals(HTML.CLOCK)) {
            if ("left".equals(button)) {
                switchChixSymbol();
            }
        } else if (label.equals(HTML.BOOK_VIEW_BUTTON)) {
            switch (button) {
                case "right": {
                    userWorkspaceRequests.publish(new HostWorkspaceRequest(client.getHost(), symbol));
                    break;
                }
                default: {
                    setBookView();
                    break;
                }
            }
        } else if (label.equals(HTML.STACK_VIEW_BUTTON)) {
            switch (button) {
                case "right": {

                    final String popUpSymbol;
                    if (null != metaData && null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                        popUpSymbol = metaData.spreadContractSet.parentSymbol;
                    } else {
                        popUpSymbol = symbol + ";S";
                    }

                    if (popUpSymbol.equals(symbol + ";S")) {
                        setStackView();
                    } else {
                        view.popUp("/ladder#" + popUpSymbol, symbol + " stack", 245, 600);
                    }
                    break;
                }
                default: {
                    setStackView();
                    break;
                }
            }
        } else if (label.equals(HTML.AFTER_HOURS_WEIGHT)) {
            if ("left".equals(button)) {
                openEwokView();
            }
        } else if ("middle".equals(button) &&
                (label.startsWith(HTML.PRICE) || label.startsWith(HTML.STACK_BID_OFFSET) || label.startsWith(HTML.STACK_ASK_OFFSET) ||
                        label.startsWith(HTML.STACK_DIVIDER))) {

            recenterLaddersForUser.publish(new RecenterLaddersForUser(client.getUserName()));

        } else if (label.equals(HTML.POSITION) || label.equals(HTML.TOTAL_TRADED)) {
            showTotalTraded = !showTotalTraded;
        } else if (label.equals(HTML.TEXT_PREFIX + HTML.R1C2) || label.equals(HTML.TEXT_PREFIX + HTML.R1C3) ||
                label.equals(HTML.TEXT_PREFIX + HTML.R1C4)) {
            final Contract contract;
            switch (label.substring(HTML.TEXT_PREFIX.length())) {
                case HTML.R1C2:
                    contract = Contract.FRONT;
                    break;
                case HTML.R1C3:
                    contract = Contract.BACK;
                    break;
                case HTML.R1C4:
                    contract = Contract.SPREAD;
                    break;
                default:
                    contract = null;
            }

            final UserCycleRequest cycleRequest = new UserCycleRequest(client.getUserName(), contract);
            userCycleContractPublisher.publish(cycleRequest);
            return;
        } else {

            try {
                activeView.onClick(clientSpeedState, label, button, data);
            } catch (final Exception e) {
                ladderClickTradingIssuePublisher.publish(new LadderClickTradingIssue(symbol, "Active view [" + e.getMessage() + "]."));
                e.printStackTrace();
            }
        }

        flushDynamicFeatures();
    }

    boolean switchContract(final UserCycleRequest cycleRequest) {
        if (null != metaData && null != metaData.spreadContractSet) {

            if (cycleRequest.contract != null) {
                final SpreadContractSet contracts = metaData.spreadContractSet;
                switch (cycleRequest.contract) {
                    case FRONT:
                        return goToContract(contracts.frontMonth);
                    case BACK:
                        return goToContract(contracts.backMonth);
                    case SPREAD:
                        return goToContract(contracts.spread);
                    default:
                        return false;
                }
            } else {
                return nextContract();
            }
        } else {
            return false;
        }
    }

    private boolean nextContract() {
        final SpreadContractSet contracts = metaData.spreadContractSet;
        String nextContract = contracts.next(symbol);
        int count = 3;
        while (!symbolExists.test(nextContract) && 0 < count--) {
            nextContract = contracts.next(nextContract);
        }
        return goToContract(nextContract);
    }

    private boolean goToContract(final String targetSymbol) {
        if (targetSymbol != null && !symbol.equals(targetSymbol) && symbolExists.test(targetSymbol)) {
            view.goToSymbol(targetSymbol);
            return true;
        } else {
            return false;
        }
    }

    private boolean switchChixSymbol() {
        if (null != metaData && null != metaData.chixSwitchSymbol && symbolExists.test(metaData.chixSwitchSymbol)) {
            view.goToSymbol(metaData.chixSwitchSymbol);
            return true;
        } else {
            return false;
        }
    }

    private boolean openEwokView() {
        if (null != marketData && null != marketData.getBook()) {
            switch (marketData.getBook().getInstType()) {
                case FUTURE: {
                    return popupFuture(marketData.getBook().getSymbol());
                }
                case FUTURE_SPREAD: {
                    return popupFuture(marketData.getBook().getSymbol().split("-")[0]);
                }
                case EQUITY:
                case ETF:
                case DR:
                case INDEX: {
                    final String symbol = marketData.getBook().getSymbol();
                    final String url = ewokBaseURL + "/smart#" + symbol;
                    view.popUp(url, "Ewok " + symbol, 1200, 800);
                    return true;
                }
                default: {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean popupFuture(final String expiry) {
        final FutureConstant future = FutureConstant.getFutureFromSymbol(expiry);
        if (null != future) {
            final String url = ewokBaseURL + "/smart#" + future.index.name();
            view.popUp(url, "Ewok " + future.index.name(), 1200, 800);
            return true;
        } else {
            return false;
        }
    }

    static BookSide convertSide(final Side s1) {
        if (s1 == Side.BID) {
            return BookSide.BID;
        } else {
            return BookSide.ASK;
        }
    }

    private boolean isTrader() {
        return ladderOptions.traders.contains(client.getUserName());
    }

    static String getOrderType(final WorkingOrderType workingOrderType) {
        if (workingOrderType == WorkingOrderType.MARKET) {
            return "MKT_CLOSE";
        } else {
            return workingOrderType.name();
        }
    }

    void onSingleOrderCommand(final OrdersPresenter.SingleOrderCommand singleOrderCommand) {

        bookView.onSingleOrderCommand(clientSpeedState, singleOrderCommand);
    }

    // Heartbeats

    void sendHeartbeat() {
        if (lastHeartbeatSentMillis == null) {
            lastHeartbeatSentMillis = System.currentTimeMillis();
            ui.send(UiPipeImpl.cmd("heartbeat", lastHeartbeatSentMillis, heartbeatSeqNo.getAndIncrement()));
        }
    }

    private void checkClientSpeed() {

        final long clientSpeed = Math.max(null == lastHeartbeatSentMillis ? 0L : System.currentTimeMillis() - lastHeartbeatSentMillis,
                lastHeartbeatRoundTripMillis);
        if (ClientSpeedState.TOO_SLOW.thresholdMillis < clientSpeed) {
            clientSpeedState = ClientSpeedState.TOO_SLOW;
        } else if (ClientSpeedState.SLOW.thresholdMillis < clientSpeed) {
            clientSpeedState = ClientSpeedState.SLOW;
        } else {
            clientSpeedState = ClientSpeedState.FINE;
        }
    }

    static String formatClickQty(final Integer qty) {

        if (qty < 1000) {
            return Integer.toString(qty);
        } else if (qty < 100000) {
            return qty / 1000 + "K";
        } else {
            return MILLIONS_QTY_FORMAT.format(qty / 1000000d) + 'M';
        }
    }

    void setIsinsGoingEx(final Set<String> isinsGoingEx) {
        this.isinsGoingEx = isinsGoingEx;
        this.exState = GoingExState.Unknown;
        checkGoingEx();
    }

    private void checkGoingEx() {

        if (this.exState != GoingExState.Unknown) {
            return;
        }
        GoingExState exState = GoingExState.Unknown;
        if (null != isinsGoingEx && null != marketData && null != marketData.getBook()) {
            if (isinsGoingEx.contains(marketData.getBook().getISIN())) {
                exState = GoingExState.YES;
            } else {
                exState = GoingExState.NO;
            }
        }
        this.exState = exState;
    }

    enum GoingExState {
        YES,
        NO,
        Unknown
    }

}
