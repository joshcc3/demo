package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.icepie.transport.io.LadderTextNumberUnits;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.UserCycleRequest;
import com.drwtrading.london.reddal.data.InstrumentMetaData;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.NibblerLastTradeDataForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiEventHandler;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import com.drwtrading.london.reddal.ladders.model.HeaderPanel;
import com.drwtrading.london.reddal.ladders.model.LadderViewModel;
import com.drwtrading.london.reddal.ladders.model.LeftHandPanel;
import com.drwtrading.london.reddal.ladders.model.QtyButton;
import com.drwtrading.london.reddal.opxl.LadderNumberUpdate;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntrySymbolChannel;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByPrice;
import com.drwtrading.london.reddal.workspace.HostWorkspaceRequest;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.websockets.WebSocketClient;
import drw.eeif.fees.FeesCalc;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class LadderView implements UiEventHandler {

    private static final AtomicLong heartbeatSeqNo = new AtomicLong(0L);

    private static final int RECENTER_TIME_MS = 11000;
    private static final int RECENTER_WARN_TIME_MS = 9000;

    private static final int TAB = 9;
    private static final int PG_UP = 33;
    private static final int PG_DOWN = 34;
    private static final int END_KEY = 35;
    private static final int HOME_KEY = 36;
    private static final int INSERT_KEY = 45;
    private static final int NUM_PLUS = 107;
    private static final int NUM_MINUS = 109;
    private static final int PLUS = 187;
    private static final int MINUS = 189;

    static final Map<InstType, Map<QtyButton, Integer>> INST_TYPE_BUTTON_QTIES;

    static {
        INST_TYPE_BUTTON_QTIES = new EnumMap<>(InstType.class);

        final Map<QtyButton, Integer> defaultQties = new EnumMap<>(QtyButton.class);
        defaultQties.put(QtyButton.ONE, 1);
        defaultQties.put(QtyButton.TWO, 5);
        defaultQties.put(QtyButton.THREE, 10);
        defaultQties.put(QtyButton.FOUR, 50);
        defaultQties.put(QtyButton.FIVE, 100);
        defaultQties.put(QtyButton.SIX, 500);
        for (final InstType instType : InstType.values()) {
            INST_TYPE_BUTTON_QTIES.put(instType, defaultQties);
        }

        final Map<QtyButton, Integer> futuresQties = new EnumMap<>(QtyButton.class);
        futuresQties.put(QtyButton.ONE, 1);
        futuresQties.put(QtyButton.TWO, 5);
        futuresQties.put(QtyButton.THREE, 10);
        futuresQties.put(QtyButton.FOUR, 25);
        futuresQties.put(QtyButton.FIVE, 50);
        futuresQties.put(QtyButton.SIX, 100);
        INST_TYPE_BUTTON_QTIES.put(InstType.FUTURE, futuresQties);
        INST_TYPE_BUTTON_QTIES.put(InstType.FUTURE_SPREAD, futuresQties);

        final Map<QtyButton, Integer> fxQties = new EnumMap<>(QtyButton.class);
        fxQties.put(QtyButton.ONE, 100000);
        fxQties.put(QtyButton.TWO, 200000);
        fxQties.put(QtyButton.THREE, 500000);
        fxQties.put(QtyButton.FOUR, 1000000);
        fxQties.put(QtyButton.FIVE, 2500000);
        fxQties.put(QtyButton.SIX, 5000000);
        INST_TYPE_BUTTON_QTIES.put(InstType.FX, fxQties);

        final Map<QtyButton, Integer> equityQties = new EnumMap<>(QtyButton.class);
        equityQties.put(QtyButton.ONE, 1);
        equityQties.put(QtyButton.TWO, 10);
        equityQties.put(QtyButton.THREE, 100);
        equityQties.put(QtyButton.FOUR, 1000);
        equityQties.put(QtyButton.FIVE, 5000);
        equityQties.put(QtyButton.SIX, 10000);
        INST_TYPE_BUTTON_QTIES.put(InstType.DR, equityQties);
        INST_TYPE_BUTTON_QTIES.put(InstType.EQUITY, equityQties);
        INST_TYPE_BUTTON_QTIES.put(InstType.ETF, equityQties);
    }

    private final IFuseBox<ReddalComponents> monitor;

    private final WebSocketClient client;
    private final ILadderUI view;
    private final String ewokBaseURL;
    private final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher;
    private final LadderOptions ladderOptions;
    private final FXCalc<?> fxCalc;
    private final FeesCalc feesCalc;
    private final DecimalFormat feeDF;
    private final DecimalFormat ladderTextDF;
    private final Publisher<RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Map<String, OrderEntrySymbolChannel> orderEntryMap;
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer;
    private final TradingStatusForAll tradingStatusForAll;
    private final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final Publisher<UserPriceModeRequest> userPriceModeRequestPublisher;
    private final Publisher<HostWorkspaceRequest> userWorkspaceRequests;
    private final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher;
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher;
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher;
    private final Map<String, SearchResult> refData;

    private final LadderViewModel ladderModel;

    public String symbol;
    private int levels;
    private MDForSymbol marketData;
    private long lastCenteredTime = 0;
    private LadderMetaData metaData;
    private InstrumentMetaData instMetaData;
    private NibblerLastTradeDataForSymbol extraDataForSymbolForNibbler;
    private JasperLastTradeDataForSymbol extraDataForSymbolForJasper;

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

    LadderView(final IFuseBox<ReddalComponents> monitor, final WebSocketClient client, final UiPipeImpl ui, final ILadderUI view,
            final String ewokBaseURL, final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher, final LadderOptions ladderOptions,
            final FXCalc<?> fxCalc, final FeesCalc feesCalc, final DecimalFormat feeDF, final TradingStatusForAll tradingStatusForAll,
            final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher, final Publisher<RecenterLaddersForUser> recenterLaddersForUser,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher,
            final Publisher<UserCycleRequest> userCycleContractPublisher,
            final Publisher<UserPriceModeRequest> userPriceModeRequestPublisher,
            final Publisher<HostWorkspaceRequest> userWorkspaceRequests, final Map<String, OrderEntrySymbolChannel> orderEntryMap,
            final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher,
            final Publisher<StackIncreaseParentOffsetCmd> increaseParentOffsetPublisher,
            final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher,
            final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher, final Map<String, SearchResult> refData) {

        this.monitor = monitor;

        this.client = client;
        this.view = view;
        this.ewokBaseURL = ewokBaseURL;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.ladderOptions = ladderOptions;
        this.fxCalc = fxCalc;
        this.feesCalc = feesCalc;
        this.feeDF = feeDF;
        this.ladderTextDF = NumberFormatUtil.getDF("0.#");

        this.recenterLaddersForUser = recenterLaddersForUser;
        this.ladderClickTradingIssuePublisher = ladderClickTradingIssuePublisher;
        this.orderEntryMap = orderEntryMap;
        this.eeifCommandToServer = orderEntryCommandToServerPublisher;
        this.increaseParentOffsetPublisher = increaseParentOffsetPublisher;
        this.increaseChildOffsetCmdPublisher = increaseChildOffsetCmdPublisher;
        this.disableSiblingsCmdPublisher = disableSiblingsCmdPublisher;
        this.tradingStatusForAll = tradingStatusForAll;
        this.heartbeatRoundTripPublisher = heartbeatRoundTripPublisher;
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.userPriceModeRequestPublisher = userPriceModeRequestPublisher;
        this.userWorkspaceRequests = userWorkspaceRequests;
        this.refData = refData;

        this.ladderModel = new LadderViewModel(ui);
        ui.setHandler(this);

        this.activeView = LadderNoView.SINGLETON;
    }

    void replaceSymbol(final ReplaceCommand replaceCommand) {
        if (null != view) {
            view.replace(replaceCommand.from, replaceCommand.to);
        }
    }

    void subscribeToSymbol(final String symbol, final int levels, final Set<OrderType> supportedOrderTypes, final MDForSymbol marketData,
            final WorkingOrdersByPrice workingOrders, final LadderMetaData metaData, final InstrumentMetaData instMetaData,
            final NibblerLastTradeDataForSymbol extraNibblerDataForSymbol, final JasperLastTradeDataForSymbol extraDataForSymbolForJasper,
            final SymbolStackData stackData, final LadderPrefsForSymbolUser ladderPrefsForSymbolUser,
            final OrderUpdatesForSymbol orderUpdatesForSymbol) {

        this.symbol = symbol;
        this.levels = levels;
        this.ladderModel.extendToLevels(levels);
        this.marketData = marketData;
        this.metaData = metaData;
        this.instMetaData = instMetaData;
        this.extraDataForSymbolForNibbler = extraNibblerDataForSymbol;
        this.extraDataForSymbolForJasper = extraDataForSymbolForJasper;
        this.stackData = stackData;

        final boolean wasBookView = null == bookView || activeView == bookView;

        final User user = User.get(client.getUserName());
        final long bookCenteredPrice = null == bookView ? 0 : bookView.getCenteredPrice();
        this.bookView =
                new LadderBookView(monitor, user, isTrader(), symbol, ladderModel, view, fxCalc, feesCalc, feeDF, ladderPrefsForSymbolUser,
                        ladderClickTradingIssuePublisher, remoteOrderCommandToServerPublisher, eeifCommandToServer, tradingStatusForAll,
                        supportedOrderTypes, marketData, workingOrders, extraDataForSymbolForNibbler, this.extraDataForSymbolForJasper,
                        orderUpdatesForSymbol, levels, stackData, metaData, instMetaData, increaseParentOffsetPublisher,
                        increaseChildOffsetCmdPublisher, disableSiblingsCmdPublisher, orderEntryMap, bookCenteredPrice);

        final IBook<?> book = marketData.getBook();
        final Map<QtyButton, Integer> buttonQties;
        if (null == book) {
            buttonQties = INST_TYPE_BUTTON_QTIES.get(InstType.UNKNOWN);
        } else {
            buttonQties = INST_TYPE_BUTTON_QTIES.get(book.getInstType());
        }

        this.stackView = new LadderStackView(user, isTrader(), symbol, buttonQties, levels, ladderModel, stackData, metaData,
                increaseParentOffsetPublisher, increaseChildOffsetCmdPublisher, disableSiblingsCmdPublisher, view, ladderPrefsForSymbolUser,
                marketData);

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
    }

    private void initialSetup() {

        ladderModel.clear();
        view.draw(levels);
        ladderModel.getBookPanel().sendLevelData(levels);

        for (final CSSClass type : CSSClass.values()) {
            ladderModel.setClass(HTML.ORDER_TYPE_LEFT, type, false);
            ladderModel.setClass(HTML.ORDER_TYPE_RIGHT, type, false);
            ladderModel.setClass(HTML.WORKING_ORDER_TAG, type, false);
        }

        final String symbolDescription = getSymbolDescription();

        final HeaderPanel headerPanel = ladderModel.getHeaderPanel();
        headerPanel.setSymbol(symbol);
        headerPanel.setDescription(symbolDescription);

        ladderModel.setClass(HTML.LASER + "BID", CSSClass.INVISIBLE, true);
        ladderModel.setClass(HTML.LASER + "GREEN", CSSClass.INVISIBLE, true);
        ladderModel.setClass(HTML.LASER + "WHITE", CSSClass.INVISIBLE, true);
        ladderModel.setClass(HTML.LASER + "ASK", CSSClass.INVISIBLE, true);

        activeView.switchedTo();

        ladderModel.setClickable('#' + HTML.SYMBOL);
        ladderModel.setClickable('#' + HTML.BOOK_VIEW_BUTTON);
        ladderModel.setClickable('#' + HTML.STACK_VIEW_BUTTON);
        ladderModel.setClickable('#' + HTML.CLOCK);
        ladderModel.setClickable('#' + HTML.POSITION);
        ladderModel.setClickable('#' + HTML.TOTAL_TRADED);
        ladderModel.setClickable('#' + HTML.AFTER_HOURS_WEIGHT);

        for (int i = 0; i < levels; i++) {
            ladderModel.setClickable('#' + HTML.PRICE + i);
        }
        ladderModel.setClickable(HTML.BUTTONS);
        ladderModel.setScrollable('#' + HTML.LADDER);

        ladderModel.setClickable('#' + HTML.TEXT_PREFIX + HTML.R1C2);
        ladderModel.setClickable('#' + HTML.TEXT_PREFIX + HTML.R1C3);
        ladderModel.setClickable('#' + HTML.TEXT_PREFIX + HTML.R1C4);

        final boolean isTrader = isTrader();
        if (isTrader) {

            for (int i = 0; i < levels; i++) {
                ladderModel.setClickable('#' + HTML.BID + i);
                ladderModel.setClickable('#' + HTML.OFFER + i);
                ladderModel.setClickable('#' + HTML.ORDER + i);

                ladderModel.setClickable('#' + HTML.STACK_BID_PICARD + i);
                ladderModel.setClickable('#' + HTML.STACK_BID_QUOTE + i);
                ladderModel.setClickable('#' + HTML.STACK_BID_OFFSET + i);

                ladderModel.setClickable('#' + HTML.STACK_ASK_PICARD + i);
                ladderModel.setClickable('#' + HTML.STACK_ASK_QUOTE + i);
                ladderModel.setClickable('#' + HTML.STACK_ASK_OFFSET + i);
            }

            ladderModel.setClickable('#' + HTML.BUY_QTY);
            ladderModel.setClickable('#' + HTML.SELL_QTY);
            ladderModel.setClickable('#' + HTML.BUY_OFFSET_UP);
            ladderModel.setClickable('#' + HTML.BUY_OFFSET_DOWN);
            ladderModel.setClickable('#' + HTML.SELL_OFFSET_UP);
            ladderModel.setClickable('#' + HTML.SELL_OFFSET_DOWN);
            ladderModel.setClickable('#' + HTML.START_BUY);
            ladderModel.setClickable('#' + HTML.STOP_BUY);
            ladderModel.setClickable('#' + HTML.START_SELL);
            ladderModel.setClickable('#' + HTML.STOP_SELL);

            ladderModel.setClass(HTML.OFFSET_CONTROL, CSSClass.INVISIBLE, false);
        }
    }

    private String getSymbolDescription() {

        final FutureConstant futureFromSymbol = FutureConstant.getFutureFromSymbol(symbol);

        if (null != futureFromSymbol) {

            return futureFromSymbol.contractDesc + " [" + futureFromSymbol.wholePointValue + "x " + futureFromSymbol.index + ']';
        } else {

            final String symbolDescription;
            if (null != instMetaData && instMetaData.getDescription() != null) {
                symbolDescription = instMetaData.getDescription();
            } else {
                symbolDescription = symbol;
            }

            return symbolDescription;
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
        ladderModel.flush();
    }

    void fastInputFlush() {

        activeView.refresh(getSymbol());
        ladderModel.flush();
    }

    private void drawClock() {

        final LeftHandPanel panel = ladderModel.getLeftHandPanel();
        panel.setTime(System.currentTimeMillis());
        ladderModel.setClass(HTML.CLOCK, CSSClass.SLOW, clientSpeedState == ClientSpeedState.SLOW);
        ladderModel.setClass(HTML.CLOCK, CSSClass.VERY_SLOW, clientSpeedState == ClientSpeedState.TOO_SLOW);
    }

    void clickTradingIssue(final String issue) {
        ladderModel.setErrorText(issue);
    }

    private String getSymbol() {
        if (null != metaData && null != metaData.displaySymbol) {
            return metaData.displaySymbol;
        } else {
            return symbol;
        }
    }

    private void drawMetaData() {

        final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();

        ladderModel.setClass(HTML.BOOK_VIEW_BUTTON, CSSClass.ACTIVE_MODE, activeView == bookView);
        ladderModel.setClass(HTML.STACK_VIEW_BUTTON, CSSClass.ACTIVE_MODE, activeView == stackView);

        ladderModel.setClass(HTML.SYMBOL, CSSClass.REVERSE_SPREAD, null != marketData && marketData.isReverseSpread());

        if (null != metaData) {

            final HeaderPanel headerPanel = ladderModel.getHeaderPanel();

            if (metaData.spreadContractSet != null) {
                ladderModel.setClass(HTML.SYMBOL, CSSClass.SPREAD, symbol.equals(metaData.spreadContractSet.contractAfterNext));
                ladderModel.setClass(HTML.SYMBOL, CSSClass.BACK, symbol.equals(metaData.spreadContractSet.nextContract));
            }
            // Desk position

            if (metaData.deskPosition.isInitialised()) {
                final String formattedDeskPosition = metaData.deskPosition.getFormattedValue();
                final long deskPosition = metaData.deskPosition.getValue();
                leftHandPanel.setDeskPosition(deskPosition, formattedDeskPosition);
            }
            // Day position
            if (null != instMetaData.getFormattedMrChillNetPosition()) {
                leftHandPanel.setDayPosition(instMetaData.getMrChillNetPosition(), instMetaData.getFormattedMrChillNetPosition());
                leftHandPanel.setOurTotalTradedQty(instMetaData.getMrChillVolume(), instMetaData.getFormattedMrChillVolume());

                ladderModel.setClass(HTML.POSITION, CSSClass.INVISIBLE, showTotalTraded);
                ladderModel.setClass(HTML.TOTAL_TRADED, CSSClass.INVISIBLE, !showTotalTraded);
            }

            final PKSExposure pksExposure = metaData.getPKSData();
            if (null != pksExposure) {
                final double combinedPosition = pksExposure.getCombinedPosition();

                headerPanel.setPksExposure(pksExposure.dryExposure, metaData.pksExposure.getFormattedValue());
                headerPanel.setPksPosition(combinedPosition, metaData.pksPosition.getFormattedValue());
            }

            // Ladder text
            for (final Map.Entry<ReddalFreeTextCell, String> ladderText : metaData.freeTextCells.entrySet()) {

                final ReddalFreeTextCell cell = ladderText.getKey();
                final String text = ladderText.getValue();
                headerPanel.setLadderText(cell, text);

                final String cellDescription = metaData.ladderTextDescription.get(cell);
                if (cellDescription != null) {
                    headerPanel.setCellDescription(cell, cellDescription);
                }
            }

            // Ladder Numbers
            for (final Map.Entry<ReddalFreeTextCell, LadderNumberUpdate> ladderNumber : metaData.ladderTextNumberCells.entrySet()) {

                final ReddalFreeTextCell cell = ladderNumber.getKey();
                final LadderNumberUpdate update = ladderNumber.getValue();
                final double value = (double) update.value / Constants.NORMALISING_FACTOR;
                final Double refPrice = getRefPrice();
                final double convertedValue;
                if (null == refPrice) {
                    convertedValue = value;
                } else {
                    convertedValue = convertFrom(bookView.getActivePricingMode(), update.units, value, refPrice);
                }
                final String formattedValue = formatNumber(convertedValue, bookView.getActivePricingMode(), update.units);
                headerPanel.setLadderText(cell, formattedValue);

                final String cellDescription = metaData.ladderTextDescription.get(cell);
                if (cellDescription != null) {
                    headerPanel.setCellDescription(cell, cellDescription);
                }
            }

            headerPanel.setBestBidOffsetBPS(stackData.getBidTopOrderOffsetBPS());
            headerPanel.setBestAskOffsetBPS(stackData.getAskTopOrderOffsetBPS());

            final TheoValue theoValue = stackData.getTheoValue();
            headerPanel.setTheoValue(theoValue);

            // Going ex
            checkGoingEx();
            ladderModel.setClass(HTML.TEXT, CSSClass.GOING_EX, exState == GoingExState.YES);
        }

        if (null != stackData) {
            final double uppyDownyValue = stackData.getPriceOffsetTickSize();
            leftHandPanel.setBPSTooltip(uppyDownyValue);
        }
    }

    private String formatNumber(final double convertedValue, final PricingMode activePricingMode, final LadderTextNumberUnits units) {
        final double absVal = Math.abs(convertedValue);

        if (PricingMode.BPS == activePricingMode && LadderTextNumberUnits.RAW != units) {
            if (convertedValue >= 100) {
                return ladderTextDF.format(convertedValue / 100) + '%';
            } else {
                return ladderTextDF.format(convertedValue);
            }
        } else {
            if (absVal > 1_000_000_000) {
                return (int) (convertedValue / 1_000_000_000) + "B";
            } else if (absVal > 1_000_000) {
                return (int) (convertedValue / 1_000_000) + "M";
            } else if (absVal > 1_000) {
                return (int) (convertedValue / 1_000) + "K";
            } else {
                return ladderTextDF.format(convertedValue);
            }
        }
    }

    private static double convertFrom(final PricingMode activePricingMode, final LadderTextNumberUnits units, final double value,
            final double refPrice) {
        if (PricingMode.BPS == activePricingMode && LadderTextNumberUnits.EFP == units) {
            return value / refPrice * 1_00_00;
        } else if (PricingMode.EFP == activePricingMode && LadderTextNumberUnits.BPS == units) {
            return value / 1_00_00 * refPrice;
        } else {
            return value;
        }
    }

    private void recenterIfTimeoutElapsed() {

        final long milliSinceLastCentered = System.currentTimeMillis() - lastCenteredTime;

        if (!activeView.canMoveTowardsCenter() || (RECENTER_TIME_MS < milliSinceLastCentered && activeView.moveTowardsCenter())) {
            resetLastCenteredTime();
        }
        ladderModel.setClass(HTML.LADDER, CSSClass.RECENTERING, 0 < lastCenteredTime && RECENTER_WARN_TIME_MS <= milliSinceLastCentered);
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
        ladderModel.inboundFromUI(data);
    }

    @Override
    public void onUpdate(final String label, final Map<String, String> dataArg) {

        String value = dataArg.get("value");
        if (value != null) {
            value = value.trim();
            if (HTML.INP_QTY.equals(label)) {
                activeView.setTradingBoxQty(Integer.parseInt(value));
            } else if (HTML.STACK_TICK_SIZE.equals(label)) {
                activeView.setStackTickSize(Double.parseDouble(value));
            } else if (HTML.STACK_ALIGNMENT_TICK_TO_BPS.equals(label)) {
                activeView.setStackAlignmentTickToBPS(Double.parseDouble(value));
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
            case INSERT_KEY: {
                activeView.setLaserLineCenter();
                break;
            }
            case PLUS:
            case NUM_PLUS: {
                activeView.zoomIn();
                break;
            }
            case MINUS:
            case NUM_MINUS: {
                activeView.zoomOut();
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
            } else if ("right".equals(button)) {
                openIndyScreen();
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
        } else if (label.startsWith(HTML.PRICING) && "middle".equals(button)) {
            final PricingMode mode = PricingMode.getFromHtml(label);
            if (mode != null) {
                final UserPriceModeRequest priceModeRequest = new UserPriceModeRequest(client.getUserName(), mode);
                userPriceModeRequestPublisher.publish(priceModeRequest);
            }
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

    void switchContract(final UserCycleRequest cycleRequest) {
        if (null != metaData && null != metaData.spreadContractSet) {

            if (cycleRequest.contract != null) {
                final SpreadContractSet contracts = metaData.spreadContractSet;
                switch (cycleRequest.contract) {
                    case FRONT:
                        goToContract(contracts.frontMonth);
                        return;
                    case BACK:
                        goToContract(contracts.backMonth);
                        return;
                    case SPREAD:
                        goToContract(contracts.spread);
                        return;
                    default:
                }
            } else {
                nextContract();
            }
        }
    }

    void setPricingMode(final PricingMode mode) {
        if (bookView != null) {
            bookView.setPricingMode(mode);
        }
    }

    private void nextContract() {
        final SpreadContractSet contracts = metaData.spreadContractSet;
        if (contracts != null) {
            String nextContract = contracts.next(symbol);
            int count = 3;
            while (!refData.containsKey(nextContract) && 0 < count--) {
                nextContract = contracts.next(nextContract);
            }
            goToContract(nextContract);
        }
    }

    private void goToContract(final String targetSymbol) {
        if (targetSymbol != null && !symbol.equals(targetSymbol) && refData.containsKey(targetSymbol)) {
            view.goToSymbol(targetSymbol);
        }
    }

    private void switchChixSymbol() {
        if (null != metaData && null != metaData.chixSwitchSymbol && refData.containsKey(metaData.chixSwitchSymbol)) {
            view.goToSymbol(metaData.chixSwitchSymbol);
        }
    }

    private void openEwokView() {

        if (null != marketData && null != marketData.getBook()) {
            switch (marketData.getBook().getInstType()) {
                case FUTURE: {
                    popupFuture(marketData.getBook().getSymbol());
                    return;
                }
                case FUTURE_SPREAD: {
                    popupFuture(marketData.getBook().getSymbol().split("-")[0]);
                    return;
                }
                case EQUITY:
                case ETF:
                case DR:
                case INDEX: {
                    popupEwok();
                    return;
                }
                default: {
                }
            }
        }
    }

    private void popupEwok() {
        final String symbol = marketData.getBook().getSymbol();

        final String url;

        if (symbol.endsWith(" RFQ")) {
            if (symbol.length() > 6) { //suffix +" RFQ" e.g. "LN RFQ"
                final int length = symbol.length() - 4;
                final String originalSymbol = symbol.substring(0, length - 2) + ' ' + symbol.substring(length - 2, length);

                url = ewokBaseURL + "/smart#" + originalSymbol;
            } else {
                url = ewokBaseURL + "/smart#" + symbol;
            }
        } else {
            url = ewokBaseURL + "/smart#" + symbol;
        }

        view.popUp(url, "Ewok " + symbol, 1200, 800);
    }

    private void popupFuture(final String expiry) {
        final FutureConstant future = FutureConstant.getFutureFromSymbol(expiry);
        if (null != future) {
            final String url = ewokBaseURL + "/smart#" + future.index.name();
            view.popUp(url, "Ewok " + future.index.name(), 1200, 800);
        }
    }

    private void openIndyScreen() {

        if (null != instMetaData && null != instMetaData.getIndyDefName() && null != marketData && null != marketData.getBook()) {

            final boolean isETF = InstType.ETF == marketData.getBook().getInstType();
            final String etfSwitch = isETF ? "etf." : "";

            final String url = "http://prod-indy.eeif.drw:11100/composition#" + etfSwitch + instMetaData.getIndyDefName();
            view.popUp(url, "Indy " + symbol, 1200, 800);
        }
    }

    private boolean isTrader() {
        final User user = User.getUser(client.getUserName());
        return ladderOptions.traders.contains(user);
    }

    void onSingleOrderCommand(final ISingleOrderCommand singleOrderCommand) {

        bookView.onSingleOrderCommand(clientSpeedState, singleOrderCommand);
    }

    // Heartbeats

    void sendHeartbeat() {

        if (lastHeartbeatSentMillis == null) {
            lastHeartbeatSentMillis = System.currentTimeMillis();
            ladderModel.sendHeartbeat(lastHeartbeatSentMillis, heartbeatSeqNo.getAndIncrement());
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

    private Double getRefPrice() {
        if (null == marketData || null == marketData.getBook()) {
            return null;
        } else {
            final IBook<?> book = marketData.getBook();
            final IBookLevel bestBid = book.getBestBid();
            final IBookLevel bestAsk = book.getBestAsk();
            final IBookReferencePrice yestClose = book.getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
            if (book.isValid() && null != bestBid && null != bestAsk) {
                return ((bestBid.getPrice() >> 1) + (bestAsk.getPrice() >> 1)) / (double) Constants.NORMALISING_FACTOR;
            } else if (book.isValid() && yestClose.isValid()) {
                return yestClose.getPrice() / (double) Constants.NORMALISING_FACTOR;
            } else {
                return null;
            }
        }
    }
}
