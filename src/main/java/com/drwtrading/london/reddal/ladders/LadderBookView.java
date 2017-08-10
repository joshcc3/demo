package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.Metadata;
import com.drwtrading.london.photons.eeifoe.OrderSide;
import com.drwtrading.london.photons.eeifoe.Submit;
import com.drwtrading.london.photons.reddal.Command;
import com.drwtrading.london.photons.reddal.Direction;
import com.drwtrading.london.photons.reddal.ReddalCommand;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.photons.reddal.UpdateOffset;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.orderentry.ManagedOrderType;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderentry.UpdateFromServer;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.util.EnumSwitcher;
import com.drwtrading.london.reddal.util.Mathematics;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.photons.ladder.LaserLine;
import com.google.common.collect.ImmutableSet;
import drw.london.json.Jsonable;
import eeif.execution.RemoteModifyOrder;
import eeif.execution.RemoteOrder;
import eeif.execution.RemoteOrderType;
import eeif.execution.RemoteSubmitOrder;
import eeif.execution.Side;
import eeif.execution.WorkingOrderState;
import eeif.execution.WorkingOrderType;
import eeif.execution.WorkingOrderUpdate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LadderBookView implements ILadderBoard {

    private static final int MODIFY_TIMEOUT_MILLI = 5000;

    private static final int REALLY_BIG_NUMBER_THRESHOLD = 100000;
    private static final double DEFAULT_EQUITY_NOTIONAL_EUR = 100000.0;

    private static final DecimalFormat BASIS_POINT_DECIMAL_FORMAT = NumberFormatUtil.getDF(".0");
    private static final DecimalFormat EFP_DECIMAL_FORMAT = NumberFormatUtil.getDF("0.00");
    private static final DecimalFormat FX_DECIMAL_FORMAT = NumberFormatUtil.getDF(".0");
    private static final NumberFormat BIG_NUMBER_DF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE + 'M', 0, 2);

    private static final Metadata LADDER_SOURCE_METADATA = new Metadata("SOURCE", "LADDER");

    private static final String WHITE_LASER_LINE_ID = "white";

    private static final int AUTO_RECENTER_TICKS = 3;

    private static final Set<String> TAGS = ImmutableSet.of("CHAD", "DIV", "STRING", "CLICKNOUGHT", "GLABN");

    private static final EnumMap<WorkingOrderType, CSSClass> WORKING_ORDER_CSS;

    static {
        WORKING_ORDER_CSS = new EnumMap<>(WorkingOrderType.class);
        for (final WorkingOrderType workingOrderType : WorkingOrderType.values()) {
            final CSSClass cssClass = CSSClass.valueOf("WORKING_ORDER_TYPE_" + LadderView.getOrderType(workingOrderType).toUpperCase());
            WORKING_ORDER_CSS.put(workingOrderType, cssClass);
        }
    }

    private static final Set<String> PERSISTENT_PREFS = new HashSet<>();

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

    private final UiPipeImpl ui;
    private final ILadderUI view;

    private final LadderOptions ladderOptions;

    private final LadderPrefsForSymbolUser ladderPrefsForSymbolUser;
    private final Map<String, String> defaultPrefs;

    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuesPublisher;
    private final Publisher<ReddalMessage> commandPublisher;
    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher;
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer;
    private final TradingStatusForAll tradingStatusForAll;

    private final MDForSymbol marketData;
    private final WorkingOrdersForSymbol workingOrdersForSymbol;
    private final ExtraDataForSymbol dataForSymbol;
    private final OrderUpdatesForSymbol orderUpdatesForSymbol;

    private final EnumSwitcher<PricingMode> pricingModes;
    private final Map<String, Integer> buttonQty;

    private final int levels;
    private final LadderHTMLTable ladderHTMLKeys;
    private final LongMap<LadderBoardRow> priceRows;

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

    private int clickTradingBoxQty = 0;
    private int orderSeqNo = 0;

    private Long modifyFromPrice;
    private long modifyFromPriceSelectedTime;

    LadderBookView(final IResourceMonitor<ReddalComponents> monitor, final String username, final boolean isTrader, final String symbol,
            final UiPipeImpl ui, final ILadderUI view, final LadderOptions ladderOptions,
            final LadderPrefsForSymbolUser ladderPrefsForSymbolUser,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssuesPublisher, final Publisher<ReddalMessage> commandPublisher,
            final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher,
            final Publisher<OrderEntryCommandToServer> eeifCommandToServer, final TradingStatusForAll tradingStatusForAll,
            final MDForSymbol marketData, final WorkingOrdersForSymbol workingOrdersForSymbol, final ExtraDataForSymbol extraDataForSymbol,
            final OrderUpdatesForSymbol orderUpdatesForSymbol, final int levels, final LadderHTMLTable ladderHTMLKeys,
            final Publisher<Jsonable> trace, final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap,
            final long centeredPrice) {

        this.monitor = monitor;

        this.username = username;
        this.isTrader = isTrader;
        this.symbol = symbol;

        this.ui = ui;
        this.view = view;

        this.ladderOptions = ladderOptions;

        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;

        this.defaultPrefs = new HashMap<>();
        this.defaultPrefs.put(HTML.WORKING_ORDER_TAG, "CHAD");
        if (symbol.startsWith("FDAX")) {
            this.defaultPrefs.put(HTML.INP_RELOAD, "5");
        } else if (username.startsWith("dcook")) {
            this.defaultPrefs.put(HTML.INP_RELOAD, "0");
        } else {
            this.defaultPrefs.put(HTML.INP_RELOAD, "50");
        }
        this.defaultPrefs.put(HTML.ORDER_TYPE_LEFT, "HAWK");
        this.defaultPrefs.put(HTML.ORDER_TYPE_RIGHT, "MANUAL");
        this.defaultPrefs.put(HTML.RANDOM_RELOAD_CHECK, "true");

        this.ladderClickTradingIssuesPublisher = ladderClickTradingIssuesPublisher;
        this.commandPublisher = commandPublisher;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.eeifCommandToServer = eeifCommandToServer;
        this.tradingStatusForAll = tradingStatusForAll;

        this.marketData = marketData;
        this.workingOrdersForSymbol = workingOrdersForSymbol;
        this.dataForSymbol = extraDataForSymbol;
        this.orderUpdatesForSymbol = orderUpdatesForSymbol;

        this.levels = levels;
        this.ladderHTMLKeys = ladderHTMLKeys;
        this.pricingModes = new EnumSwitcher<>(PricingMode.class, PricingMode.values());
        this.buttonQty = new HashMap<>();

        this.priceRows = new LongMap<>();

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
    }

    @Override
    public void setStackTickSize(final double tickSize) {
        // ignored
    }

    @Override
    public void setStackGroupTickMultiplier(final int tickMultiplier) {
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

        ui.cls(HTML.LADDER_DIV, CSSClass.STACK_VIEW, false);

        view.trading(isTrader, TAGS, filterUsableOrderTypes(ladderOptions.orderTypesLeft),
                filterUsableOrderTypes(ladderOptions.orderTypesRight));

        ui.clickable('#' + HTML.YESTERDAY_SETTLE);
        ui.clickable('#' + HTML.LAST_TRADE_COD);

        ui.cls(HTML.RANDOM_RELOAD, CSSClass.INVISIBLE, false);

        ui.cls(HTML.ORDER_TYPE_LEFT, CSSClass.FULL_WIDTH, false);
        ui.cls(HTML.ORDER_TYPE_RIGHT, CSSClass.FULL_WIDTH, false);

        ui.cls(HTML.STACK_CONFIG_BUTTON, CSSClass.INVISIBLE, true);
        ui.cls(HTML.STACKS_CONTROL, CSSClass.INVISIBLE, true);

        for (final Map.Entry<String, Integer> entry : buttonQty.entrySet()) {
            final String display = LadderView.formatClickQty(entry.getValue());
            ui.txt(entry.getKey(), display);
        }

        for (int i = 0; i < levels; i++) {
            ui.clickable('#' + HTML.VOLUME + i);
        }
    }

    @Override
    public void timedRefresh() {

        clearModifyPriceIfTimedOut();

        if (pendingRefDataAndSettle && null != marketData && null != marketData.getBook() && marketData.getBook().isValid()) {

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

            for (final Map.Entry<String, Integer> entry : buttonQty.entrySet()) {
                final String display = LadderView.formatClickQty(entry.getValue());
                ui.txt(entry.getKey(), display);
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

    void drawPricingButtons() {

        for (final PricingMode mode : pricingModes.getUniverse()) {
            ui.cls(HTML.PRICING + mode, CSSClass.INVISIBLE, !pricingModes.isValidChoice(mode));
            ui.cls(HTML.PRICING + mode, CSSClass.ACTIVE_MODE, pricingModes.get() == mode);
        }
    }

    @Override
    public boolean moveTowardsCenter() {

        if (null != marketData.getBook() && marketData.getBook().isValid()) {

            final long bookCenter = getCenterPrice();
            if (bookCenter < bottomPrice || topPrice < bookCenter) {
                final long newCentrePrice;
                if (0 == centeredPrice) {
                    newCentrePrice = bookCenter;
                } else {
                    final long direction = (long) Math.signum(getCenterPrice() - centeredPrice);
                    newCentrePrice = marketData.getBook().getTickTable().addTicks(centeredPrice, AUTO_RECENTER_TICKS * direction);
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

    @Override
    public void setCenteredPrice(final long newCenterPrice) {

        if (null != marketData && null != marketData.getBook()) {

            this.centeredPrice = this.marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, newCenterPrice);

            final int centerLevel = levels / 2;
            topPrice = marketData.getBook().getTickTable().addTicks(this.centeredPrice, centerLevel);
            priceRows.clear();

            long price = topPrice;
            for (int i = 0; i < levels; ++i) {

                final String formattedPrice = marketData.formatPrice(price);
                final LadderHTMLRow htmlRowKeys = ladderHTMLKeys.getRow(i);
                final LadderBoardRow ladderBookRow = new LadderBoardRow(formattedPrice, htmlRowKeys);

                priceRows.put(price, ladderBookRow);

                bottomPrice = price;
                price = marketData.getBook().getTickTable().addTicks(price, -1);
            }
        }
    }

    private long getCenterPrice() {

        final IBook<?> book = marketData.getBook();
        if (!pendingRefDataAndSettle && null != book && book.isValid()) {

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
            } else if (null != workingOrdersForSymbol && !workingOrdersForSymbol.ordersByKey.isEmpty()) {
                final long n = workingOrdersForSymbol.ordersByKey.size();
                long avgPrice = 0L;
                for (final WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
                    avgPrice += orderUpdateFromServer.value.getPrice() / n;
                }
                center = avgPrice;
            } else if (auctionSummaryPrice.isValid()) {
                center = auctionSummaryPrice.getPrice();
            } else if (marketData.getTradeTracker().hasTrade()) {
                center = marketData.getTradeTracker().getLastPrice();
            } else if (yestClose.isValid()) {
                center = yestClose.getPrice();
            } else {
                center = 0;
            }
            return marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, center);
        } else {
            return 0;
        }
    }

    private void drawPriceLevels() {

        if (!pendingRefDataAndSettle) {
            final LaserLine theo;
            if (null != dataForSymbol && dataForSymbol.laserLineByName.containsKey(WHITE_LASER_LINE_ID)) {
                theo = dataForSymbol.laserLineByName.get(WHITE_LASER_LINE_ID);
            } else if (null != dataForSymbol && dataForSymbol.laserLineByName.containsKey(ladderOptions.theoLaserLineID)) {
                theo = dataForSymbol.laserLineByName.get(ladderOptions.theoLaserLineID);
            } else {
                theo = new LaserLine(null, ladderOptions.theoLaserLineID, Long.MIN_VALUE, false, "");
            }
            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {
                final long price = priceNode.key;
                final LadderHTMLRow htmlRowKeys = priceNode.getValue().htmlKeys;
                if (pricingModes.get() == PricingMode.BPS && theo.isValid()) {
                    final double points = (10000.0 * (price - theo.getPrice())) / theo.getPrice();
                    ui.txt(htmlRowKeys.bookPriceKey, BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (pricingModes.get() == PricingMode.BPS && isCashEquityOrFX && hasBestBid() && null != marketData) {
                    final long basePrice = marketData.getBook().getBestBid().getPrice();
                    final double points = (10000.0 * (price - basePrice)) / basePrice;
                    ui.txt(htmlRowKeys.bookPriceKey, BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (PricingMode.EFP == pricingModes.get() && null != marketData && theo.isValid()) {
                    final double efp = Math.round((price - theo.getPrice()) * 100d / Constants.NORMALISING_FACTOR) / 100d;
                    ui.txt(htmlRowKeys.bookPriceKey, EFP_DECIMAL_FORMAT.format(efp));
                } else if (PricingMode.EFP == pricingModes.get() && null != marketData && marketData.isPriceInverted()) {
                    final double invertedPrice = Constants.NORMALISING_FACTOR / (double) price;
                    ui.txt(htmlRowKeys.bookPriceKey, FX_DECIMAL_FORMAT.format(invertedPrice));
                } else {
                    final LadderBoardRow priceRow = priceRows.get(price);
                    ui.txt(htmlRowKeys.bookPriceKey, priceRow.formattedPrice);
                }

                ui.data(htmlRowKeys.bookBidKey, DataKey.PRICE, price);
                ui.data(htmlRowKeys.bookAskKey, DataKey.PRICE, price);
                ui.data(htmlRowKeys.bookOrderKey, DataKey.PRICE, price);
            }
        }
    }

    private boolean hasBestBid() {
        return null != marketData.getBook() && null != marketData.getBook().getBestBid();
    }

    private void drawMetaData() {

        if (!pendingRefDataAndSettle && null != dataForSymbol && null != marketData) {

            drawLaserLines();

            /* Change on day*/
            final Long lastTradeChangeOnDay = getLastTradeChangeOnDay(marketData);
            if (lastTradeChangeOnDay != null) {
                ui.txt(HTML.LAST_TRADE_COD, marketData.formatPrice(lastTradeChangeOnDay));
            }
            LadderView.decorateUpDown(ui, HTML.LAST_TRADE_COD, lastTradeChangeOnDay);

            /* Yesterday settle */
            final Long yesterdaySettle = getYesterdaySettle(marketData);
            if (null != yesterdaySettle) {
                ui.txt(HTML.YESTERDAY_SETTLE, marketData.formatPriceWithoutTrailingZeroes(yesterdaySettle));
            }

            ui.cls(HTML.YESTERDAY_SETTLE, CSSClass.INVISIBLE, !showYesterdaySettleInsteadOfCOD);
            ui.cls(HTML.LAST_TRADE_COD, CSSClass.INVISIBLE, showYesterdaySettleInsteadOfCOD);

            for (final LongMapNode<LadderBoardRow> entry : priceRows) {
                final long price = entry.key;
                final String priceKey = entry.getValue().htmlKeys.bookPriceKey;
                ui.cls(priceKey, CSSClass.LAST_BID, dataForSymbol.lastBuy != null && dataForSymbol.lastBuy.getPrice() == price);
                ui.cls(priceKey, CSSClass.LAST_ASK, dataForSymbol.lastSell != null && dataForSymbol.lastSell.getPrice() == price);
            }
        }
    }

    private Collection<String> filterUsableOrderTypes(final Collection<CSSClass> types) {
        if (null != marketData && null != marketData.getBook()) {
            final String mic = marketData.getBook().getMIC().name();
            return types.stream().filter(orderType -> isOrderTypeSupported(orderType, mic)).map(Enum::name).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isOrderTypeSupported(final CSSClass orderType, final String mic) {

        return TAGS.stream().filter(tag -> {
            boolean oldOrderType = ladderOptions.serverResolver.resolveToServerName(symbol, orderType.name(), tag, mic) != null;
            boolean newOrderType = orderEntryMap.containsKey(symbol) && managedOrderTypes.contains(orderType.name()) &&
                    orderEntryMap.get(symbol).supportedTypes.contains(ManagedOrderType.valueOf(orderType.name()));
            return oldOrderType || newOrderType;
        }).findAny().isPresent();
    }

    private void drawClickTrading() {

        if (null != ladderPrefsForSymbolUser) {

            ui.txt(HTML.INP_QTY, clickTradingBoxQty);

            for (final String pref : PERSISTENT_PREFS) {
                ui.txt(pref, getPref(pref));
            }

            final String leftOrderPricePref = getPref(HTML.ORDER_TYPE_LEFT);
            for (final CSSClass type : ladderOptions.orderTypesLeft) {
                ui.cls(HTML.ORDER_TYPE_LEFT, type, type.name().equals(leftOrderPricePref));
            }

            final String rightOrderPricePref = getPref(HTML.ORDER_TYPE_RIGHT);
            for (final CSSClass type : ladderOptions.orderTypesRight) {
                ui.cls(HTML.ORDER_TYPE_RIGHT, type, type.name().equals(rightOrderPricePref));
            }
        }

        if (!pendingRefDataAndSettle) {
            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {
                final long price = priceNode.key;
                final LadderHTMLRow htmlRowKeys = priceNode.getValue().htmlKeys;
                ui.cls(htmlRowKeys.bookOrderKey, CSSClass.MODIFY_PRICE_SELECTED, null != modifyFromPrice && price == modifyFromPrice);
            }
        }

        if (null != dataForSymbol) {
            ui.cls(HTML.OFFSET_CONTROL, CSSClass.INVISIBLE, !dataForSymbol.symbolAvailable);
        }
    }

    private String getPref(final String id) {
        return ladderPrefsForSymbolUser.get(id, defaultPrefs.get(id));
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

    private void drawLaserLines() {

        if (null != marketData.getBook() && marketData.getBook().isValid()) {
            for (final LaserLine laserLine : dataForSymbol.laserLineByName.values()) {

                final String laserKey = HTML.LASER + ("offer".equals(laserLine.getId()) ? "ask" : laserLine.getId());

                if (laserLine.isValid() && 0 < levels) {

                    if (topPrice < laserLine.getPrice()) {
                        final LadderBoardRow priceRow = priceRows.get(topPrice);
                        ui.height(laserKey, priceRow.htmlKeys.bookPriceKey, 0.5);
                    } else if (laserLine.getPrice() < bottomPrice) {
                        final LadderBoardRow priceRow = priceRows.get(bottomPrice);
                        ui.height(laserKey, priceRow.htmlKeys.bookPriceKey, -0.5);
                    } else {
                        long price = bottomPrice;
                        while (price <= topPrice) {
                            final long priceAbove = marketData.getBook().getTickTable().addTicks(price, 1);
                            if (price <= laserLine.getPrice() && laserLine.getPrice() <= priceAbove && priceRows.containsKey(price)) {
                                final long fractionalPrice = laserLine.getPrice() - price;
                                final double tickFraction = 1.0 * fractionalPrice / (priceAbove - price);
                                final LadderBoardRow priceRow = priceRows.get(price);
                                ui.height(laserKey, priceRow.htmlKeys.bookPriceKey, tickFraction);
                                break;
                            }
                            price = priceAbove;
                        }
                    }

                    ui.cls(laserKey, CSSClass.INVISIBLE, false);
                } else {
                    ui.cls(laserKey, CSSClass.INVISIBLE, true);
                }
            }
        }
    }

    private void drawBook(final String symbol) {

        if (!pendingRefDataAndSettle && null != marketData && null != marketData.getBook()) {
            switch (marketData.getBook().getStatus()) {
                case CONTINUOUS: {
                    ui.txt(HTML.SYMBOL, symbol);
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case AUCTION: {
                    ui.txt(HTML.SYMBOL, symbol + " - AUC");
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, true);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case CLOSED: {
                    ui.txt(HTML.SYMBOL, symbol + " - CLSD");
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, true);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                default: {
                    ui.txt(HTML.SYMBOL, symbol + " - ?");
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, true);
                    break;
                }
            }
            ui.title(symbol);

            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {

                final long price = priceNode.key;
                final LadderBoardRow bookRow = priceNode.getValue();

                final IBookLevel bidLevel = marketData.getBook().getBidLevel(price);
                final IBookLevel askLevel = marketData.getBook().getAskLevel(price);

                if (null == bidLevel) {
                    bidQty(bookRow.htmlKeys, 0);
                    ui.cls(bookRow.htmlKeys.bookBidKey, CSSClass.IMPLIED_BID, false);
                } else {
                    bidQty(bookRow.htmlKeys, bidLevel.getQty());
                    ui.cls(bookRow.htmlKeys.bookBidKey, CSSClass.IMPLIED_BID, 0 < bidLevel.getImpliedQty());
                }

                if (null == askLevel) {
                    askQty(bookRow.htmlKeys, 0);
                    ui.cls(bookRow.htmlKeys.bookAskKey, CSSClass.IMPLIED_ASK, false);
                } else {
                    askQty(bookRow.htmlKeys, askLevel.getQty());
                    ui.cls(bookRow.htmlKeys.bookAskKey, CSSClass.IMPLIED_ASK, 0 < askLevel.getImpliedQty());
                }
            }

            ui.cls(HTML.BOOK_TABLE, CSSClass.AUCTION, BookMarketState.AUCTION == marketData.getBook().getStatus());

            if (BookMarketState.AUCTION == marketData.getBook().getStatus()) {

                final IBookReferencePrice auctionIndicative = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
                final LadderBoardRow bookRow = priceRows.get(auctionIndicative.getPrice());

                if (auctionIndicative.isValid() && null != bookRow) {

                    final long auctionQty = auctionIndicative.getQty();

                    bidQty(bookRow.htmlKeys, auctionQty);
                    askQty(bookRow.htmlKeys, auctionQty);

                    ui.cls(bookRow.htmlKeys.bookBidKey, CSSClass.AUCTION, true);
                    ui.cls(bookRow.htmlKeys.bookAskKey, CSSClass.AUCTION, true);
                }
            }
        }
    }

    private void drawWorkingOrders() {

        final WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && null != w && null != orderUpdatesForSymbol) {

            final StringBuilder keys = new StringBuilder();
            final StringBuilder eeifKeys = new StringBuilder();
            final Set<WorkingOrderType> orderTypes = EnumSet.noneOf(WorkingOrderType.class);

            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {

                final long price = priceNode.key;
                final LadderHTMLRow htmlRowKey = priceNode.getValue().htmlKeys;

                keys.setLength(0);
                eeifKeys.setLength(0);
                orderTypes.clear();

                int managedOrderQty = 0;
                int hiddenTickTakerQty = 0;
                int totalQty = 0;
                BookSide side = null;

                for (final WorkingOrderUpdateFromServer orderFromServer : w.ordersByPrice.get(price)) {
                    final WorkingOrderUpdate order = orderFromServer.value;
                    final int orderQty = order.getTotalQuantity() - order.getFilledQuantity();
                    side = LadderView.convertSide(order.getSide());
                    keys.append(orderFromServer.key());
                    keys.append('!');
                    if (0 < orderQty) {
                        orderTypes.add(orderFromServer.value.getWorkingOrderType());
                    }
                    if (WorkingOrderType.HIDDEN_TICKTAKER == order.getWorkingOrderType()) {
                        hiddenTickTakerQty += orderQty;
                    } else {
                        totalQty += orderQty;
                    }
                }

                for (final UpdateFromServer update : orderUpdatesForSymbol.getOrdersForPrice(price)) {
                    managedOrderQty += update.update.getRemainingQty();
                    side = convertSide(update.update.getOrder().getSide());
                    keys.append(update.key);
                    keys.append('!');
                    eeifKeys.append(update.key);
                    eeifKeys.append('!');
                }

                totalQty += Math.max(managedOrderQty, hiddenTickTakerQty);
                workingQty(htmlRowKey, totalQty, side, orderTypes, 0 < managedOrderQty);

                if (0 < keys.length()) {
                    keys.setLength(keys.length() - 1);
                }
                if (0 < eeifKeys.length()) {
                    eeifKeys.setLength(eeifKeys.length() - 1);
                }

                ui.data(htmlRowKey.bookOrderKey, DataKey.ORDER, keys);
                ui.data(htmlRowKey.bookOrderKey, DataKey.EEIF, eeifKeys);
            }
            int buyQty = 0;
            int sellQty = 0;
            int buyHiddenTTQty = 0;
            int sellHiddenTTQty = 0;
            int buyManagedQty = 0;
            int sellManagedQty = 0;
            for (final WorkingOrderUpdateFromServer orderUpdateFromServer : w.ordersByKey.values()) {
                if (orderUpdateFromServer.value.getWorkingOrderState() != WorkingOrderState.DEAD) {
                    final int remainingQty =
                            orderUpdateFromServer.value.getTotalQuantity() - orderUpdateFromServer.value.getFilledQuantity();
                    if (orderUpdateFromServer.value.getSide() == Side.BID) {
                        if (orderUpdateFromServer.value.getWorkingOrderType() == WorkingOrderType.HIDDEN_TICKTAKER) {
                            buyHiddenTTQty += remainingQty;
                        } else {
                            buyQty += remainingQty;
                        }
                    } else {
                        if (orderUpdateFromServer.value.getWorkingOrderType() == WorkingOrderType.HIDDEN_TICKTAKER) {
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
            ui.cls(HTML.BUY_QTY, CSSClass.INVISIBLE, 0 == buyQty);
            ui.txt(HTML.BUY_QTY, buyQty);
            ui.cls(HTML.SELL_QTY, CSSClass.INVISIBLE, 0 == sellQty);
            ui.txt(HTML.SELL_QTY, sellQty);
        }
    }

    private void drawTradedVolumes() {

        final MDForSymbol m = this.marketData;
        if (!pendingRefDataAndSettle && null != m) {

            final TradeTracker tradeTracker = m.getTradeTracker();
            final long lastTradePrice = tradeTracker.getQtyRunAtLastPrice();

            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {

                final long price = priceNode.key;
                final LadderHTMLRow htmlRowKeys = priceNode.getValue().htmlKeys;

                final Long qty = tradeTracker.getTotalQtyTradedAtPrice(price);
                if (null != qty) {
                    ui.txt(htmlRowKeys.bookVolumeKey, formatMktQty(qty));
                } else {
                    ui.txt(htmlRowKeys.bookVolumeKey, HTML.EMPTY);
                }

                final boolean withinTradedRange = tradeTracker.getMinTradedPrice() <= price && price <= tradeTracker.getMaxTradedPrice();
                ui.cls(htmlRowKeys.bookPriceKey, CSSClass.PRICE_TRADED, withinTradedRange);

                if (tradeTracker.hasTrade() && price == tradeTracker.getLastPrice()) {
                    ui.txt(htmlRowKeys.bookTradeKey, formatMktQty(lastTradePrice));
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.INVISIBLE, false);
                    ui.cls(htmlRowKeys.bookVolumeKey, CSSClass.INVISIBLE, true);
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.TRADED_UP, tradeTracker.isLastTickUp());
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.TRADED_DOWN, tradeTracker.isLastTickDown());
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.TRADED_AGAIN, tradeTracker.isLastTradeSameLevel());
                } else {
                    ui.txt(htmlRowKeys.bookTradeKey, HTML.EMPTY);
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.INVISIBLE, true);
                    ui.cls(htmlRowKeys.bookVolumeKey, CSSClass.INVISIBLE, false);
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.TRADED_UP, false);
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.TRADED_DOWN, false);
                    ui.cls(htmlRowKeys.bookTradeKey, CSSClass.TRADED_AGAIN, false);
                }
            }

            ui.txt(HTML.VOLUME + '0', marketData.getBook().getCCY().name());
            ui.cls(HTML.VOLUME + '0', CSSClass.CCY, true);
            final long quantityTraded = tradeTracker.getTotalQtyTraded();
            if (1000000 < quantityTraded) {
                ui.txt(HTML.TOTAL_TRADED_VOLUME, BASIS_POINT_DECIMAL_FORMAT.format(1.0 / 1000000 * quantityTraded) + 'M');
            } else if (1000 < quantityTraded) {
                ui.txt(HTML.TOTAL_TRADED_VOLUME, BASIS_POINT_DECIMAL_FORMAT.format(1.0 / 1000 * quantityTraded) + 'K');
            } else {
                ui.txt(HTML.TOTAL_TRADED_VOLUME, quantityTraded);
            }
        }
    }

    private void workingQty(final LadderHTMLRow htmlRowKeys, final int qty, final BookSide side, final Set<WorkingOrderType> orderTypes,
            final boolean hasEeifOEOrder) {

        ui.txt(htmlRowKeys.bookOrderKey, formatMktQty(qty));
        ui.cls(htmlRowKeys.bookOrderKey, CSSClass.WORKING_QTY, 0 < qty);
        ui.cls(htmlRowKeys.bookOrderKey, CSSClass.WORKING_BID, BookSide.BID == side);
        ui.cls(htmlRowKeys.bookOrderKey, CSSClass.WORKING_OFFER, BookSide.ASK == side);
        for (final WorkingOrderType workingOrderType : WorkingOrderType.values()) {
            final CSSClass cssClass = WORKING_ORDER_CSS.get(workingOrderType);
            ui.cls(htmlRowKeys.bookOrderKey, cssClass, !hasEeifOEOrder && orderTypes.contains(workingOrderType));
        }
        ui.cls(htmlRowKeys.bookOrderKey, CSSClass.EEIF_ORDER_TYPE, hasEeifOEOrder);
    }

    @Override
    public void scrollUp() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, 1);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void scrollDown() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final long newCenterPrice = book.getTickTable().subtractTicks(centeredPrice, 1);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void pageUp() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {

            final int n = levels - 1;
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, n);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void pageDown() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {

            final int n = -1 * (levels - 1);
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, n);
            setCenteredPrice(newCenterPrice);
        }
    }

    @Override
    public void onClick(final ClientSpeedState clientSpeedState, final String label, final String button, final Map<String, String> data) {

        if ("left".equals(button)) {
            if (buttonQty.containsKey(label)) {
                clickTradingBoxQty += buttonQty.get(label);
            } else if (HTML.BUTTON_CLR.equals(label)) {
                clickTradingBoxQty = 0;
            } else if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (null != ladderPrefsForSymbolUser) {
                    submitOrderLeftClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                final long price = Long.valueOf(data.get("price"));
                final LadderHTMLRow htmlRowKeys = priceRows.get(price).htmlKeys;
                if (label.equals(htmlRowKeys.bookOrderKey)) {
                    cancelWorkingOrders(price);
                } else {
                    monitor.logError(ReddalComponents.LADDER_PRESENTER,
                            "Mismatched label: " + data.get("price") + ' ' + htmlRowKeys.bookOrderKey + ' ' + label);
                }
            } else if (label.equals(HTML.BUY_OFFSET_UP)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.BID, Direction.UP));
            } else if (label.equals(HTML.BUY_OFFSET_DOWN)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.BID, Direction.DOWN));
            } else if (label.equals(HTML.SELL_OFFSET_UP)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.OFFER, Direction.UP));
            } else if (label.equals(HTML.SELL_OFFSET_DOWN)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.OFFER, Direction.DOWN));
            } else if (label.equals(HTML.START_BUY)) {
                commandPublisher.publish(new Command(ReddalCommand.START, symbol, com.drwtrading.london.photons.reddal.Side.BID));
            } else if (label.equals(HTML.START_SELL)) {
                commandPublisher.publish(new Command(ReddalCommand.START, symbol, com.drwtrading.london.photons.reddal.Side.OFFER));
            } else if (label.equals(HTML.STOP_BUY)) {
                commandPublisher.publish(new Command(ReddalCommand.STOP, symbol, com.drwtrading.london.photons.reddal.Side.BID));
            } else if (label.equals(HTML.STOP_SELL)) {
                commandPublisher.publish(new Command(ReddalCommand.STOP, symbol, com.drwtrading.london.photons.reddal.Side.OFFER));
            } else if (label.equals(HTML.PRICING_BPS)) {
                pricingModes.set(PricingMode.BPS);
            } else if (label.equals(HTML.PRICING_RAW)) {
                pricingModes.set(PricingMode.RAW);
            } else if (label.equals(HTML.PRICING_EFP)) {
                pricingModes.set(PricingMode.EFP);
            } else if (label.startsWith(DataKey.PRICE.key)) {
                pricingModes.next();
            } else if (label.startsWith(HTML.VOLUME)) {
                view.launchBasket(symbol);
            } else if (label.equals(HTML.YESTERDAY_SETTLE) || label.equals(HTML.LAST_TRADE_COD)) {
                showYesterdaySettleInsteadOfCOD = !showYesterdaySettleInsteadOfCOD;
            }
        } else if ("right".equals(button)) {
            if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (ladderPrefsForSymbolUser != null) {
                    submitOrderRightClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                rightClickModify(clientSpeedState, data);
            }
        } else if ("middle".equals(button)) {
            if (label.startsWith(HTML.ORDER)) {
                final String price = data.get("price");
                final String url = "/orders#" + symbol + ',' + price;
                final Collection<WorkingOrderUpdateFromServer> orders = workingOrdersForSymbol.ordersByPrice.get(Long.valueOf(price));
                if (!orders.isEmpty()) {
                    view.popUp(url, "orders", 270, 20 * (1 + orders.size()));
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

    private void bidQty(final LadderHTMLRow htmlRowKeys, final long qty) {
        ui.txt(htmlRowKeys.bookBidKey, formatMktQty(qty));
        ui.cls(htmlRowKeys.bookBidKey, CSSClass.BID_ACTIVE, 0 < qty);
        ui.cls(htmlRowKeys.bookBidKey, CSSClass.AUCTION, false);
    }

    private void askQty(final LadderHTMLRow htmlRowKeys, final long qty) {
        ui.txt(htmlRowKeys.bookAskKey, formatMktQty(qty));
        ui.cls(htmlRowKeys.bookAskKey, CSSClass.ASK_ACTIVE, 0 < qty);
        ui.cls(htmlRowKeys.bookAskKey, CSSClass.AUCTION, false);
    }

    private static String formatMktQty(final long qty) {
        if (qty <= 0) {
            return HTML.EMPTY;
        } else if (REALLY_BIG_NUMBER_THRESHOLD <= qty) {
            final double d = qty / 1000000d;
            return BIG_NUMBER_DF.format(d);
        } else {
            return Long.toString(qty);
        }
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
        final LadderBoardRow bookRow = priceRows.get(price);

        final Side side;
        if (label.equals(bookRow.htmlKeys.bookBidKey)) {
            side = Side.BID;
        } else if (label.equals(bookRow.htmlKeys.bookAskKey)) {
            side = Side.OFFER;
        } else {
            throw new IllegalArgumentException("Price " + price + " did not match key " + label);
        }

        final String tag = ladderPrefsForSymbolUser.get(HTML.WORKING_ORDER_TAG);

        if (null == tag) {
            throw new IllegalArgumentException("No tag provided.");
        } else {

            if (orderType != null && clickTradingBoxQty > 0) {
                if (managedOrderTypes.contains(orderType)) {
                    submitManagedOrder(orderType, price, side, tag);
                } else if (oldOrderTypes.contains(orderType)) {
                    submitOrder(clientSpeedState, orderType, price, side, tag);
                } else {
                    LadderView.clickTradingIssue(ui, new LadderClickTradingIssue(symbol, "Unknown order type: " + orderType));
                    return;
                }
            }

            final boolean randomReload = "true".equals(getPref(HTML.RANDOM_RELOAD_CHECK));
            final int reloadBoxQty = Integer.valueOf(getPref(HTML.INP_RELOAD));

            if (randomReload) {
                clickTradingBoxQty = Math.max(0, reloadBoxQty - (int) (Math.random() * ladderOptions.randomReloadFraction * reloadBoxQty));
            } else {
                clickTradingBoxQty = Math.max(0, reloadBoxQty);
            }
        }
    }

    private void submitManagedOrder(final String orderType, final long price, final Side side, final String tag) {

        int tradingBoxQty = this.clickTradingBoxQty;
        trace.publish(new CommandTrace("submitManaged", username, symbol, orderType, true, price, side.toString(), tag, tradingBoxQty,
                orderSeqNo++));
        final OrderEntryClient.SymbolOrderChannel symbolOrderChannel = orderEntryMap.get(symbol);
        if (null != symbolOrderChannel) {
            final ManagedOrderType managedOrderType = ManagedOrderType.valueOf(orderType);
            if (!symbolOrderChannel.supportedTypes.contains(managedOrderType)) {
                LadderView.clickTradingIssue(ui,
                        new LadderClickTradingIssue(symbol, "Does not support order type " + orderType + " for symbol " + symbol));
                return;
            }
            tradingBoxQty = managedOrderType.getQty(tradingBoxQty);
            if (0 == tradingBoxQty) {
                tradingBoxQty = clickTradingBoxQty;
            }
            final OrderSide orderSide = side == Side.BID ? OrderSide.BUY : OrderSide.SELL;
            final com.drwtrading.london.photons.eeifoe.RemoteOrder remoteOrder =
                    new com.drwtrading.london.photons.eeifoe.RemoteOrder(symbol, orderSide, price, tradingBoxQty, username,
                            managedOrderType.getOrder(price, tradingBoxQty, orderSide),
                            new ObjectArrayList<>(Arrays.asList(LADDER_SOURCE_METADATA, new Metadata("TAG", tag))));
            final Submit submit = new Submit(remoteOrder);
            symbolOrderChannel.publisher.publish(submit);
        } else {
            LadderView.clickTradingIssue(ui,
                    new LadderClickTradingIssue(symbol, "Cannot find server to send order type " + orderType + " for symbol " + symbol));
        }
    }

    private void submitOrder(final ClientSpeedState clientSpeedState, final String orderType, final long price, final Side side,
            final String tag) {

        final int sequenceNumber = orderSeqNo++;

        trace.publish(new CommandTrace("submit", username, symbol, orderType, true, price, side.toString(), tag, clickTradingBoxQty,
                sequenceNumber));

        if (clientSpeedState == ClientSpeedState.TOO_SLOW) {
            final String message = "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", client " + username +
                    " is " + clientSpeedState;
            monitor.logError(ReddalComponents.LADDER_PRESENTER, message);
            ladderClickTradingIssuesPublisher.publish(new LadderClickTradingIssue(symbol, message));
        } else {

            final String serverName =
                    ladderOptions.serverResolver.resolveToServerName(symbol, orderType, tag, marketData.getBook().getMIC().name());

            final RemoteOrderType remoteOrderType = WorkingOrderUpdateFromServer.getRemoteOrderType(orderType);

            if (null == serverName) {
                final String message = "Cannot submit order " + orderType + ' ' + side + ' ' + clickTradingBoxQty + " for " + symbol +
                        ", no valid server found.";
                monitor.logError(ReddalComponents.LADDER_PRESENTER, message);
                ladderClickTradingIssuesPublisher.publish(new LadderClickTradingIssue(symbol, message));
            } else {

                final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus =
                        tradingStatusForAll.serverTradingStatusMap.get(serverName);

                if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
                    final String message =
                            "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", server " + serverName +
                                    " has status " + (serverTradingStatus == null ? null : serverTradingStatus.toString());
                    monitor.logError(ReddalComponents.LADDER_PRESENTER, message);
                    ladderClickTradingIssuesPublisher.publish(new LadderClickTradingIssue(symbol, message));
                } else {

                    final RemoteSubmitOrder remoteSubmitOrder = new RemoteSubmitOrder(serverName, username, sequenceNumber,
                            new RemoteOrder(symbol, side, price, clickTradingBoxQty, remoteOrderType, true, tag));

                    remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(serverName, remoteSubmitOrder));
                }
            }
        }
    }

    private void rightClickModify(final ClientSpeedState clientSpeedState, final Map<String, String> data) {

        if (null != workingOrdersForSymbol) {
            final long price = Long.valueOf(data.get("price"));
            if (null != modifyFromPrice) {
                if (modifyFromPrice != price) {

                    for (final WorkingOrderUpdateFromServer order : workingOrdersForSymbol.ordersByPrice.get(modifyFromPrice)) {
                        final WorkingOrderUpdate workingOrderUpdate = order.value;
                        modifyOrder(clientSpeedState, price, order, workingOrderUpdate, workingOrderUpdate.getTotalQuantity());
                    }
                }
                modifyFromPrice = null;
                modifyFromPriceSelectedTime = 0L;
            } else if (workingOrdersForSymbol.ordersByPrice.containsKey(price)) {
                modifyFromPrice = price;
                modifyFromPriceSelectedTime = System.currentTimeMillis();
            }
        }
    }

    private void clearModifyPriceIfTimedOut() {
        if (null != modifyFromPrice && modifyFromPriceSelectedTime + MODIFY_TIMEOUT_MILLI < System.currentTimeMillis()) {
            modifyFromPrice = null;
        }
    }

    private void modifyOrder(final ClientSpeedState clientSpeedState, final long price, final WorkingOrderUpdateFromServer order,
            final WorkingOrderUpdate workingOrderUpdate, final int totalQuantity) {

        trace.publish(new CommandTrace("modify", username, symbol, order.value.getWorkingOrderType().toString(), true, price,
                order.value.getSide().toString(), order.value.getTag(), clickTradingBoxQty, order.value.getChainId()));

        if (isTrader) {

            final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus =
                    tradingStatusForAll.serverTradingStatusMap.get(order.fromServer);
            if (clientSpeedState == ClientSpeedState.TOO_SLOW) {
                monitor.logError(ReddalComponents.LADDER_PRESENTER, "Cannot modify order , client " + username + " is " + clientSpeedState);
            } else if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
                monitor.logError(ReddalComponents.LADDER_PRESENTER, "Cannot modify order: server " + order.fromServer + " has status " +
                        (serverTradingStatus == null ? null : serverTradingStatus.toString()));
            } else {

                final RemoteModifyOrder remoteModifyOrder =
                        new RemoteModifyOrder(order.fromServer, username, workingOrderUpdate.getChainId(),
                                order.toRemoteOrder(workingOrderUpdate.getPrice(), workingOrderUpdate.getTotalQuantity()),
                                order.toRemoteOrder(price, totalQuantity));

                remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(order.fromServer, remoteModifyOrder));
            }

        }
    }

    private void cancelWorkingOrders(final Long price) {

        if (!pendingRefDataAndSettle && workingOrdersForSymbol != null) {
            for (final WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByPrice.get(price)) {
                cancelOrder(orderUpdateFromServer);
            }
        }
        if (orderUpdatesForSymbol != null) {
            for (final UpdateFromServer updateFromServer : orderUpdatesForSymbol.updatesByPrice.get(price).values()) {
                cancelManagedOrder(updateFromServer);
            }
        }
    }

    private void cancelManagedOrder(final UpdateFromServer updateFromServer) {

        final com.drwtrading.london.photons.eeifoe.RemoteOrder order = updateFromServer.update.getOrder();
        trace.publish(new CommandTrace("cancelManaged", username, symbol, "MANAGED", false, updateFromServer.update.getIndicativePrice(),
                order.getSide().name(), "?", clickTradingBoxQty, updateFromServer.update.getSystemOrderId()));
        if (isTrader) {
            eeifCommandToServer.publish(new OrderEntryCommandToServer(updateFromServer.server,
                    new Cancel(updateFromServer.update.getSystemOrderId(), updateFromServer.update.getOrder())));
        }
    }

    @Override
    public void cancelAllForSide(final BookSide side) {

        if (null != workingOrdersForSymbol) {
            for (final WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
                if (LadderView.convertSide(orderUpdateFromServer.value.getSide()) == side) {
                    cancelOrder(orderUpdateFromServer);
                }
            }
            orderUpdatesForSymbol.updatesByKey.values().stream().forEach(update -> {
                if (convertSide(update.update.getOrder().getSide()) == side) {
                    cancelManagedOrder(update);
                }
            });
        }
    }

    private static BookSide convertSide(final OrderSide side) {
        if (side == OrderSide.BUY) {
            return BookSide.BID;
        } else {
            return BookSide.ASK;
        }
    }

    private void cancelOrder(final WorkingOrderUpdateFromServer order) {
        trace.publish(
                new CommandTrace("cancel", username, symbol, order.value.getWorkingOrderType().toString(), false, order.value.getPrice(),
                        order.value.getSide().toString(), order.value.getTag(), clickTradingBoxQty, order.value.getChainId()));

        if (isTrader) {
            final Main.RemoteOrderCommandToServer cancel = order.buildCancelCommand(username);
            remoteOrderCommandToServerPublisher.publish(cancel);
        }
    }

    public void onSingleOrderCommand(final ClientSpeedState clientSpeedState, final OrdersPresenter.SingleOrderCommand singleOrderCommand) {

        final WorkingOrderUpdateFromServer orderUpdateFromServer = workingOrdersForSymbol.ordersByKey.get(singleOrderCommand.getOrderKey());
        if (orderUpdateFromServer == null) {

            monitor.logError(ReddalComponents.LADDER_PRESENTER, "Could not find order for command: " + singleOrderCommand);
        } else if (singleOrderCommand instanceof OrdersPresenter.CancelOrder) {
            cancelOrder(orderUpdateFromServer);
        } else if (singleOrderCommand instanceof OrdersPresenter.ModifyOrderQuantity) {
            final int totalQuantity = orderUpdateFromServer.value.getFilledQuantity() +
                    ((OrdersPresenter.ModifyOrderQuantity) singleOrderCommand).newRemainingQuantity;

            modifyOrder(clientSpeedState, orderUpdateFromServer.value.getPrice(), orderUpdateFromServer, orderUpdateFromServer.value,
                    totalQuantity);
        }
    }
}
