package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.LastTradeDataForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.model.BookHTMLRow;
import com.drwtrading.london.reddal.ladders.model.BookPanel;
import com.drwtrading.london.reddal.ladders.model.BookPanelRow;
import com.drwtrading.london.reddal.ladders.model.HeaderPanel;
import com.drwtrading.london.reddal.ladders.model.LadderViewModel;
import com.drwtrading.london.reddal.ladders.model.LeftHandPanel;
import com.drwtrading.london.reddal.ladders.model.QtyButton;
import com.drwtrading.london.reddal.orderManagement.oe.ManagedOrderType;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.util.EnumSwitcher;
import com.drwtrading.london.reddal.util.Mathematics;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByPrice;
import com.google.common.collect.ImmutableSet;
import drw.eeif.eeifoe.Cancel;
import drw.eeif.eeifoe.Metadata;
import drw.eeif.eeifoe.OrderSide;
import drw.eeif.eeifoe.Submit;
import drw.eeif.fees.FeesCalc;
import drw.london.json.Jsonable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LadderBookView implements ILadderBoard {

    public static final int REALLY_BIG_NUMBER_THRESHOLD = 100000;
    private static final String LADDER_SOURCE = "LadderView";
    private static final int MODIFY_TIMEOUT_MILLI = 5000;
    private static final double DEFAULT_EQUITY_NOTIONAL_EUR = 100000.0;

    private static final Metadata LADDER_SOURCE_METADATA = new Metadata("SOURCE", "LADDER");

    private static final int AUTO_RECENTER_TICKS = 3;

    private static final Set<String> TAGS = ImmutableSet.of("CHAD", "DIV", "STRING", "CLICKNOUGHT", "GLABN");

    private static final EnumSet<CSSClass> WORKING_ORDER_CSS;
    private static final Set<String> PERSISTENT_PREFS = new HashSet<>();

    static {

        WORKING_ORDER_CSS = EnumSet.noneOf(CSSClass.class);

        for (final CSSClass cssClass : CSSClass.values()) {

            if (cssClass.name().startsWith("WORKING_ORDER_TYPE_")) {
                WORKING_ORDER_CSS.add(cssClass);
            }
        }
    }

    static {
        PERSISTENT_PREFS.add(HTML.WORKING_ORDER_TAG);
        PERSISTENT_PREFS.add(HTML.INP_RELOAD);
        PERSISTENT_PREFS.add(HTML.ORDER_TYPE_LEFT);
        PERSISTENT_PREFS.add(HTML.ORDER_TYPE_RIGHT);
        PERSISTENT_PREFS.add(HTML.RANDOM_RELOAD_CHECK);
    }

    private final IResourceMonitor<ReddalComponents> monitor;

    private final String username;
    private final boolean isTrader;
    private final String symbol;

    private final LadderViewModel ladderModel;
    private final ILadderUI view;

    private final LadderOptions ladderOptions;
    private final FXCalc<?> fxCalc;
    private final FeesCalc feesCalc;
    private final DecimalFormat feeDF;

    private final LadderPrefsForSymbolUser ladderPrefsForSymbolUser;
    private final Map<String, String> defaultPrefs;

    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuesPublisher;
    private final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher;
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer;
    private final TradingStatusForAll tradingStatusForAll;

    private final Set<OrderType> supportedOrderTypes;
    private final Set<AlgoType> supportedAlgoTypes;

    private final MDForSymbol marketData;
    private final WorkingOrdersByPrice workingOrders;
    private final LastTradeDataForSymbol dataForSymbol;
    private final OrderUpdatesForSymbol orderUpdatesForSymbol;

    private final EnumSwitcher<PricingMode> pricingModes;
    private final Map<QtyButton, Integer> buttonQty;

    private final int levels;
    private final SymbolStackData stackData;
    private final LadderMetaData metaData;
    private final Publisher<StackIncreaseParentOffsetCmd> stackParentCmdPublisher;
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher;
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher;

    private final Publisher<Jsonable> trace;
    private final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap;
    private final Set<String> managedOrderTypes;
    private final Set<String> oldOrderTypes;

    private boolean isCashEquityOrFX;
    private boolean showYesterdaySettleInsteadOfCOD;

    private boolean pendingRefDataAndSettle;

    private long centeredPrice;
    private long topPrice;
    private long bottomPrice;

    private int clickTradingBoxQty;
    private String feeString;
    private int orderSeqNo = 0;

    private Long modifyFromPrice;
    private long modifyFromPriceSelectedTime;

    LadderBookView(final IResourceMonitor<ReddalComponents> monitor, final String username, final boolean isTrader, final String symbol,
            final LadderViewModel ladderModel, final ILadderUI view, final LadderOptions ladderOptions, final FXCalc<?> fxCalc,
            final FeesCalc feesCalc, final DecimalFormat feeDF, final LadderPrefsForSymbolUser ladderPrefsForSymbolUser,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssuesPublisher,
            final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher, final Publisher<OrderEntryCommandToServer> eeifCommandToServer,
            final TradingStatusForAll tradingStatusForAll, final Set<OrderType> supportedOrderTypes, final Set<AlgoType> supportedAlgoTypes,
            final MDForSymbol marketData, final WorkingOrdersByPrice workingOrders, final LastTradeDataForSymbol extraDataForSymbol,
            final OrderUpdatesForSymbol orderUpdatesForSymbol, final int levels, final SymbolStackData stackData,
            final LadderMetaData metaData, final Publisher<StackIncreaseParentOffsetCmd> stackParentCmdPublisher,
            final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher,
            final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher, final Publisher<Jsonable> trace,
            final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap, final long centeredPrice) {

        this.monitor = monitor;

        this.username = username;
        this.isTrader = isTrader;
        this.symbol = symbol;

        this.ladderModel = ladderModel;
        this.view = view;

        this.ladderOptions = ladderOptions;
        this.fxCalc = fxCalc;
        this.feesCalc = feesCalc;
        this.feeDF = feeDF;

        this.clickTradingBoxQty = 0;
        recalcFee();

        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;

        this.defaultPrefs = new HashMap<>();
        this.defaultPrefs.put(HTML.WORKING_ORDER_TAG, "CHAD");
        if (username.startsWith("dcook")) {
            this.defaultPrefs.put(HTML.INP_RELOAD, "0");
        } else if (symbol.startsWith("FDAX")) {
            this.defaultPrefs.put(HTML.INP_RELOAD, "5");
        } else {
            this.defaultPrefs.put(HTML.INP_RELOAD, "50");
        }
        this.defaultPrefs.put(HTML.ORDER_TYPE_LEFT, "HAWK");
        this.defaultPrefs.put(HTML.ORDER_TYPE_RIGHT, "MANUAL");
        this.defaultPrefs.put(HTML.RANDOM_RELOAD_CHECK, "true");

        this.ladderClickTradingIssuesPublisher = ladderClickTradingIssuesPublisher;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.eeifCommandToServer = eeifCommandToServer;
        this.tradingStatusForAll = tradingStatusForAll;

        this.supportedOrderTypes = supportedOrderTypes;
        this.supportedAlgoTypes = supportedAlgoTypes;
        this.marketData = marketData;
        this.workingOrders = workingOrders;
        this.dataForSymbol = extraDataForSymbol;
        this.orderUpdatesForSymbol = orderUpdatesForSymbol;

        this.levels = levels;
        this.stackData = stackData;
        this.metaData = metaData;
        this.stackParentCmdPublisher = stackParentCmdPublisher;
        this.increaseChildOffsetCmdPublisher = increaseChildOffsetCmdPublisher;
        this.disableSiblingsCmdPublisher = disableSiblingsCmdPublisher;

        this.pricingModes = new EnumSwitcher<>(PricingMode.class, PricingMode.values());
        this.buttonQty = new EnumMap<>(QtyButton.class);

        this.trace = trace;
        this.orderEntryMap = orderEntryMap;

        this.managedOrderTypes = new HashSet<>();
        for (final ManagedOrderType orderType : ManagedOrderType.values()) {
            managedOrderTypes.add(orderType.toString());
        }

        this.oldOrderTypes = new HashSet<>();
        for (final RemoteOrderType orderType : RemoteOrderType.values()) {
            oldOrderTypes.add(orderType.toString());
        }

        this.isCashEquityOrFX = false;
        this.pendingRefDataAndSettle = true;

        this.centeredPrice = centeredPrice;

        this.bottomPrice = Long.MAX_VALUE;
        this.topPrice = Long.MIN_VALUE;

    }

    @Override
    public long getCenteredPrice() {
        return centeredPrice;
    }

    @Override
    public void setCenteredPrice(final long newCenterPrice) {

        if (null != marketData.getBook()) {
            final BookPanel bookPanel = ladderModel.getBookPanel();
            final int zoomLevel = bookPanel.getZoomLevel();
            this.centeredPrice = this.marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, newCenterPrice, zoomLevel);

            final int centerLevel = levels / 2;
            final ITickTable tickTable = marketData.getBook().getTickTable();
            topPrice = tickTable.addTicks(this.centeredPrice, centerLevel * zoomLevel);

            bookPanel.clearPriceMapping();

            long price = topPrice;
            for (int i = 0; i < levels; ++i) {

                final String formattedPrice = marketData.formatPrice(price);
                bookPanel.setRowPrice(i, price, formattedPrice);

                bottomPrice = price;
                price = tickTable.addTicks(price, -1 * zoomLevel);
            }

            bookPanel.sendLevelData(levels);
        }
    }

    @Override
    public boolean setPersistencePreference(final String label, final String value) {

        final boolean result = PERSISTENT_PREFS.contains(label);
        if (result) {
            ladderPrefsForSymbolUser.set(label, value);
        }
        return result;
    }

    @Override
    public void setTradingBoxQty(final int qty) {
        clickTradingBoxQty = qty;
        recalcFee();
    }

    private void recalcFee() {

        if (0 < clickTradingBoxQty) {
            final IBook<?> inst = marketData.getBook();
            if (null != inst && null != inst.getBestBid() && null != inst.getBestAsk()) {

                final double toEurFX = fxCalc.getLastValidMid(inst.getCCY(), CCY.EUR);
                final double mid = toEurFX * (inst.getBestBid().getPrice() + inst.getBestAsk().getPrice()) / 2d;
                final double notionalEUR = clickTradingBoxQty * mid / Constants.NORMALISING_FACTOR;

                final double indicativeFeeWithoutFirstTrade =
                        feesCalc.getFeeEur(symbol, marketData.getTradeMIC(), inst.getMIC(), inst.getInstType(), BookSide.BID,
                                clickTradingBoxQty, notionalEUR, false);
                final double indicativeFeeWithFirstTrade =
                        feesCalc.getFeeEur(symbol, marketData.getTradeMIC(), inst.getMIC(), inst.getInstType(), BookSide.BID,
                                clickTradingBoxQty, notionalEUR, true);
                final double firstTradeFee = indicativeFeeWithFirstTrade - indicativeFeeWithoutFirstTrade;
                feeString = feeDF.format(indicativeFeeWithoutFirstTrade) + " (" + feeDF.format(firstTradeFee) + ')';
            }
        } else {
            feeString = "---";
        }
    }

    @Override
    public void setStackTickSize(final double tickSize) {
        // ignored
    }

    @Override
    public void setStackAlignmentTickToBPS(final double stackAlignmentTickToBPS) {
        // ignored
    }

    @Override
    public void setStackTickSizeToMatchQuote() {
        // ignored
    }

    @Override
    public void switchedTo() {

        ladderModel.setClass(HTML.LADDER_DIV, CSSClass.STACK_VIEW, false);

        view.trading(isTrader, TAGS, filterUsableOrderTypes(ladderOptions.orderTypesLeft),
                filterUsableOrderTypes(ladderOptions.orderTypesRight));

        ladderModel.setClickable('#' + HTML.YESTERDAY_SETTLE);
        ladderModel.setClickable('#' + HTML.LAST_TRADE_COD);

        ladderModel.setClickable('#' + HTML.PKS_EXPOSURE);
        ladderModel.setClickable('#' + HTML.PKS_POSITION);

        ladderModel.setClass(HTML.RANDOM_RELOAD, CSSClass.INVISIBLE, false);

        ladderModel.setClass(HTML.ORDER_TYPE_LEFT, CSSClass.FULL_WIDTH, false);
        ladderModel.setClass(HTML.ORDER_TYPE_RIGHT, CSSClass.FULL_WIDTH, false);

        ladderModel.setClass(HTML.STACK_CONFIG_BUTTON, CSSClass.INVISIBLE, true);
        ladderModel.setClass(HTML.STACKS_CONTROL, CSSClass.INVISIBLE, true);

        for (final Map.Entry<QtyButton, Integer> entry : buttonQty.entrySet()) {
            ladderModel.getLeftHandPanel().setQtyButton(entry.getKey(), entry.getValue());
        }

        for (int i = 0; i < levels; i++) {
            ladderModel.setClickable('#' + HTML.VOLUME + i);
        }
    }

    @Override
    public void timedRefresh() {

        clearModifyPriceIfTimedOut();

        if (pendingRefDataAndSettle && null != marketData.getBook() && marketData.getBook().isValid()) {

            final InstType instType = marketData.getBook().getInstType();
            buttonQty.putAll(LadderView.INST_TYPE_BUTTON_QTIES.get(instType));

            switch (instType) {
                case EQUITY:
                case ETF:
                case DR: {
                    isCashEquityOrFX = true;
                    pricingModes.setValidChoices(PricingMode.BPS, PricingMode.RAW);
                    pricingModes.set(PricingMode.BPS);
                    final IBookReferencePrice closePrice = marketData.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
                    if (closePrice.isValid()) {
                        int qty =
                                (int) Mathematics.toQuantityFromNotionalInSafetyCurrency(DEFAULT_EQUITY_NOTIONAL_EUR, closePrice.getPrice(),
                                        marketData.getBook(), marketData.getBook().getWPV());
                        qty = Math.max(50, qty);
                        defaultPrefs.put(HTML.INP_RELOAD, Integer.toString(qty));
                    }

                    break;
                }
                case FX: {
                    isCashEquityOrFX = true;
                    pricingModes.setValidChoices(PricingMode.BPS, PricingMode.RAW);
                    pricingModes.set(PricingMode.BPS);
                    defaultPrefs.put(HTML.INP_RELOAD, "1000000");

                    break;
                }
                default: {
                    pricingModes.setValidChoices(PricingMode.EFP, PricingMode.RAW);
                    pricingModes.set(PricingMode.EFP);
                    break;
                }
            }

            final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();
            for (final Map.Entry<QtyButton, Integer> entry : buttonQty.entrySet()) {
                leftHandPanel.setQtyButton(entry.getKey(), entry.getValue());
            }

            pendingRefDataAndSettle = false;
            moveTowardsCenter();
        }
    }

    @Override
    public void refresh(final String symbol) {

        drawBook(symbol);
        drawTradedVolumes();
        drawWorkingOrders();
        drawPricingButtons();
        drawPriceLevels();
        drawMetaData();
        drawClickTrading();
    }

    private void drawPricingButtons() {

        for (final PricingMode mode : pricingModes.getUniverse()) {
            ladderModel.setClass(HTML.PRICING + mode, CSSClass.INVISIBLE, !pricingModes.isValidChoice(mode));
            ladderModel.setClass(HTML.PRICING + mode, CSSClass.ACTIVE_MODE, pricingModes.get() == mode);
        }
    }

    @Override
    public boolean moveTowardsCenter() {

        if (null != marketData.getBook() && marketData.getBook().isValid()) {
            final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();

            final long bookCenter = getCenterPrice();
            if (bookCenter < bottomPrice || topPrice < bookCenter) {
                final long newCentrePrice;
                if (0 == centeredPrice) {
                    newCentrePrice = bookCenter;
                } else {
                    final long direction = (long) Math.signum(bookCenter - centeredPrice);
                    newCentrePrice =
                            marketData.getBook().getTickTable().addTicks(centeredPrice, AUTO_RECENTER_TICKS * direction * zoomLevel);
                }
                setCenteredPrice(newCentrePrice);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canMoveTowardsCenter() {
        if (null != marketData.getBook() && marketData.getBook().isValid()) {
            final long centerPrice = getCenterPrice();
            return centerPrice < bottomPrice || topPrice < centerPrice;
        } else {
            return false;
        }
    }

    @Override
    public void center() {

        if (null != marketData.getBook() && marketData.getBook().isValid()) {

            final long bookCenter = getCenterPrice();
            setCenteredPrice(bookCenter);
        }
    }

    private long getCenterPrice() {

        final IBook<?> book = marketData.getBook();
        if (!pendingRefDataAndSettle && null != book && book.isValid()) {

            final Long specialCasePrice = specialCaseCenterPrice(book);

            final IBookLevel bestBid = book.getBestBid();
            final IBookLevel bestAsk = book.getBestAsk();
            final boolean isBookNotAuction = book.getStatus() != BookMarketState.AUCTION;
            final IBookReferencePrice auctionIndicativePrice = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
            final IBookReferencePrice auctionSummaryPrice = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_SUMMARY);
            final IBookReferencePrice yestClose = marketData.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);

            final long center;
            if (isBookNotAuction && null != bestBid && null != bestAsk) {
                center = (bestBid.getPrice() + bestAsk.getPrice()) / 2;
            } else if (isBookNotAuction && null != bestBid) {
                center = bestBid.getPrice();
            } else if (isBookNotAuction && null != bestAsk) {
                center = bestAsk.getPrice();
            } else if (auctionIndicativePrice.isValid()) {
                center = auctionIndicativePrice.getPrice();
            } else if (workingOrders.hasAnyWorkingOrder()) {

                final Collection<Long> prices = workingOrders.getWorkingOrderPrices();
                final long n = prices.size();
                long avgPrice = 0L;
                for (final long price : prices) {
                    avgPrice += price / n;
                }
                center = avgPrice;
            } else if (auctionSummaryPrice.isValid()) {
                center = auctionSummaryPrice.getPrice();
            } else if (marketData.getTradeTracker().hasTrade()) {
                center = marketData.getTradeTracker().getLastPrice();
            } else if (stackData.getTheoLaserLine().isValid()) {
                center = stackData.getTheoLaserLine().getValue();
            } else if (null != specialCasePrice) {
                center = specialCasePrice;
            } else if (yestClose.isValid()) {
                center = yestClose.getPrice();
            } else {
                center = 0;
            }
            final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();
            return marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, center, zoomLevel);
        } else {
            return 0;
        }
    }

    private void drawPriceLevels() {

        if (!pendingRefDataAndSettle) {

            final LaserLineValue navLaserLine = stackData.getNavLaserLine();
            final BookPanel bookPanel = ladderModel.getBookPanel();

            switch (pricingModes.get()) {
                case RAW: {
                    bookPanel.setRawPrices(levels);
                    break;
                }
                case BPS: {
                    if (navLaserLine.isValid()) {
                        final long basePrice = navLaserLine.getValue();
                        drawBPSBook(bookPanel, basePrice);
                    } else if (isCashEquityOrFX && hasBestBid()) {
                        final long basePrice = marketData.getBook().getBestBid().getPrice();
                        drawBPSBook(bookPanel, basePrice);
                    } else {
                        bookPanel.setRawPrices(levels);
                    }
                    break;
                }
                case EFP: {
                    if (navLaserLine.isValid()) {
                        drawEFPBook(bookPanel, navLaserLine.getValue());
                    } else if (marketData.isPriceInverted()) {
                        drawInvertedFXFutureBook(bookPanel);
                    } else {
                        bookPanel.setRawPrices(levels);
                    }
                    break;
                }
            }
        }
    }

    private void drawBPSBook(final BookPanel bookPanel, final long basePrice) {

        for (int i = 0; i < levels; ++i) {
            final BookPanelRow row = bookPanel.getRow(i);
            final double points = (10000d * (row.getPrice() - basePrice)) / basePrice;
            bookPanel.setBPS(row, points);
        }
    }

    private void drawEFPBook(final BookPanel bookPanel, final double navPrice) {

        for (int i = 0; i < levels; ++i) {
            final BookPanelRow row = bookPanel.getRow(i);
            final double efp = (row.getPrice() - navPrice) / Constants.NORMALISING_FACTOR;
            bookPanel.setEFP(row, efp);
        }
    }

    private void drawInvertedFXFutureBook(final BookPanel bookPanel) {

        for (int i = 0; i < levels; ++i) {
            final BookPanelRow row = bookPanel.getRow(i);
            final double invertedPrice = Constants.NORMALISING_FACTOR / (double) row.getPrice();
            bookPanel.setEFP(row, invertedPrice);
        }
    }

    private boolean hasBestBid() {
        return null != marketData.getBook() && null != marketData.getBook().getBestBid();
    }

    private void drawMetaData() {

        if (!pendingRefDataAndSettle && null != dataForSymbol && null != marketData) {

            drawLaserLines();

            final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();
            final BookPanel bookPanel = ladderModel.getBookPanel();

            final Long lastTradeChangeOnDay = getLastTradeChangeOnDay(marketData);
            if (lastTradeChangeOnDay != null) {
                leftHandPanel.setLastTradeCOD(lastTradeChangeOnDay, marketData);
            }

            final Long yesterdaySettle = getYesterdaySettle(marketData);
            if (null != yesterdaySettle) {
                leftHandPanel.setYesterdaySettlePrice(yesterdaySettle, marketData);
            }

            ladderModel.setClass(HTML.YESTERDAY_SETTLE, CSSClass.INVISIBLE, !showYesterdaySettleInsteadOfCOD);
            ladderModel.setClass(HTML.LAST_TRADE_COD, CSSClass.INVISIBLE, showYesterdaySettleInsteadOfCOD);

            final ITickTable tickTable = marketData.getBook().getTickTable();
            final int zoomLevel = bookPanel.getZoomLevel();

            final LastTrade lastBid = dataForSymbol.lastBid();
            final LastTrade lastAsk = dataForSymbol.lastAsk();
            final long lastBidPrice =
                    lastBid != null ? tickTable.roundAwayToTick(BookSide.BID, lastBid.getPrice(), zoomLevel) : Long.MIN_VALUE;
            final long lastAskPrice =
                    lastAsk != null ? tickTable.roundAwayToTick(BookSide.ASK, lastAsk.getPrice(), zoomLevel) : Long.MAX_VALUE;

            for (int i = 0; i < levels; ++i) {
                final BookPanelRow priceRow = bookPanel.getRow(i);
                final String priceKey = priceRow.htmlData.bookPriceKey;
                final long rowPrice = priceRow.getPrice();

                ladderModel.setClass(priceKey, CSSClass.LAST_BID, lastBidPrice == rowPrice);
                ladderModel.setClass(priceKey, CSSClass.LAST_ASK, lastAskPrice == rowPrice);
            }
        }
    }

    private Collection<String> filterUsableOrderTypes(final Collection<CSSClass> types) {
        if (null != marketData.getBook()) {
            final String mic = marketData.getBook().getMIC().name();
            return types.stream().filter(orderType -> isOrderTypeSupported(orderType, mic)).map(Enum::name).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isOrderTypeSupported(final CSSClass orderTypeCSS, final String mic) {

        final RemoteOrderType orderType = getRemoteOrderType(orderTypeCSS.name());

        return null != orderType && TAGS.stream().anyMatch(tag -> {
            final boolean manualOrderType = supportedOrderTypes.contains(orderType.orderType);
            final boolean newOrderType = orderEntryMap.containsKey(symbol) && managedOrderTypes.contains(orderType.name()) &&
                    orderEntryMap.get(symbol).supportedTypes.contains(ManagedOrderType.valueOf(orderType.name()));
            return manualOrderType || newOrderType;
        });
    }

    private void drawClickTrading() {

        if (null != ladderPrefsForSymbolUser) {

            final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();
            leftHandPanel.setClickTradingQty(clickTradingBoxQty, feeString);

            for (final String pref : PERSISTENT_PREFS) {
                leftHandPanel.setClickTradingPreference(pref, getPref(pref));
            }

            final String leftOrderPricePref = getPref(HTML.ORDER_TYPE_LEFT);
            for (final CSSClass type : ladderOptions.orderTypesLeft) {
                ladderModel.setClass(HTML.ORDER_TYPE_LEFT, type, type.name().equals(leftOrderPricePref));
            }

            final String rightOrderPricePref = getPref(HTML.ORDER_TYPE_RIGHT);
            for (final CSSClass type : ladderOptions.orderTypesRight) {
                ladderModel.setClass(HTML.ORDER_TYPE_RIGHT, type, type.name().equals(rightOrderPricePref));
            }
        }

        if (!pendingRefDataAndSettle) {
            final BookPanel bookPanel = ladderModel.getBookPanel();
            for (int i = 0; i < levels; ++i) {
                final BookPanelRow priceRow = bookPanel.getRow(i);
                final long price = priceRow.getPrice();
                ladderModel.setClass(priceRow.htmlData.bookOrderKey, CSSClass.MODIFY_PRICE_SELECTED,
                        null != modifyFromPrice && price == modifyFromPrice);
            }
        }

        final boolean isInvisible = null == metaData.spreadContractSet || null == metaData.spreadContractSet.stackSymbol;
        ladderModel.setClass(HTML.OFFSET_CONTROL, CSSClass.INVISIBLE, isInvisible);
    }

    private String getPref(final String id) {
        return ladderPrefsForSymbolUser.get(id, defaultPrefs.get(id));
    }

    private void drawLaserLines() {

        if (null != marketData.getBook() && marketData.getBook().isValid()) {

            for (final LaserLineValue laserLine : stackData.getLaserLines()) {
                setLaserLine(laserLine);
            }
        }
    }

    private void setLaserLine(final LaserLineValue laserLine) {

        final String laserKey = laserLine.getType().htmlKey;
        final BookPanel bookPanel = ladderModel.getBookPanel();

        if (0 < levels && laserLine.isValid() && !bookPanel.isEmpty()) {

            final long laserLinePrice = laserLine.getValue();

            if (topPrice <= laserLinePrice) {
                final BookPanelRow priceRow = bookPanel.getRow(0);
                ladderModel.setHeight(laserKey, priceRow.htmlData.bookPriceKey, 0.5);
            } else if (laserLinePrice < bottomPrice) {
                final BookPanelRow priceRow = bookPanel.getRow(levels - 1);
                ladderModel.setHeight(laserKey, priceRow.htmlData.bookPriceKey, -0.5);
            } else {
                long price = bottomPrice;
                while (price <= topPrice) {
                    final long priceAbove = marketData.getBook().getTickTable().addTicks(price, bookPanel.getZoomLevel());
                    final BookPanelRow row = bookPanel.getRowByPrice(price);
                    if (price <= laserLinePrice && laserLinePrice <= priceAbove && null != row) {
                        final long fractionalPrice = laserLinePrice - price;
                        final double tickFraction = 1.0 * fractionalPrice / (priceAbove - price);
                        ladderModel.setHeight(laserKey, row.htmlData.bookPriceKey, tickFraction);
                        break;
                    }
                    price = priceAbove;
                }
            }

            ladderModel.setClass(laserKey, CSSClass.INVISIBLE, false);
        } else {
            ladderModel.setClass(laserKey, CSSClass.INVISIBLE, true);
        }
    }

    private void drawBook(final String symbol) {

        if (!pendingRefDataAndSettle && null != marketData.getBook()) {

            final BookPanel bookPanel = ladderModel.getBookPanel();
            final HeaderPanel headerPanel = ladderModel.getHeaderPanel();
            final int zoomLevel = bookPanel.getZoomLevel();
            final boolean isZoomed = zoomLevel != 1;
            final String zoomString = isZoomed ? " x" + zoomLevel : "";

            switch (marketData.getBook().getStatus()) {
                case CONTINUOUS: {
                    headerPanel.setSymbol(symbol + zoomString);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.AUCTION, false);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case AUCTION: {
                    headerPanel.setSymbol(symbol + " - AUC" + zoomString);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.AUCTION, true);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case CLOSED: {
                    headerPanel.setSymbol(symbol + " - CLSD" + zoomString);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.AUCTION, true);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                default: {
                    headerPanel.setSymbol(symbol + " - ?" + zoomString);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.AUCTION, false);
                    ladderModel.setClass(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, true);
                    break;
                }
            }
            headerPanel.setTitle(symbol);

            final boolean isAuctionValid;
            final long auctionPrice;
            final long auctionQty;

            if (BookMarketState.AUCTION == marketData.getBook().getStatus()) {
                final IBookReferencePrice auctionIndicative = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
                isAuctionValid = auctionIndicative.isValid();
                auctionPrice = auctionIndicative.getPrice();
                auctionQty = auctionIndicative.getQty();
            } else {
                isAuctionValid = false;
                auctionPrice = 0;
                auctionQty = 0;
            }

            final ITickTable tickTable = marketData.getBook().getTickTable();
            final IBookLevel bestBid = marketData.getBook().getBestBid();
            final IBookLevel bestAsk = marketData.getBook().getBestAsk();

            IBookLevel bid = bestBid;
            while (bid != null && bid.getPrice() > topPrice) {
                bid = bid.next();
            }

            long rowPrice = topPrice;
            while (rowPrice >= bottomPrice) {
                long bidQty = 0;
                long bidImpliedQty = 0;

                while (bid != null && bid.getPrice() >= rowPrice) {
                    bidQty += bid.getQty();
                    bidImpliedQty += bid.getImpliedQty();
                    bid = bid.next();
                }

                final BookPanelRow row = bookPanel.getRowByPrice(rowPrice);
                bidQty(row, bidQty, false);
                ladderModel.setClass(row.htmlData.bookBidKey, CSSClass.IMPLIED_BID, 0 < bidImpliedQty);
                rowPrice = tickTable.subtractTicks(rowPrice, zoomLevel);
            }

            IBookLevel ask = bestAsk;
            while (ask != null && ask.getPrice() < bottomPrice) {
                ask = ask.next();
            }

            rowPrice = bottomPrice;
            while (rowPrice <= topPrice) {
                long askQty = 0;
                long askImpliedQty = 0;

                while (ask != null && ask.getPrice() <= rowPrice) {
                    askQty += ask.getQty();
                    askImpliedQty += ask.getImpliedQty();
                    ask = ask.next();
                }

                final BookPanelRow row = bookPanel.getRowByPrice(rowPrice);
                askQty(row, askQty, false);
                ladderModel.setClass(row.htmlData.bookAskKey, CSSClass.IMPLIED_ASK, 0 < askImpliedQty);
                rowPrice = tickTable.addTicks(rowPrice, zoomLevel);
            }

            if (isAuctionValid && auctionPrice >= bottomPrice && auctionPrice <= topPrice) {
                final BookPanelRow auctionRow = bookPanel.getRowByPrice(tickTable.roundAwayToTick(BookSide.BID, auctionPrice, zoomLevel));
                bidQty(auctionRow, auctionQty, true);
                askQty(auctionRow, auctionQty, true);
            }

            ladderModel.setClass(HTML.BOOK_TABLE, CSSClass.AUCTION, BookMarketState.AUCTION == marketData.getBook().getStatus());
        }
    }

    private void drawWorkingOrders() {

        if (!pendingRefDataAndSettle && null != workingOrders && null != orderUpdatesForSymbol) {
            final BookPanel bookPanel = ladderModel.getBookPanel();
            final int zoomLevel = bookPanel.getZoomLevel();
            final ITickTable tickTable = marketData.getBook().getTickTable();

            final long bottomOfferPrice = tickTable.subtractTicks(bottomPrice, zoomLevel - 1);
            final long topBidPrice = tickTable.addTicks(topPrice, zoomLevel - 1);

            final Map<BookSide, Iterator<Map.Entry<Long, LinkedHashSet<SourcedWorkingOrder>>>> woItMap = new EnumMap<>(BookSide.class);
            final Map<BookSide, Iterator<Map.Entry<Long, HashMap<String, UpdateFromServer>>>> oeItMap = new EnumMap<>(BookSide.class);

            for (final BookSide side : BookSide.values()) {
                woItMap.put(side, workingOrders.getOrdersInRange(side, bottomOfferPrice, topBidPrice).entrySet().iterator());
                oeItMap.put(side, orderUpdatesForSymbol.getOrdersInRange(side, bottomOfferPrice, topBidPrice).entrySet().iterator());
            }

            final RowAccumulator rA = new RowAccumulator();
            Map.Entry<Long, LinkedHashSet<SourcedWorkingOrder>> bidWoEntry = null;
            Map.Entry<Long, LinkedHashSet<SourcedWorkingOrder>> askWoEntry = null;

            Map.Entry<Long, HashMap<String, UpdateFromServer>> bidOeEntry = null;
            Map.Entry<Long, HashMap<String, UpdateFromServer>> askOeEntry = null;
            for (int i = 0; i < levels; i++) {
                final BookPanelRow row = bookPanel.getRow(i);
                final long rowPrice = row.getPrice();
                rA.clear();

                bidWoEntry = iterate(tickTable, rowPrice, zoomLevel, BookSide.BID, bidWoEntry, woItMap.get(BookSide.BID), rA::addWOs);
                askWoEntry = iterate(tickTable, rowPrice, zoomLevel, BookSide.ASK, askWoEntry, woItMap.get(BookSide.ASK), rA::addWOs);

                bidOeEntry = iterate(tickTable, rowPrice, zoomLevel, BookSide.BID, bidOeEntry, oeItMap.get(BookSide.BID), rA::addOEUpdates);
                askOeEntry = iterate(tickTable, rowPrice, zoomLevel, BookSide.ASK, askOeEntry, oeItMap.get(BookSide.ASK), rA::addOEUpdates);

                rA.totalQty += Math.max(rA.managedOrderQty, rA.hiddenTickTakerQty);
                workingQty(bookPanel, row, rA.totalQty, rA.side, rA.orderTypes, 0 < rA.managedOrderQty);
                if (0 < rA.keys.length()) {
                    rA.keys.setLength(rA.keys.length() - 1);
                }
                if (0 < rA.eeifKeys.length()) {
                    rA.eeifKeys.setLength(rA.eeifKeys.length() - 1);
                }

                ladderModel.setData(row.htmlData.bookOrderKey, DataKey.ORDER, rA.keys);
                ladderModel.setData(row.htmlData.bookOrderKey, DataKey.EEIF, rA.eeifKeys);
            }
        }

        long buyQty = 0;
        long sellQty = 0;
        long buyHiddenTTQty = 0;
        long sellHiddenTTQty = 0;
        long buyManagedQty = 0;
        long sellManagedQty = 0;

        for (final long activePrice : workingOrders.getWorkingOrderPrices()) {

            for (final SourcedWorkingOrder workingOrderNode : workingOrders.getWorkingOrdersAtPrice(activePrice)) {

                final WorkingOrder workingOrder = workingOrderNode.order;
                final long remainingQty = workingOrder.getOrderQty() - workingOrder.getFilledQty();
                if (BookSide.BID == workingOrder.getSide()) {
                    if (AlgoType.HIDDEN_TICK_TAKER == workingOrder.getAlgoType()) {
                        buyHiddenTTQty += remainingQty;
                    } else {
                        buyQty += remainingQty;
                    }
                } else {
                    if (AlgoType.HIDDEN_TICK_TAKER == workingOrder.getAlgoType()) {
                        sellHiddenTTQty += remainingQty;
                    } else {
                        sellQty += remainingQty;
                    }
                }
            }
        }
        for (final UpdateFromServer updateFromServer : orderUpdatesForSymbol.updatesByKey.values()) {
            if (updateFromServer.update.getOrder().getSide() == OrderSide.BUY) {
                buyManagedQty += updateFromServer.update.getRemainingQty();
            } else if (updateFromServer.update.getOrder().getSide() == OrderSide.SELL) {
                sellManagedQty += updateFromServer.update.getRemainingQty();
            }
        }
        buyQty += Math.max(buyHiddenTTQty, buyManagedQty);
        sellQty += Math.max(sellHiddenTTQty, sellManagedQty);

        final HeaderPanel headerPanel = ladderModel.getHeaderPanel();
        headerPanel.setBidQty(buyQty);
        headerPanel.setAskQty(sellQty);
    }

    private void drawTradedVolumes() {

        final MDForSymbol m = this.marketData;
        if (!pendingRefDataAndSettle && null != m) {

            final TradeTracker tradeTracker = m.getTradeTracker();
            final long lastTradePrice = tradeTracker.getQtyRunAtLastPrice();
            final BookPanel bookPanel = ladderModel.getBookPanel();
            final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();
            final int zoomLevel = bookPanel.getZoomLevel();
            final long minTradedPrice = tradeTracker.getMinTradedPrice();
            final long maxTradedPrice = tradeTracker.getMaxTradedPrice();

            for (int i = 0; i < levels; ++i) {

                final BookPanelRow bookPanelRow = bookPanel.getRow(i);
                final long rowPrice = bookPanelRow.getPrice();
                final boolean withinTradedRange = minTradedPrice <= rowPrice && rowPrice <= maxTradedPrice;

                long qty = 0;
                long tickPrice = rowPrice;
                final ITickTable tickTable = m.getBook().getTickTable();

                if (withinTradedRange) {
                    for (int tick = 0; tick < zoomLevel; tick++) {
                        final Long qtyAtPrice = tradeTracker.getTotalQtyTradedAtPrice(tickPrice);
                        qty += qtyAtPrice != null ? qtyAtPrice : 0;
                        tickPrice = tickTable.addTicks(tickPrice, -1);
                    }
                }

                bookPanel.setVolume(bookPanelRow, qty);
                ladderModel.setClass(bookPanelRow.htmlData.bookPriceKey, CSSClass.PRICE_TRADED, withinTradedRange);

                final long nextRowPrice = tickTable.addTicks(rowPrice, -zoomLevel);
                if (1 < i && tradeTracker.hasTrade() && nextRowPrice < tradeTracker.getLastPrice() &&
                        rowPrice >= tradeTracker.getLastPrice()) {
                    bookPanel.setLastTradePriceVolume(bookPanelRow, lastTradePrice);
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.INVISIBLE, false);
                    ladderModel.setClass(bookPanelRow.htmlData.bookVolumeKey, CSSClass.INVISIBLE, true);
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.TRADED_UP, tradeTracker.isLastTickUp());
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.TRADED_DOWN, tradeTracker.isLastTickDown());
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.TRADED_AGAIN, tradeTracker.isLastTradeSameLevel());
                } else {
                    bookPanel.setLastTradePriceVolume(bookPanelRow, 0);
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.INVISIBLE, true);
                    ladderModel.setClass(bookPanelRow.htmlData.bookVolumeKey, CSSClass.INVISIBLE, false);
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.TRADED_UP, false);
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.TRADED_DOWN, false);
                    ladderModel.setClass(bookPanelRow.htmlData.bookTradeKey, CSSClass.TRADED_AGAIN, false);
                }
            }

            if (0 < levels) {
                bookPanel.setCCY(marketData.getBook().getCCY());
                ladderModel.setClass(HTML.VOLUME + '0', CSSClass.CCY, true);
            }

            if (1 < levels) {
                bookPanel.setMIC(marketData.getBook().getMIC());
                ladderModel.setClass(HTML.VOLUME + '1', CSSClass.MIC, true);
            }

            final long mktQty = tradeTracker.getTotalQtyTraded();
            leftHandPanel.setMktTotalTradedQty(mktQty);
        }
    }

    private void workingQty(final BookPanel bookPanel, final BookPanelRow bookPanelRow, final long qty, final BookSide side,
            final Set<CSSClass> orderTypes, final boolean hasEeifOEOrder) {

        bookPanel.setWorkingQty(bookPanelRow, qty);
        ladderModel.setClass(bookPanelRow.htmlData.bookOrderKey, CSSClass.WORKING_QTY, 0 < qty);
        ladderModel.setClass(bookPanelRow.htmlData.bookOrderKey, CSSClass.WORKING_BID, BookSide.BID == side);
        ladderModel.setClass(bookPanelRow.htmlData.bookOrderKey, CSSClass.WORKING_OFFER, BookSide.ASK == side);

        for (final CSSClass cssClass : WORKING_ORDER_CSS) {
            ladderModel.setClass(bookPanelRow.htmlData.bookOrderKey, cssClass, !hasEeifOEOrder && orderTypes.contains(cssClass));
        }
        ladderModel.setClass(bookPanelRow.htmlData.bookOrderKey, CSSClass.EEIF_ORDER_TYPE, hasEeifOEOrder);
    }

    @Override
    public void scrollUp() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, zoomLevel);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void scrollDown() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();
            final long newCenterPrice = book.getTickTable().subtractTicks(centeredPrice, zoomLevel);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void pageUp() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();
            final int n = levels - 1;
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, n * zoomLevel);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void pageDown() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();
            final int n = -1 * (levels - 1);
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, n * zoomLevel);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void onClick(final ClientSpeedState clientSpeedState, final String label, final String button, final Map<String, String> data) {

        if ("left".equals(button)) {
            final QtyButton qtyButton = QtyButton.getButtonFromHTML(label);
            if (buttonQty.containsKey(qtyButton)) {
                clickTradingBoxQty += buttonQty.get(qtyButton);
                recalcFee();
            } else if (HTML.BUTTON_CLR.equals(label)) {
                clickTradingBoxQty = 0;
                recalcFee();
            } else if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (null != ladderPrefsForSymbolUser) {
                    submitOrderLeftClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                final long price = Long.valueOf(data.get("price"));
                final BookHTMLRow htmlRowKeys = ladderModel.getBookPanel().getRowByPrice(price).htmlData;
                if (label.equals(htmlRowKeys.bookOrderKey)) {
                    cancelWorkingOrders(price);
                } else {
                    monitor.logError(ReddalComponents.LADDER_PRESENTER,
                            "Mismatched label: " + data.get("price") + ' ' + htmlRowKeys.bookOrderKey + ' ' + label);
                }
            } else if (label.equals(HTML.BUY_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, 1));
                } else {
                    stackData.improveBidStackPriceOffset(stackData.getPriceOffsetTickSize());
                }
            } else if (label.equals(HTML.BUY_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, -1));
                } else {
                    stackData.improveBidStackPriceOffset(-stackData.getPriceOffsetTickSize());
                }
            } else if (label.equals(HTML.SELL_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, 1));
                } else {
                    stackData.improveAskStackPriceOffset(stackData.getPriceOffsetTickSize());
                }
            } else if (label.equals(HTML.SELL_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, -1));
                } else {
                    stackData.improveAskStackPriceOffset(-stackData.getPriceOffsetTickSize());
                }
            } else if (label.equals(HTML.START_BUY)) {
                stackData.startBidStrategy();
            } else if (label.equals(HTML.START_SELL)) {
                stackData.startAskStrategy();
            } else if (label.equals(HTML.STOP_BUY)) {
                if (null != metaData.spreadContractSet.parentSymbol) {
                    final StacksSetSiblingsEnableCmd cmd =
                            new StacksSetSiblingsEnableCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, false);
                    disableSiblingsCmdPublisher.publish(cmd);
                }
                stackData.stopBidStrategy();
            } else if (label.equals(HTML.STOP_SELL)) {
                if (null != metaData.spreadContractSet.parentSymbol) {
                    final StacksSetSiblingsEnableCmd cmd =
                            new StacksSetSiblingsEnableCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, false);
                    disableSiblingsCmdPublisher.publish(cmd);
                }
                stackData.stopAskStrategy();
            } else if (label.equals(HTML.PRICING_BPS)) {
                pricingModes.set(PricingMode.BPS);
            } else if (label.equals(HTML.PRICING_RAW)) {
                pricingModes.set(PricingMode.RAW);
            } else if (label.equals(HTML.PRICING_EFP)) {
                pricingModes.set(PricingMode.EFP);
            } else if (label.startsWith(DataKey.PRICE.key)) {
                pricingModes.next();
            } else if (label.equals(HTML.VOLUME + '0')) {
                view.popUp(
                        "/fx#" + ((double) centeredPrice / Constants.NORMALISING_FACTOR) + ' ' + marketData.getBook().getCCY().major.name(),
                        null, 245, 332);
            } else if (label.startsWith(HTML.VOLUME)) {
                view.launchBasket(symbol);
            } else if (label.equals(HTML.YESTERDAY_SETTLE) || label.equals(HTML.LAST_TRADE_COD)) {
                showYesterdaySettleInsteadOfCOD = !showYesterdaySettleInsteadOfCOD;
            } else if (label.equals(HTML.PKS_EXPOSURE)) {

                final PKSExposure pksExposure = metaData.getPKSData();
                if (null != pksExposure) {
                    clickTradingBoxQty = Math.abs((int) pksExposure.exposure);
                }
            } else if (label.equals(HTML.PKS_POSITION)) {

                final PKSExposure pksExposure = metaData.getPKSData();
                if (null != pksExposure) {
                    clickTradingBoxQty = Math.abs((int) pksExposure.position);
                }
            }
        } else if ("right".equals(button)) {
            if (HTML.BUTTON_CLR.equals(label)) {
                ladderPrefsForSymbolUser.set(HTML.INP_RELOAD, Integer.toString(clickTradingBoxQty));
            } else if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (ladderPrefsForSymbolUser != null) {
                    submitOrderRightClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                rightClickModify(clientSpeedState, data);
            } else if (label.equals(HTML.PRICING_RAW)) {
                view.popUp("/shredder#" + symbol, "shredder " + symbol, 500, 500);
            } else if (label.equals(HTML.BUY_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(LADDER_SOURCE, symbol, BookSide.BID, stackData.getPriceOffsetTickSize()));
                } else {
                    stackData.adjustBidStackLevels(-1);
                }
            } else if (label.equals(HTML.BUY_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(LADDER_SOURCE, symbol, BookSide.BID, -stackData.getPriceOffsetTickSize()));
                } else {
                    stackData.adjustBidStackLevels(1);
                }
            } else if (label.equals(HTML.SELL_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(LADDER_SOURCE, symbol, BookSide.ASK, stackData.getPriceOffsetTickSize()));
                } else {
                    stackData.adjustAskStackLevels(1);
                }
            } else if (label.equals(HTML.SELL_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(LADDER_SOURCE, symbol, BookSide.ASK, -stackData.getPriceOffsetTickSize()));
                } else {
                    stackData.adjustAskStackLevels(-1);
                }
            } else if (label.equals(HTML.START_BUY)) {
                if (null != metaData.spreadContractSet.parentSymbol) {
                    final StacksSetSiblingsEnableCmd cmd =
                            new StacksSetSiblingsEnableCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, true);
                    disableSiblingsCmdPublisher.publish(cmd);
                }
                stackData.startBidStrategy();
            } else if (label.equals(HTML.START_SELL)) {
                stackData.startAskStrategy();
                if (null != metaData.spreadContractSet.parentSymbol) {
                    final StacksSetSiblingsEnableCmd cmd =
                            new StacksSetSiblingsEnableCmd(LADDER_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, true);
                    disableSiblingsCmdPublisher.publish(cmd);
                }
            } else if (label.equals(HTML.STOP_BUY)) {
                stackData.stopBidStrategy();
            } else if (label.equals(HTML.STOP_SELL)) {
                stackData.stopAskStrategy();
            }
        } else if ("middle".equals(button)) {
            if (label.startsWith(HTML.ORDER)) {
                final String priceLevel = data.get("price");
                final long price = Long.valueOf(priceLevel);
                final ITickTable tickTable = marketData.getBook().getTickTable();
                final int zoomLevel = ladderModel.getBookPanel().getZoomLevel();
                final long bidPrice = tickTable.addTicks(price, zoomLevel - 1);
                final long askPrice = tickTable.subtractTicks(price, zoomLevel - 1);
                if (workingOrders.hasOrderBetween(bidPrice, askPrice)) {
                    final String url = "/orders#" + symbol + ',' + priceLevel + ',' + bidPrice + ',' + askPrice;
                    view.popUp(url, "orders", 270, 120);
                }
            }
        }
    }

    @Override
    public void setBestAskCenter() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {

            final IBookLevel bestAsk = book.getBestAsk();
            if (null != bestAsk) {
                final long newCenterPrice = bestAsk.getPrice();
                setCenteredPrice(newCenterPrice);
            }
        }
    }

    @Override
    public void setBestBidCenter() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {

            final IBookLevel bestBid = book.getBestBid();
            if (null != bestBid) {
                final long newCenterPrice = bestBid.getPrice();
                setCenteredPrice(newCenterPrice);
            }
        }
    }

    private void bidQty(final BookPanelRow row, final long qty, final boolean isAuctionPrice) {
        final BookPanel bookPanel = ladderModel.getBookPanel();
        bookPanel.setBidQty(row, qty);
        ladderModel.setClass(row.htmlData.bookBidKey, CSSClass.BID_ACTIVE, 0 < qty);
        ladderModel.setClass(row.htmlData.bookBidKey, CSSClass.AUCTION, isAuctionPrice);
    }

    private void askQty(final BookPanelRow row, final long qty, final boolean isAuctionPrice) {
        final BookPanel bookPanel = ladderModel.getBookPanel();
        bookPanel.setAskQty(row, qty);
        ladderModel.setClass(row.htmlData.bookAskKey, CSSClass.ASK_ACTIVE, 0 < qty);
        ladderModel.setClass(row.htmlData.bookAskKey, CSSClass.AUCTION, isAuctionPrice);
    }

    private void submitOrderLeftClick(final ClientSpeedState clientSpeedState, final String label, final Map<String, String> data) {

        final String orderType = getPref(HTML.ORDER_TYPE_LEFT);
        submitOrderClick(clientSpeedState, label, data, orderType);
    }

    private void submitOrderRightClick(final ClientSpeedState clientSpeedState, final String label, final Map<String, String> data) {

        final String orderType = getPref(HTML.ORDER_TYPE_RIGHT);
        submitOrderClick(clientSpeedState, label, data, orderType);
    }

    private void submitOrderClick(final ClientSpeedState clientSpeedState, final String label, final Map<String, String> data,
            final String orderType) {

        final long price = Long.valueOf(data.get("price"));
        final BookPanelRow bookRow = ladderModel.getBookPanel().getRowByPrice(price);

        final BookSide side;
        if (label.equals(bookRow.htmlData.bookBidKey)) {
            side = BookSide.BID;
        } else if (label.equals(bookRow.htmlData.bookAskKey)) {
            side = BookSide.ASK;
        } else {
            throw new IllegalArgumentException("Price " + price + " did not match key " + label);
        }

        final String tag = ladderPrefsForSymbolUser.get(HTML.WORKING_ORDER_TAG);

        if (null == tag) {
            throw new IllegalArgumentException("No tag provided.");
        } else {

            if (orderType != null && 0 < clickTradingBoxQty) {
                if (managedOrderTypes.contains(orderType)) {
                    submitManagedOrder(orderType, price, side, tag);
                } else if (oldOrderTypes.contains(orderType)) {
                    submitOrder(clientSpeedState, orderType, price, side, tag);
                } else {
                    ladderModel.setErrorText("Unknown order type [" + orderType + "].");
                    return;
                }
            }

            final boolean randomReload = "true".equals(getPref(HTML.RANDOM_RELOAD_CHECK));
            final int reloadBoxQty = Integer.valueOf(getPref(HTML.INP_RELOAD));

            if (randomReload) {
                clickTradingBoxQty = Math.max(0, reloadBoxQty - (int) (Math.random() * ladderOptions.randomReloadFraction * reloadBoxQty));
                recalcFee();
            } else {
                clickTradingBoxQty = Math.max(0, reloadBoxQty);
                recalcFee();
            }
        }
    }

    private void submitManagedOrder(final String orderType, final long price, final BookSide side, final String tag) {

        int tradingBoxQty = this.clickTradingBoxQty;
        trace.publish(
                new CommandTrace("submitManaged", username, symbol, orderType, true, price, side.name(), tag, tradingBoxQty, orderSeqNo++));
        final OrderEntryClient.SymbolOrderChannel symbolOrderChannel = orderEntryMap.get(symbol);
        if (null != symbolOrderChannel) {
            final ManagedOrderType managedOrderType = ManagedOrderType.valueOf(orderType);
            if (!symbolOrderChannel.supportedTypes.contains(managedOrderType)) {
                ladderModel.setErrorText("Order type [" + orderType + "] not supported.");
                return;
            }
            tradingBoxQty = managedOrderType.getQty(tradingBoxQty);
            if (0 == tradingBoxQty) {
                tradingBoxQty = clickTradingBoxQty;
            }
            final OrderSide orderSide = BookSide.BID == side ? OrderSide.BUY : OrderSide.SELL;
            final drw.eeif.eeifoe.RemoteOrder remoteOrder =
                    new drw.eeif.eeifoe.RemoteOrder(symbol, orderSide, price, tradingBoxQty, username,
                            managedOrderType.getOrder(price, tradingBoxQty, orderSide),
                            new ObjectArrayList<>(Arrays.asList(LADDER_SOURCE_METADATA, new Metadata("TAG", tag))));
            final Submit submit = new Submit(remoteOrder);
            symbolOrderChannel.publisher.publish(submit);
        } else {
            ladderModel.setErrorText("Cannot find server to send order type [" + orderType + "].");
        }
    }

    private void submitOrder(final ClientSpeedState clientSpeedState, final String orderType, final long price, final BookSide side,
            final String tag) {

        final int sequenceNumber = orderSeqNo++;

        trace.publish(
                new CommandTrace("submit", username, symbol, orderType, true, price, side.name(), tag, clickTradingBoxQty, sequenceNumber));

        if (clientSpeedState == ClientSpeedState.TOO_SLOW) {
            final String message =
                    "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", client " + username + " is " +
                            clientSpeedState;
            monitor.logError(ReddalComponents.LADDER_PRESENTER, message);
            ladderClickTradingIssuesPublisher.publish(new LadderClickTradingIssue(symbol, message));
        } else {

            final RemoteOrderType remoteOrderType = getRemoteOrderType(orderType);

            final IOrderCmd submit =
                    new SubmitOrderCmd(symbol, ladderClickTradingIssuesPublisher, username, side, remoteOrderType.orderType,
                            remoteOrderType.algoType, tag, price, clickTradingBoxQty);
            remoteOrderCommandToServerPublisher.publish(submit);
        }
    }

    private void rightClickModify(final ClientSpeedState clientSpeedState, final Map<String, String> data) {

        final long price = Long.valueOf(data.get("price"));
        final ITickTable tickTable = marketData.getBook().getTickTable();
        if (null != modifyFromPrice) {
            if (modifyFromPrice != price) {
                final BookSide passiveSide = price > modifyFromPrice ? BookSide.ASK : BookSide.BID;
                final BookSide aggressiveSide = passiveSide.getOppositeSide();
                final long bidPrice = tickTable.addTicks(modifyFromPrice, ladderModel.getBookPanel().getZoomLevel() - 1);
                final long askPrice = tickTable.subtractTicks(modifyFromPrice, ladderModel.getBookPanel().getZoomLevel() - 1);

                if (!workingOrders.getOrdersInRange(passiveSide, modifyFromPrice, bidPrice).isEmpty()) {
                    modifyOrders(clientSpeedState, price, workingOrders.getOrdersInRange(passiveSide, askPrice, bidPrice));
                } else {
                    modifyOrders(clientSpeedState, price, workingOrders.getOrdersInRange(aggressiveSide, askPrice, bidPrice));
                }
            }
            modifyFromPrice = null;
            modifyFromPriceSelectedTime = 0L;
        } else {
            final long bidPrice = tickTable.addTicks(price, ladderModel.getBookPanel().getZoomLevel() - 1);
            final long askPrice = tickTable.subtractTicks(price, ladderModel.getBookPanel().getZoomLevel() - 1);
            if (workingOrders.hasOrderBetween(askPrice, bidPrice)) {
                modifyFromPrice = price;
                modifyFromPriceSelectedTime = System.currentTimeMillis();
            }
        }
    }

    private void modifyOrders(final ClientSpeedState clientSpeedState, final long toPrice,
            final Map<Long, LinkedHashSet<SourcedWorkingOrder>> aggressiveOrders) {
        for (final LinkedHashSet<SourcedWorkingOrder> orders : aggressiveOrders.values()) {
            for (final SourcedWorkingOrder workingOrder : orders) {
                modifyOrder(clientSpeedState, toPrice, workingOrder, workingOrder.order.getOrderQty());
            }
        }
    }

    private void clearModifyPriceIfTimedOut() {
        if (null != modifyFromPrice && modifyFromPriceSelectedTime + MODIFY_TIMEOUT_MILLI < System.currentTimeMillis()) {
            modifyFromPrice = null;
        }
    }

    private void modifyOrder(final ClientSpeedState clientSpeedState, final long price, final SourcedWorkingOrder sourcedOrder,
            final long totalQuantity) {

        final String sourceNibbler = sourcedOrder.source;
        final WorkingOrder order = sourcedOrder.order;

        trace.publish(new CommandTrace("modify", username, symbol, order.getOrderType().toString(), true, price, order.getSide().toString(),
                order.getTag(), clickTradingBoxQty, order.getChainID()));

        if (isTrader) {

            if (clientSpeedState == ClientSpeedState.TOO_SLOW) {

                monitor.logError(ReddalComponents.LADDER_PRESENTER, "Cannot modify order , client " + username + " is " + clientSpeedState);

            } else if (!tradingStatusForAll.isNibblerConnected(sourceNibbler)) {

                monitor.logError(ReddalComponents.LADDER_PRESENTER, "Cannot modify order: server " + sourceNibbler + " is not connected.");

            } else {

                final IOrderCmd cmd = sourcedOrder.buildModify(ladderClickTradingIssuesPublisher, username, price, (int) totalQuantity);
                remoteOrderCommandToServerPublisher.publish(cmd);
            }
        }
    }

    private void cancelWorkingOrders(final Long rowPrice) {

        if (!pendingRefDataAndSettle && null != workingOrders) {
            for (final BookSide side : BookSide.values()) {
                for (int tick = 0; tick < ladderModel.getBookPanel().getZoomLevel(); tick++) {
                    final long tickPrice = marketData.getBook().getTickTable().addTicks(rowPrice, side.tradeSignum * tick);
                    final LinkedHashSet<SourcedWorkingOrder> workingOrders = this.workingOrders.getWorkingOrdersAtPrice(tickPrice);
                    if (null != workingOrders) {
                        for (final SourcedWorkingOrder order : workingOrders) {
                            if (order.order.getSide() == side) {
                                cancelOrder(order);
                            }
                        }
                    }

                    if (orderUpdatesForSymbol != null) {
                        for (final UpdateFromServer updateFromServer : orderUpdatesForSymbol.updatesByPrice.get(tickPrice).values()) {
                            if (convertSide(updateFromServer.update.getOrder().getSide()) == side) {
                                cancelManagedOrder(updateFromServer);
                            }
                        }
                    }

                }
            }

        }
    }

    private void cancelManagedOrder(final UpdateFromServer updateFromServer) {

        final drw.eeif.eeifoe.RemoteOrder order = updateFromServer.update.getOrder();
        trace.publish(new CommandTrace("cancelManaged", username, symbol, "MANAGED", false, updateFromServer.update.getIndicativePrice(),
                order.getSide().name(), "?", clickTradingBoxQty, updateFromServer.update.getSystemOrderId()));
        if (isTrader) {
            eeifCommandToServer.publish(new OrderEntryCommandToServer(updateFromServer.server,
                    new Cancel(updateFromServer.update.getSystemOrderId(), updateFromServer.update.getOrder())));
        }
    }

    @Override
    public void cancelAllForSide(final BookSide side) {

        if (null != workingOrders) {

            for (final SourcedWorkingOrder order : workingOrders.getAllWorkingOrders()) {

                if (side == order.order.getSide()) {
                    cancelOrder(order);
                }
            }
            orderUpdatesForSymbol.updatesByKey.values().forEach(update -> {
                if (convertSide(update.update.getOrder().getSide()) == side) {
                    cancelManagedOrder(update);
                }
            });
        }
    }

    @Override
    public void zoomIn() {
        ladderModel.getBookPanel().zoomIn();
        final boolean isZoomedOut = ladderModel.getBookPanel().getZoomLevel() != 1;
        ladderModel.setClass(HTML.LADDER, CSSClass.ZOOMED_OUT, isZoomedOut);
        center();
        refresh(symbol);
    }

    @Override
    public void zoomOut() {
        ladderModel.getBookPanel().zoomOut();
        final boolean isZoomedOut = ladderModel.getBookPanel().getZoomLevel() != 1;
        ladderModel.setClass(HTML.LADDER, CSSClass.ZOOMED_OUT, isZoomedOut);
        center();
        refresh(symbol);
    }

    private void cancelOrder(final SourcedWorkingOrder sourcedOrder) {

        final WorkingOrder order = sourcedOrder.order;

        trace.publish(new CommandTrace("cancel", username, symbol, order.getOrderType().toString(), false, order.getPrice(),
                order.getSide().toString(), order.getTag(), clickTradingBoxQty, order.getChainID()));

        if (isTrader) {
            final IOrderCmd cancel = sourcedOrder.buildCancel(ladderClickTradingIssuesPublisher, username, false);
            remoteOrderCommandToServerPublisher.publish(cancel);
        }
    }

    public void onSingleOrderCommand(final ClientSpeedState clientSpeedState, final ISingleOrderCommand singleOrderCommand) {

        final SourcedWorkingOrder order = getSourcedWorkingOrder(singleOrderCommand.getOrderKey());
        if (null == order) {

            monitor.logError(ReddalComponents.LADDER_PRESENTER, "Could not find order for command: " + singleOrderCommand);

        } else if (singleOrderCommand instanceof CancelOrderCmd) {

            cancelOrder(order);

        } else if (singleOrderCommand instanceof ModifyOrderQtyCmd) {

            final long totalQuantity = order.order.getFilledQty() + ((ModifyOrderQtyCmd) singleOrderCommand).newRemainingQuantity;
            modifyOrder(clientSpeedState, order.order.getPrice(), order, totalQuantity);
        }
    }

    private SourcedWorkingOrder getSourcedWorkingOrder(final String key) {

        for (final SourcedWorkingOrder order : workingOrders.getAllWorkingOrders()) {
            if (order.uiKey.equals(key)) {
                return order;
            }
        }
        return null;
    }

    private static <T> Map.Entry<Long, T> iterate(final ITickTable tickTable, final long rowPrice, final int zoomLevel, final BookSide side,
            Map.Entry<Long, T> entry, final Iterator<Map.Entry<Long, T>> it,
            final Consumer<T> accumulator) {
        while (it.hasNext() || entry != null) {
            if (entry != null) {
                final int comparison = Long.compare(tickTable.roundAwayToTick(side, entry.getKey(), zoomLevel), rowPrice);
                if (comparison == 0) {
                    accumulator.accept(entry.getValue());
                    entry = it.hasNext() ? it.next() : null;
                } else if (comparison > 0) {
                    entry = it.hasNext() ? it.next() : null;
                } else {
                    return entry;
                }
            } else {
                entry = it.next();
            }
        }

        return entry;
    }

    private static Long specialCaseCenterPrice(final IBook<?> book) {
        if (book.getInstType() == InstType.FUTURE && book.getSymbol().startsWith("FES1")) {
            return 0L;
        }
        return null;
    }

    private static Long getLastTradeChangeOnDay(final MDForSymbol m) {
        final IBookReferencePrice refPriceData = m.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
        if (refPriceData.isValid() && m.getTradeTracker().hasTrade()) {
            return m.getTradeTracker().getLastPrice() - refPriceData.getPrice();
        } else {
            return null;
        }
    }

    private static Long getYesterdaySettle(final MDForSymbol m) {
        final IBookReferencePrice refPriceData = m.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
        if (refPriceData.isValid()) {
            return refPriceData.getPrice();
        } else {
            return null;
        }
    }

    private static BookSide convertSide(final OrderSide side) {
        if (side == OrderSide.BUY) {
            return BookSide.BID;
        } else {
            return BookSide.ASK;
        }
    }

    private static RemoteOrderType getRemoteOrderType(final String orderType) {

        for (final RemoteOrderType remoteOrderType : RemoteOrderType.values()) {
            if (remoteOrderType.toString().toUpperCase().equals(orderType.toUpperCase())) {
                return remoteOrderType;
            }
        }
        return RemoteOrderType.MANUAL;
    }

    static class RowAccumulator {

        final StringBuilder keys = new StringBuilder();
        final StringBuilder eeifKeys = new StringBuilder();
        final Set<CSSClass> orderTypes = EnumSet.noneOf(CSSClass.class);
        long managedOrderQty = 0;
        long hiddenTickTakerQty = 0;
        long totalQty = 0;
        BookSide side = null;

        void clear() {
            this.managedOrderQty = 0;
            this.hiddenTickTakerQty = 0;
            this.totalQty = 0;
            this.side = null;
            this.keys.setLength(0);
            this.eeifKeys.setLength(0);
            this.orderTypes.clear();
        }

        void addWOs(final LinkedHashSet<SourcedWorkingOrder> workingOrders) {
            for (final SourcedWorkingOrder workingOrder : workingOrders) {
                final WorkingOrder order = workingOrder.order;
                final long orderQty = order.getOrderQty() - order.getFilledQty();
                keys.append(workingOrder.uiKey);
                keys.append('!');
                this.side = workingOrder.order.getSide();
                if (0 < orderQty) {
                    orderTypes.add(workingOrder.cssClass);
                }
                if (AlgoType.HIDDEN_TICK_TAKER == order.getAlgoType()) {
                    hiddenTickTakerQty += orderQty;
                } else {
                    totalQty += orderQty;
                }
            }
        }

        void addOEUpdates(final HashMap<String, UpdateFromServer> updatesFromServer) {
            for (final UpdateFromServer update : updatesFromServer.values()) {
                this.side = convertSide(update.update.getOrder().getSide());
                managedOrderQty += update.update.getRemainingQty();
                keys.append(update.key);
                keys.append('!');
                eeifKeys.append(update.key);
                eeifKeys.append('!');
            }
        }
    }
}
