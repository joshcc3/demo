package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.fastui.UiEventHandler;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.photons.reddal.CenterToPrice;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import eeif.execution.Side;
import eeif.execution.WorkingOrderType;
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
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderUpdatesForSymbol;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.websockets.WebSocketClient;
import drw.london.json.Jsonable;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class LadderView implements UiEventHandler {

    private static final DecimalFormat POSITION_FMT = NumberFormatUtil.getDF("0.0");
    private static final DecimalFormat MILLIONS_QTY_FORMAT = NumberFormatUtil.getDF(".0");

    public static final AtomicLong heartbeatSeqNo = new AtomicLong(0L);

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
        defaultQties.put("btn_qty_1", 0);
        defaultQties.put("btn_qty_2", 0);
        defaultQties.put("btn_qty_3", 0);
        defaultQties.put("btn_qty_4", 0);
        defaultQties.put("btn_qty_5", 0);
        defaultQties.put("btn_qty_6", 0);
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

    private final WebSocketClient client;
    private final ILadderUI view;
    private final String ewokBaseURL;
    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher;
    private final LadderOptions ladderOptions;
    private final Publisher<ReddalMessage> commandPublisher;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<Jsonable> trace;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap;
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer;
    private final UiPipeImpl ui;
    private final Publisher<StatsMsg> statsPublisher;
    private final TradingStatusForAll tradingStatusForAll;
    private final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Predicate<String> symbolExists;
    private final LadderHTMLTable ladderHTMLKeys;

    public String symbol;
    private int levels;
    private MDForSymbol marketData;
    private long lastCenteredTime = 0;
    private SymbolMetaData metaData;
    private ClientSpeedState clientSpeedState = ClientSpeedState.FINE;

    private boolean showTotalTraded = false;

    private Long lastHeartbeatSentMillis = null;
    private long lastHeartbeatRoundTripMillis = 0;

    private LadderBookView bookView;
    private ILadderBoard stackView;
    private ILadderBoard activeView;
    private OpxlExDateSubscriber.IsinsGoingEx isinsGoingEx;
    private GoingExState exState = GoingExState.Unknown;

    public LadderView(final WebSocketClient client, final UiPipeImpl ui, final ILadderUI view, final String ewokBaseURL,
                      final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher, final LadderOptions ladderOptions,
                      final Publisher<StatsMsg> statsPublisher, final TradingStatusForAll tradingStatusForAll,
                      final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher, final Publisher<ReddalMessage> commandPublisher,
                      final Publisher<RecenterLaddersForUser> recenterLaddersForUser, final Publisher<Jsonable> trace,
                      final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher,
                      final Publisher<UserCycleRequest> userCycleContractPublisher,
                      final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap,
                      final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher, final Predicate<String> symbolExists) {

        this.client = client;
        this.view = view;
        this.ewokBaseURL = ewokBaseURL;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.ladderOptions = ladderOptions;
        this.commandPublisher = commandPublisher;
        this.recenterLaddersForUser = recenterLaddersForUser;
        this.trace = trace;
        this.ladderClickTradingIssuePublisher = ladderClickTradingIssuePublisher;
        this.orderEntryMap = orderEntryMap;
        this.eeifCommandToServer = orderEntryCommandToServerPublisher;
        this.ui = ui;
        this.statsPublisher = statsPublisher;
        this.tradingStatusForAll = tradingStatusForAll;
        this.heartbeatRoundTripPublisher = heartbeatRoundTripPublisher;
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.symbolExists = symbolExists;
        this.ladderHTMLKeys = new LadderHTMLTable();
        this.ui.setHandler(this);

        this.activeView = LadderNoView.SINGLETON;
    }

    public void replaceSymbol(final ReplaceCommand replaceCommand) {
        if (null != view) {
            System.out.println("Replacing: " + symbol + " -> " + symbol.replace(replaceCommand.from, replaceCommand.to));
            view.replace(replaceCommand.from, replaceCommand.to);
        }
    }

    public void subscribeToSymbol(final String symbol, final int levels, final MDForSymbol marketData,
                                  final WorkingOrdersForSymbol workingOrdersForSymbol, final SymbolMetaData metaData, final ExtraDataForSymbol extraDataForSymbol,
                                  final SymbolStackData stackData, final LadderPrefsForSymbolUser ladderPrefsForSymbolUser,
                                  final OrderUpdatesForSymbol orderUpdatesForSymbol) {

        this.symbol = symbol;
        this.levels = levels;
        this.ladderHTMLKeys.extendToLevels(levels);
        this.marketData = marketData;
        this.metaData = metaData;

        final boolean wasBookView = null == bookView || activeView == bookView;

        final long bookCenteredPrice = null == bookView ? 0 : bookView.getCenteredPrice();
        this.bookView = new LadderBookView(client.getUserName(), isTrader(), symbol, ui, view, ladderOptions, ladderPrefsForSymbolUser,
                ladderClickTradingIssuePublisher, commandPublisher, statsPublisher, remoteOrderCommandToServerPublisher,
                eeifCommandToServer, tradingStatusForAll, marketData, workingOrdersForSymbol, extraDataForSymbol, orderUpdatesForSymbol,
                levels, ladderHTMLKeys, trace, orderEntryMap, bookCenteredPrice);

        final IBook<?> book = marketData.getBook();
        final Map<String, Integer> buttonQties;
        if (null == book) {
            buttonQties = INST_TYPE_BUTTON_QTIES.get(InstType.UNKNOWN);
        } else {
            buttonQties = INST_TYPE_BUTTON_QTIES.get(book.getInstType());
        }

        this.stackView =
                new LadderStackView(client.getUserName(), isTrader(), symbol, buttonQties, levels, ladderHTMLKeys, stackData, ui, view,
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

        ui.cls(HTML.LASER + "bid", CSSClass.INVISIBLE, true);
        ui.cls(HTML.LASER + "green", CSSClass.INVISIBLE, true);
        ui.cls(HTML.LASER + "ask", CSSClass.INVISIBLE, true);

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

        final boolean isTrader = isTrader();
        if (isTrader) {

            for (int i = 0; i < levels; i++) {
                ui.clickable('#' + HTML.BID + i);
                ui.clickable('#' + HTML.OFFER + i);
                ui.clickable('#' + HTML.ORDER + i);
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

    public void timedRefresh() {

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

    public void fastInputFlush() {

        activeView.refresh(getSymbol());
        ui.flush();
    }

    private void drawClock() {
        ui.txt(HTML.CLOCK, SIMPLE_DATE_FORMAT.format(new Date()));
        ui.cls(HTML.CLOCK, CSSClass.SLOW, clientSpeedState == ClientSpeedState.SLOW);
        ui.cls(HTML.CLOCK, CSSClass.VERY_SLOW, clientSpeedState == ClientSpeedState.TOO_SLOW);
    }

    public void clickTradingIssue(final LadderClickTradingIssue issue) {
        clickTradingIssue(ui, issue);
    }

    public static void clickTradingIssue(final UiPipeImpl ui, final LadderClickTradingIssue issue) {
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

        if (null != metaData) {
            if (metaData.spreadContractSet != null) {
                final SpreadContractSet contracts = metaData.spreadContractSet;
                ui.cls(HTML.SYMBOL, CSSClass.SPREAD, symbol.equals(contracts.spread));
                ui.cls(HTML.SYMBOL, CSSClass.BACK, symbol.equals(contracts.back));
            }
            // Desk position
            if (null != metaData.deskPosition && null != metaData.deskPosition.getPosition() &&
                    !metaData.deskPosition.getPosition().isEmpty()) {
                try {
                    final BigDecimal decimal = new BigDecimal(metaData.deskPosition.getPosition());
                    ui.txt(HTML.DESK_POSITION, formatPosition(decimal.doubleValue()));
                    decorateUpDown(ui, HTML.DESK_POSITION, decimal.longValue());
                } catch (final NumberFormatException ignored) {
                    /* exception.printStackTrace();*/
                }
            }
            // Day position
            if (null != metaData.dayPosition) {
                ui.txt(HTML.POSITION, formatPosition(metaData.dayPosition.getNet()));
                decorateUpDown(ui, HTML.POSITION, metaData.dayPosition.getNet());
                ui.txt(HTML.TOTAL_TRADED, formatPosition(metaData.dayPosition.getVolume()));

                ui.cls(HTML.POSITION, CSSClass.INVISIBLE, showTotalTraded);
                ui.cls(HTML.TOTAL_TRADED, CSSClass.INVISIBLE, !showTotalTraded);
            }

            if (null != metaData.pksExposure) {
                final String pksExposure = formatPosition(metaData.pksExposure.exposure);
                final String pksPosition = formatPosition(metaData.pksExposure.position);
                ui.txt(HTML.PKS_EXPOSURE, pksExposure);
                ui.txt(HTML.PKS_POSITION, pksPosition);
            }
            // Ladder info
            if (metaData.infoOnLadder != null) {
                ui.txt(HTML.AFTER_HOURS_WEIGHT, metaData.infoOnLadder.getValue());
            }
            // Ladder text
            for (final LadderText ladderText : metaData.ladderTextByPosition.values()) {
                ui.txt(HTML.TEXT_PREFIX + ladderText.getCell(), ladderText.getText());
            }

            // Going ex
            checkGoingEx();
            ui.cls(HTML.TEXT, CSSClass.GOING_EX, exState == GoingExState.YES);
        }
    }

    private static String formatPosition(final double qty) {
        final String display;
        final double absQty = Math.abs(qty);
        if (absQty < 10000) {
            display = Integer.toString((int) qty);
        } else if (absQty < 1000000) {
            display = POSITION_FMT.format(qty / 1000.0) + 'K';
        } else {
            display = POSITION_FMT.format(qty / 1000000.0) + 'M';
        }
        return display;
    }

    private void recenterIfTimeoutElapsed() {

        final long milliSinceLastCentered = System.currentTimeMillis() - lastCenteredTime;

        if (!activeView.canMoveTowardsCenter() || (RECENTER_TIME_MS < milliSinceLastCentered && activeView.moveTowardsCenter())) {
            resetLastCenteredTime();
        }
        ui.cls(HTML.LADDER, CSSClass.RECENTERING, 0 < lastCenteredTime && RECENTER_WARN_TIME_MS <= milliSinceLastCentered);
    }

    public void recenterLadderForUser(final RecenterLaddersForUser recenterLaddersForUser) {
        if (client.getUserName().equals(recenterLaddersForUser.user)) {
            activeView.center();
            resetLastCenteredTime();
        }
    }

    public void recenterLadderForUser(final CenterToPrice centerToPrice) {
        if (client.getUserName().equals(centerToPrice.getUsername()) && symbol.equals(centerToPrice.getSymbol())) {
            activeView.setCenteredPrice(centerToPrice.getPrice());
            resetLastCenteredTime();
        }
    }

    public void setCenterPrice(final long price) {
        activeView.setCenteredPrice(price);
        resetLastCenteredTime();
    }

    private void resetLastCenteredTime() {
        lastCenteredTime = System.currentTimeMillis();
    }

    // Inbound

    public void onRawInboundData(final String data) {
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
            } else if (HTML.STACK_GROUP_TICK_MULTIPLIER.equals(label)) {
                activeView.setStackGroupTickMultiplier(Integer.valueOf(value));
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
            setBookView();
        } else if (label.equals(HTML.STACK_VIEW_BUTTON)) {
            setStackView();
        } else if (label.equals(HTML.AFTER_HOURS_WEIGHT)) {
            if ("left".equals(button)) {
                openEwokView();
            }
        } else if (label.startsWith(HTML.PRICE) && "middle".equals(button)) {
            recenterLaddersForUser.publish(new RecenterLaddersForUser(client.getUserName()));
        } else if (label.equals(HTML.POSITION) || label.equals(HTML.TOTAL_TRADED)) {
            showTotalTraded = !showTotalTraded;
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

    boolean nextContract() {
        if (null != metaData && null != metaData.spreadContractSet) {
            final SpreadContractSet contracts = metaData.spreadContractSet;
            String nextContract = contracts.next(symbol);
            int count = 3;
            while (!symbolExists.test(nextContract) && 0 < count--) {
                nextContract = contracts.next(nextContract);
            }
            if (null != nextContract && !symbol.equals(nextContract) && symbolExists.test(nextContract)) {
                view.goToSymbol(nextContract);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    boolean switchChixSymbol() {
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

    public void onSingleOrderCommand(final OrdersPresenter.SingleOrderCommand singleOrderCommand) {

        bookView.onSingleOrderCommand(clientSpeedState, singleOrderCommand);
    }

    // Heartbeats

    public void sendHeartbeat() {
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

    public void setIsinsGoingEx(OpxlExDateSubscriber.IsinsGoingEx isinsGoingEx) {
        this.isinsGoingEx = isinsGoingEx;
        this.exState = GoingExState.Unknown;
        checkGoingEx();
    }

    private void checkGoingEx() {
        if (this.exState != GoingExState.Unknown) {
            return;
        }
        GoingExState exState = GoingExState.Unknown;
        if (isinsGoingEx != null && marketData != null && marketData.getBook() != null) {
            if (isinsGoingEx.isins.contains(marketData.getBook().getISIN())) {
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
