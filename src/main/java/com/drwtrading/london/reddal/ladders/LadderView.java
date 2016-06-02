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
import com.drwtrading.london.fastui.UiEventHandler;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.fastui.html.CSSClass;
import com.drwtrading.london.fastui.html.DataKey;
import com.drwtrading.london.fastui.html.HTML;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.Metadata;
import com.drwtrading.london.photons.eeifoe.OrderSide;
import com.drwtrading.london.photons.eeifoe.Submit;
import com.drwtrading.london.photons.reddal.CenterToPrice;
import com.drwtrading.london.photons.reddal.Command;
import com.drwtrading.london.photons.reddal.Direction;
import com.drwtrading.london.photons.reddal.ReddalCommand;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.photons.reddal.UpdateOffset;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteModifyOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.drwtrading.london.protocols.photon.execution.RemoteSubmitOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderType;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.SpreadContractSet;
import com.drwtrading.london.reddal.UserCycleRequest;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.IMarketData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.orderentry.ManagedOrderType;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderentry.UpdateFromServer;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.util.EnumSwitcher;
import com.drwtrading.london.reddal.util.FastUtilCollections;
import com.drwtrading.london.reddal.util.Mathematics;
import com.drwtrading.london.reddal.util.PriceUtils;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.websockets.WebSocketClient;
import drw.london.json.Jsonable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LadderView implements UiEventHandler {

    public static final EnumMap<WorkingOrderType, CSSClass> WORKING_ORDER_CSS;

    static {
        WORKING_ORDER_CSS = new EnumMap<>(WorkingOrderType.class);
        for (final WorkingOrderType workingOrderType : WorkingOrderType.values()) {
            final CSSClass cssClass = CSSClass.valueOf("WORKING_ORDER_TYPE_" + getOrderType(workingOrderType).toUpperCase());
            WORKING_ORDER_CSS.put(workingOrderType, cssClass);
        }
    }

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final DecimalFormat BASIS_POINT_DECIMAL_FORMAT = new DecimalFormat(".0");
    public static final DecimalFormat MILLIONS_QTY_FORMAT = new DecimalFormat(".0");
    public static final DecimalFormat POSITION_FMT = new DecimalFormat("0.0");
    public static final DecimalFormat EFP_DECIMAL_FORMAT = new DecimalFormat("0.00");
    public static final DecimalFormat FX_DECIMAL_FORMAT = new DecimalFormat(".0");
    public static final int MODIFY_TIMEOUT_MS = 5000;
    public static final int AUTO_RECENTER_TICKS = 3;
    public static final int RECENTER_TIME_MS = 11000;
    public static final int RECENTER_WARN_TIME_MS = 9000;
    public static final int REALLY_BIG_NUMBER_THRESHOLD = 100000;
    public static final Metadata LADDER_SOURCE_METADATA = new Metadata("SOURCE", "LADDER");
    /* Click-trading*/
    public final Map<String, Integer> buttonQty = new HashMap<>();

    public static final int PG_UP = 33;
    public static final int PG_DOWN = 34;
    public static final int END_KEY = 35;
    public static final int HOME_KEY = 36;

    public enum PricingMode {
        BPS,
        EFP,
        RAW
    }

    private final WebSocketClient client;
    private final View view;
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
    private final LongMap<Integer> levelByPrice;
    private final LongMap<String> formattedPrices;
    private final LadderHTMLKeys ladderHTMLKeys;
    private final NumberFormat bigNumberDF;

    public String symbol;
    private IMarketData marketData;
    private WorkingOrdersForSymbol workingOrdersForSymbol;
    private ExtraDataForSymbol dataForSymbol;
    private int levels;
    private LadderPrefsForSymbolUser ladderPrefsForSymbolUser;
    private OrderUpdatesForSymbol orderUpdatesForSymbol;
    private boolean pendingRefDataAndSettle = true;
    private boolean isCashEquityOrFX = false;
    private long centerPrice;
    private long topPrice;
    private long bottomPrice;
    public EnumSwitcher<PricingMode> pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.allOf(PricingMode.class));
    public long lastCenteredTime = 0;

    private Long modifyFromPrice = null;
    private Long modifyFromPriceSelectedTime = 0L;

    private Long lastHeartbeatSentMillis = null;
    private long lastHeartbeatRoundtripMillis = 0;
    private ClientSpeedState clientSpeedState = ClientSpeedState.FINE;

    public LadderView(final WebSocketClient client, final UiPipeImpl ui, final View view,
            final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher, final LadderOptions ladderOptions,
            final Publisher<StatsMsg> statsPublisher, final TradingStatusForAll tradingStatusForAll,
            final Publisher<HeartbeatRoundtrip> heartbeatRoundTripPublisher, final Publisher<ReddalMessage> commandPublisher,
            final Publisher<RecenterLaddersForUser> recenterLaddersForUser, final Publisher<Jsonable> trace,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher,
            final Publisher<UserCycleRequest> userCycleContractPublisher,
            final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap,
            final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher) {

        this.client = client;
        this.view = view;
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
        this.levelByPrice = new LongMap<>();
        this.formattedPrices = new LongMap<>();
        this.ladderHTMLKeys = new LadderHTMLKeys();
        this.bigNumberDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE + 'M', 0, 2);
        this.ui.setHandler(this);
        initDefaultPrefs();
    }

    public void replaceSymbol(final ReplaceCommand replaceCommand) {
        if (view != null) {
            System.out.println("Replacing: " + symbol + " -> " + symbol.replace(replaceCommand.from, replaceCommand.to));
            view.goToSymbol(symbol.replace(replaceCommand.from, replaceCommand.to));
        }
    }

    public void subscribeToSymbol(final String symbol, final int levels, final IMarketData marketData,
            final WorkingOrdersForSymbol workingOrdersForSymbol, final ExtraDataForSymbol extraDataForSymbol,
            final LadderPrefsForSymbolUser ladderPrefsForSymbolUser, final OrderUpdatesForSymbol orderUpdatesForSymbol) {

        this.symbol = symbol;
        this.marketData = marketData;
        this.workingOrdersForSymbol = workingOrdersForSymbol;
        this.dataForSymbol = extraDataForSymbol;
        this.levels = levels;
        this.ladderHTMLKeys.extendToLevels(levels);
        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;
        this.orderUpdatesForSymbol = orderUpdatesForSymbol;
        this.pendingRefDataAndSettle = true;

        tryToDrawLadder();
        flush();
        sendHeartbeat();
    }

    private void tryToDrawLadder() {
        final IMarketData m = this.marketData;
        if (null != m && null != m.getBook() && m.getBook().isValid()) {
            ui.clear();
            ui.clickable('#' + HTML.SYMBOL);
            ui.clickable('#' + HTML.CLOCK);
            if (pendingRefDataAndSettle) {
                onRefDataAndSettleFirstAppeared();
            }
            centerPrice = this.marketData.getPriceOperations().tradablePrice(centerPrice, Side.BID);
            pendingRefDataAndSettle = false;
            recenter();
            recenterLadderAndDrawPriceLevels();
            setUpBasketLink();
            setUpClickTrading();
        }
    }

    public void flush() {
        checkClientSpeed();
        drawLadderIfRefDataHasJustComeIn();
        recenterIfTimeoutElapsed();
        clearModifyPriceIfTimedOut();
        updateEverything();
        ui.flush();
    }

    private void updateEverything() {
        drawBook();
        drawTradedVolumes();
        drawWorkingOrders();
        drawMetaData();
        drawClock();
        drawClientSpeedState();
        drawPricingButtons();
        drawPriceLevels();
        drawClickTrading();
    }

    private void drawPricingButtons() {
        for (final PricingMode mode : pricingMode.getUniverse()) {
            ui.cls(HTML.PRICING + mode, CSSClass.INVISIBLE, !pricingMode.isValidChoice(mode));
            ui.cls(HTML.PRICING + mode, CSSClass.ACTIVE_MODE, pricingMode.get() == mode);
        }
    }

    public void fastInputFlush() {
        drawClickTrading();
        ui.flush();
    }

    private void drawLadderIfRefDataHasJustComeIn() {
        if (pendingRefDataAndSettle) {
            tryToDrawLadder();
        }
    }

    private void drawClock() {
        ui.txt(HTML.CLOCK, SIMPLE_DATE_FORMAT.format(new Date()));
    }

    public void clickTradingIssue(final LadderClickTradingIssue ladderClickTradingIssue) {
        ui.txt(HTML.CLICK_TRADING_ISSUES, ladderClickTradingIssue.issue);
    }

    private void drawClientSpeedState() {
        ui.cls(HTML.CLOCK, CSSClass.SLOW, clientSpeedState == ClientSpeedState.SLOW);
        ui.cls(HTML.CLOCK, CSSClass.VERY_SLOW, clientSpeedState == ClientSpeedState.TOO_SLOW);
    }

    private void drawPriceLevels() {
        final ExtraDataForSymbol d = dataForSymbol;
        if (!pendingRefDataAndSettle) {
            LaserLine theo = new LaserLine(symbol, ladderOptions.theoLaserLine, Long.MIN_VALUE, false, "");
            if (null != d && d.laserLineByName.containsKey(ladderOptions.theoLaserLine)) {
                theo = d.laserLineByName.get(ladderOptions.theoLaserLine);
            }
            for (final LongMapNode<?> priceNode : levelByPrice) {
                final long price = priceNode.key;
                if (pricingMode.get() == PricingMode.BPS && theo.isValid()) {
                    final double points = (10000.0 * (price - theo.getPrice())) / theo.getPrice();
                    ui.txt(priceKey(price), BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (pricingMode.get() == PricingMode.BPS && !theo.isValid() && isCashEquityOrFX && hasBestBid()) {
                    final long basePrice = marketData.getBook().getBestBid().getPrice();
                    final double points = (10000.0 * (price - basePrice)) / basePrice;
                    ui.txt(priceKey(price), BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (PricingMode.EFP == pricingMode.get() && null != marketData && theo.isValid()) {
                    final double efp = Math.round((price - theo.getPrice()) * 100d / Constants.NORMALISING_FACTOR) / 100d;
                    ui.txt(priceKey(price), EFP_DECIMAL_FORMAT.format(efp));
                } else if (PricingMode.EFP == pricingMode.get() && null != marketData && marketData.isPriceInverted()) {
                    final double invertedPrice = Constants.NORMALISING_FACTOR / (double) price;
                    ui.txt(priceKey(price), FX_DECIMAL_FORMAT.format(invertedPrice));
                } else {
                    final String formattedPrice = formattedPrices.get(price);
                    ui.txt(priceKey(price), formattedPrice);
                }
            }
        }
    }

    private boolean hasBestBid() {
        return null != marketData && null != marketData.getBook() && null != marketData.getBook().getBestBid();
    }

    private void decorateUpDown(final String key, final Long value) {
        if (null != value) {
            ui.cls(key, CSSClass.POSITIVE, 0 < value);
            ui.cls(key, CSSClass.NEGATIVE, value < 0);
        }
    }

    private static Long getLastTradeChangeOnDay(final IMarketData m) {

        final IBookReferencePrice refPriceData = m.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
        if (refPriceData.isValid() && m.getTradeTracker().hasTrade()) {
            return m.getTradeTracker().getLastPrice() - refPriceData.getPrice();
        } else {
            return null;
        }
    }

    private void drawBook() {
        final IMarketData m = this.marketData;
        if (!pendingRefDataAndSettle && null != m && null != m.getBook()) {
            switch (m.getBook().getStatus()) {
                case CONTINUOUS: {
                    ui.txt(HTML.SYMBOL, getSymbol());
                    ui.cls(HTML.BOOK_STATE, CSSClass.AUCTION, false);
                    ui.cls(HTML.BOOK_STATE, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case AUCTION: {
                    ui.txt(HTML.SYMBOL, getSymbol() + " - AUC");
                    ui.cls(HTML.BOOK_STATE, CSSClass.AUCTION, true);
                    ui.cls(HTML.BOOK_STATE, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case CLOSED: {
                    ui.txt(HTML.SYMBOL, getSymbol() + " - CLSD");
                    ui.cls(HTML.BOOK_STATE, CSSClass.AUCTION, true);
                    ui.cls(HTML.BOOK_STATE, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                default: {
                    ui.txt(HTML.SYMBOL, getSymbol() + " - ?");
                    ui.cls(HTML.BOOK_STATE, CSSClass.AUCTION, false);
                    ui.cls(HTML.BOOK_STATE, CSSClass.NO_BOOK_STATE, true);
                    break;
                }
            }
            ui.title(getSymbol());
            if (dataForSymbol != null && dataForSymbol.spreadContractSet != null) {
                final SpreadContractSet contracts = dataForSymbol.spreadContractSet;
                ui.cls(HTML.SYMBOL, CSSClass.SPREAD, symbol.equals(contracts.spread));
                ui.cls(HTML.SYMBOL, CSSClass.BACK, symbol.equals(contracts.back));
            }

            if (BookMarketState.AUCTION == m.getBook().getStatus()) {

                final IBookReferencePrice auctionIndicative = m.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
                final long auctionQty = auctionIndicative.isValid() ? auctionIndicative.getQty() : 0;

                for (final LongMapNode<?> priceNode : levelByPrice) {

                    final long price = priceNode.key;
                    if (price == auctionIndicative.getPrice()) {
                        bidQty(price, auctionQty);
                        askQty(price, auctionQty);
                    } else {
                        bidQty(price, 0);
                        askQty(price, 0);
                    }
                }
            } else {

                for (final LongMapNode<?> priceNode : levelByPrice) {

                    final long price = priceNode.key;
                    final IBookLevel bidLevel = m.getBook().getBidLevel(price);
                    final IBookLevel askLevel = m.getBook().getAskLevel(price);

                    if (null == bidLevel) {
                        bidQty(price, 0);
                        ui.cls(bidKey(price), CSSClass.IMPLIED_BID, false);
                    } else {
                        bidQty(price, bidLevel.getQty());
                        ui.cls(bidKey(price), CSSClass.IMPLIED_BID, 0 < bidLevel.getImpliedQty());
                    }

                    if (null == askLevel) {
                        askQty(price, 0);
                        ui.cls(offerKey(price), CSSClass.IMPLIED_ASK, false);
                    } else {
                        askQty(price, askLevel.getQty());
                        ui.cls(offerKey(price), CSSClass.IMPLIED_ASK, 0 < askLevel.getImpliedQty());
                    }
                }
            }
        }
    }

    private String getSymbol() {
        if (null != dataForSymbol && null != dataForSymbol.displaySymbol) {
            return dataForSymbol.displaySymbol;
        } else {
            return symbol;
        }
    }

    private void drawTradedVolumes() {

        final IMarketData m = this.marketData;
        if (!pendingRefDataAndSettle && null != m) {

            final TradeTracker tradeTracker = m.getTradeTracker();
            final long lastTradePrice = tradeTracker.getQtyRunAtLastPrice();

            for (final LongMapNode<?> priceNode : levelByPrice) {

                final long price = priceNode.key;

                final Long qty = tradeTracker.getTotalQtyTradedAtPrice(price);
                if (null != qty) {
                    ui.txt(volumeKey(price), formatMktQty(qty));
                } else {
                    ui.txt(volumeKey(price), HTML.EMPTY);
                }

                final boolean withinTradedRange =
                        tradeTracker.getMinTradedPrice() <= price && price <= tradeTracker.getMaxTradedPrice();
                ui.cls(priceKey(price), CSSClass.PRICE_TRADED, withinTradedRange);

                if (tradeTracker.hasTrade() && price == tradeTracker.getLastPrice()) {
                    ui.txt(tradeKey(price), formatMktQty(lastTradePrice));
                    ui.cls(tradeKey(price), CSSClass.INVISIBLE, false);
                    ui.cls(volumeKey(price), CSSClass.INVISIBLE, true);
                    ui.cls(tradeKey(price), CSSClass.TRADED_UP, tradeTracker.isLastTickUp());
                    ui.cls(tradeKey(price), CSSClass.TRADED_DOWN, tradeTracker.isLastTickDown());
                    ui.cls(tradeKey(price), CSSClass.TRADED_AGAIN, tradeTracker.isLastTradeSameLevel());
                } else {
                    ui.txt(tradeKey(price), HTML.BLANK);
                    ui.cls(tradeKey(price), CSSClass.INVISIBLE, true);
                    ui.cls(volumeKey(price), CSSClass.INVISIBLE, false);
                    ui.cls(tradeKey(price), CSSClass.TRADED_UP, false);
                    ui.cls(tradeKey(price), CSSClass.TRADED_DOWN, false);
                    ui.cls(tradeKey(price), CSSClass.TRADED_AGAIN, false);
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

    private void drawWorkingOrders() {

        final WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && null != w && null != orderUpdatesForSymbol) {

            final StringBuilder keys = new StringBuilder();
            final StringBuilder eeifKeys = new StringBuilder();
            final Set<WorkingOrderType> orderTypes = EnumSet.noneOf(WorkingOrderType.class);

            for (final LongMapNode<?> priceNode : levelByPrice) {

                final long price = priceNode.key;

                keys.setLength(0);
                eeifKeys.setLength(0);
                orderTypes.clear();

                int managedOrderQty = 0;
                int hiddenTickTakerQty = 0;
                int totalQty = 0;
                BookSide side = null;

                for (final Main.WorkingOrderUpdateFromServer orderFromServer : w.ordersByPrice.get(price)) {
                    final WorkingOrderUpdate order = orderFromServer.value;
                    final int orderQty = order.getTotalQuantity() - order.getFilledQuantity();
                    side = convertSide(order.getSide());
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

                for (final UpdateFromServer update : orderUpdatesForSymbol.updatesByPrice.get(price).values()) {
                    managedOrderQty += update.update.getRemainingQty();
                    side = convertSide(update.update.getOrder().getSide());
                    keys.append(update.key());
                    keys.append('!');
                    eeifKeys.append(update.key());
                    eeifKeys.append('!');
                }
                totalQty += Math.max(managedOrderQty, hiddenTickTakerQty);
                workingQty(price, totalQty, side, orderTypes, 0 < managedOrderQty);

                if (0 < keys.length()) {
                    keys.setLength(keys.length() - 1);
                }
                if (0 < eeifKeys.length()) {
                    eeifKeys.setLength(eeifKeys.length() - 1);
                }
                ui.data(orderKey(price), DataKey.ORDER, keys);
                ui.data(orderKey(price), DataKey.EEIF, eeifKeys);
            }
            int buyQty = 0;
            int sellQty = 0;
            int buyHiddenTTQty = 0;
            int sellHiddenTTQty = 0;
            int buyManagedQty = 0;
            int sellManagedQty = 0;
            for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : w.ordersByKey.values())
                if (orderUpdateFromServer.value.getWorkingOrderState() != WorkingOrderState.DEAD) {
                    final int remainingQty =
                            orderUpdateFromServer.value.getTotalQuantity() - orderUpdateFromServer.value.getFilledQuantity();
                    if (orderUpdateFromServer.value.getSide() == com.drwtrading.london.protocols.photon.execution.Side.BID) {
                        if (orderUpdateFromServer.value.getWorkingOrderType() == WorkingOrderType.HIDDEN_TICKTAKER) {
                            buyHiddenTTQty += remainingQty;
                        } else {
                            buyQty += remainingQty;
                        }
                    } else if (orderUpdateFromServer.value.getSide() == com.drwtrading.london.protocols.photon.execution.Side.OFFER) {
                        if (orderUpdateFromServer.value.getWorkingOrderType() == WorkingOrderType.HIDDEN_TICKTAKER) {
                            sellHiddenTTQty += remainingQty;
                        } else {
                            sellQty += remainingQty;
                        }
                    }
                }
            for (final UpdateFromServer updateFromServer : orderUpdatesForSymbol.updatesByKey.values())
                if (updateFromServer.update.getOrder().getSide() == OrderSide.BUY) {
                    buyManagedQty += updateFromServer.update.getRemainingQty();
                } else if (updateFromServer.update.getOrder().getSide() == OrderSide.SELL) {
                    sellManagedQty += updateFromServer.update.getRemainingQty();
                }
            buyQty += Math.max(buyHiddenTTQty, buyManagedQty);
            sellQty += Math.max(sellHiddenTTQty, sellManagedQty);
            ui.cls(HTML.BUY_QTY, CSSClass.INVISIBLE, buyQty == 0);
            ui.txt(HTML.BUY_QTY, buyQty);
            ui.cls(HTML.SELL_QTY, CSSClass.INVISIBLE, sellQty == 0);
            ui.txt(HTML.SELL_QTY, sellQty);
        }
    }

    private void workingQty(final long price, final int qty, final BookSide side, final Set<WorkingOrderType> orderTypes,
            final boolean hasEeifOEOrder) {

        ui.txt(orderKey(price), formatMktQty(qty));
        ui.cls(orderKey(price), CSSClass.WORKING_QTY, 0 < qty);
        ui.cls(orderKey(price), CSSClass.WORKING_BID, BookSide.BID == side);
        ui.cls(orderKey(price), CSSClass.WORKING_OFFER, BookSide.ASK == side);
        for (final WorkingOrderType workingOrderType : WorkingOrderType.values()) {
            final CSSClass cssClass = WORKING_ORDER_CSS.get(workingOrderType);
            ui.cls(orderKey(price), cssClass, !hasEeifOEOrder && orderTypes.contains(workingOrderType));
        }
        ui.cls(orderKey(price), CSSClass.EEIF_ORDER_TYPE, hasEeifOEOrder);
    }

    private void drawMetaData() {

        final ExtraDataForSymbol d = dataForSymbol;
        final IMarketData m = marketData;
        if (!pendingRefDataAndSettle && null != d && null != m) {
            drawLaserLines(d, m);
            /* Desk position*/
            if (null != d.deskPosition && null != d.deskPosition.getPosition() && !d.deskPosition.getPosition().isEmpty()) {
                try {
                    final BigDecimal decimal = new BigDecimal(d.deskPosition.getPosition());
                    ui.txt(HTML.DESK_POSITION, formatPosition(decimal.doubleValue()));
                    decorateUpDown(HTML.DESK_POSITION, decimal.longValue());
                } catch (final NumberFormatException ignored) {
                    /* exception.printStackTrace();*/
                }
            }
            /* Day position*/
            if (d.dayPosition != null) {
                ui.txt(HTML.POSITION, formatPosition(d.dayPosition.getNet()));
                decorateUpDown(HTML.POSITION, d.dayPosition.getNet());
            }
            /* Change on day*/
            final Long lastTradeChangeOnDay = getLastTradeChangeOnDay(m);
            if (lastTradeChangeOnDay != null) {
                final String formatPrice = m.formatPrice(lastTradeChangeOnDay);
                ui.txt(HTML.LAST_TRADE_COD, formatPrice);
            }
            decorateUpDown(HTML.LAST_TRADE_COD, lastTradeChangeOnDay);
            /* Ladder info*/
            if (dataForSymbol.infoOnLadder != null) {
                ui.txt(HTML.TEXT + "info", dataForSymbol.infoOnLadder.getValue());
            }
            /* Ladder text*/
            for (final LadderText ladderText : dataForSymbol.ladderTextByPosition.values()) {
                ui.txt(HTML.TEXT + ladderText.getCell(), ladderText.getText()); /* Last trade*/
            }
            for (final LongMapNode<Integer> entry : levelByPrice) {
                final String priceKey = priceKey(entry.key);
                ui.cls(priceKey, CSSClass.LAST_BID, d.lastBuy != null && d.lastBuy.getPrice() == entry.key);
                ui.cls(priceKey, CSSClass.LAST_ASK, d.lastSell != null && d.lastSell.getPrice() == entry.key);
            }
        }
    }

    private void drawLaserLines(final ExtraDataForSymbol d, final IMarketData m) { /* Display laserlines*/
        for (final LaserLine laserLine : d.laserLineByName.values()) {
            final String laserKey = laserKey(laserLine.getId());
            ui.cls(laserKey, CSSClass.INVISIBLE, true);
            if (laserLine.isValid()) {
                long price = bottomPrice;
                ui.cls(laserKey, CSSClass.INVISIBLE, false);
                if (laserLine.getPrice() > topPrice) {
                    ui.height(laserKey, priceKey(topPrice), 0.5);
                } else if (laserLine.getPrice() < bottomPrice) {
                    ui.height(laserKey, priceKey(bottomPrice), -0.5);
                } else {
                    while (price <= topPrice) {
                        final long priceAbove = m.getPriceOperations().nTicksAway(price, 1, PriceUtils.Direction.Add);
                        if (price <= laserLine.getPrice() && laserLine.getPrice() <= priceAbove && levelByPrice.containsKey(price)) {
                            final long fractionalPrice = laserLine.getPrice() - price;
                            final double tickFraction = 1.0 * fractionalPrice / (priceAbove - price);
                            ui.height(laserKey, priceKey(price), tickFraction);
                            break;
                        }
                        price = priceAbove;
                    }
                }
            }
        }
    }

    private void setUpBasketLink() {
        if (null != marketData.getBook()) {
            for (final LongMapNode<Integer> levelNode : levelByPrice) {
                ui.clickable('#' + HTML.VOLUME + levelNode.getValue());
            }
        }
    }

    static final double DEFAULT_EQUITY_NOTIONAL_EUR = 100000.0;

    private void onRefDataAndSettleFirstAppeared() {

        switch (marketData.getBook().getInstType()) {
            case EQUITY:
            case ETF:
            case DR: {
                isCashEquityOrFX = true;
                pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.of(PricingMode.BPS, PricingMode.RAW));
                pricingMode.set(PricingMode.BPS);
                final IBookReferencePrice closePrice = marketData.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);
                if (closePrice.isValid()) {
                    int qty = (int) Mathematics.toQuantityFromNotionalInSafetyCurrency(DEFAULT_EQUITY_NOTIONAL_EUR, closePrice.getPrice(),
                            marketData.getBook(), marketData.getBook().getWPV());
                    qty = Math.max(50, qty);
                    defaultPrefs.put(HTML.INP_RELOAD, Integer.toString(qty));
                }
                break;
            }
            case FX: {
                isCashEquityOrFX = true;
                pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.of(PricingMode.BPS, PricingMode.RAW));
                pricingMode.set(PricingMode.BPS);
                defaultPrefs.put(HTML.INP_RELOAD, "1000000");
                break;
            }
            default: {
                pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.of(PricingMode.EFP, PricingMode.RAW));
                pricingMode.set(PricingMode.EFP);
                break;
            }
        }
    }

    private void recenter() {
        if (centerPrice == 0) {
            moveLadderToCenter();
        } else {
            moveLadderTowardCenter();
        }
        resetLastCenteredTime();
    }

    private void setUpClickTrading() {
        final boolean clickTradingEnabled = ladderOptions.traders.contains(client.getUserName());
        setupButtons();
        view.trading(clickTradingEnabled, filterUsableOrderTypes(ladderOptions.orderTypesLeft),
                filterUsableOrderTypes(ladderOptions.orderTypesRight));
        for (final Map.Entry<String, Integer> entry : buttonQty.entrySet()) {
            final String display = formatClickQty(entry.getValue());
            ui.txt(entry.getKey(), display);
        }
        for (int i = 0; i < levels; i++) {
            ui.clickable('#' + HTML.PRICE + i);
            ui.clickable('#' + HTML.BID + i);
            ui.clickable('#' + HTML.OFFER + i);
            ui.clickable('#' + HTML.ORDER + i);
        }
        ui.clickable(HTML.BUTTONS);
        ui.scrollable('#' + HTML.LADDER);
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

    private static String formatClickQty(final Integer qty) {
        final String display;
        if (qty < 1000) {
            display = Integer.toString(qty);
        } else if (qty < 100000) {
            display = qty / 1000 + "K";
        } else {
            display = MILLIONS_QTY_FORMAT.format(qty / 1000000d) + 'M';
        }
        return display;
    }

    private Collection<String> filterUsableOrderTypes(final Collection<CSSClass> types) {
        return types.stream().filter(input -> {
            boolean oldOrderType = ladderOptions.serverResolver.resolveToServerName(symbol, input.name()) != null;
            boolean newOrderType = orderEntryMap.containsKey(symbol) && managedOrderTypes.contains(input.name()) &&
                    orderEntryMap.get(symbol).supportedTypes.contains(ManagedOrderType.valueOf(input.name()));
            return oldOrderType || newOrderType;
        }).map(Enum::name).collect(Collectors.toList());
    }

    private void setupButtons() {

        switch (marketData.getBook().getInstType()) {
            case EQUITY:
            case DR:
            case ETF: {
                buttonQty.put("btn_qty_1", 1);
                buttonQty.put("btn_qty_2", 10);
                buttonQty.put("btn_qty_3", 100);
                buttonQty.put("btn_qty_4", 1000);
                buttonQty.put("btn_qty_5", 5000);
                buttonQty.put("btn_qty_6", 10000);
                break;
            }
            case FX: {
                buttonQty.put("btn_qty_1", 100000);
                buttonQty.put("btn_qty_2", 200000);
                buttonQty.put("btn_qty_3", 500000);
                buttonQty.put("btn_qty_4", 1000000);
                buttonQty.put("btn_qty_5", 2500000);
                buttonQty.put("btn_qty_6", 5000000);
                break;
            }
            default: {
                buttonQty.put("btn_qty_1", 1);
                buttonQty.put("btn_qty_2", 5);
                buttonQty.put("btn_qty_3", 10);
                buttonQty.put("btn_qty_4", 25);
                buttonQty.put("btn_qty_5", 50);
                buttonQty.put("btn_qty_6", 100);
                break;
            }
        }
    }

    public void recenterIfTimeoutElapsed() {
        final IMarketData m = marketData;
        if (!pendingRefDataAndSettle && m != null) {
            if (bottomPrice <= getCenterPrice(m) && getCenterPrice(m) <= topPrice) {
                resetLastCenteredTime();
            } else if (System.currentTimeMillis() - lastCenteredTime > RECENTER_TIME_MS) {
                recenter();
                recenterLadderAndDrawPriceLevels();
            }
            ui.cls(HTML.LADDER, CSSClass.RECENTERING,
                    lastCenteredTime > 0 && RECENTER_WARN_TIME_MS <= System.currentTimeMillis() - lastCenteredTime);
        }
    }

    private void moveLadderTowardCenter() {
        final int direction = (int) Math.signum(getCenterPrice(marketData) - centerPrice);
        centerPrice = getPriceNTicksFrom(centerPrice, AUTO_RECENTER_TICKS * direction);
    }

    private void resetLastCenteredTime() {
        lastCenteredTime = System.currentTimeMillis();
    }

    private long getCenterPrice(final IMarketData md) {

        if (!pendingRefDataAndSettle) {

            long center = 0;

            final IBook<?> book = md.getBook();
            final IBookLevel bestBid = book.getBestBid();
            final IBookLevel bestAsk = book.getBestAsk();
            final boolean isBookNotAuction = book.getStatus() != BookMarketState.AUCTION;
            final IBookReferencePrice auctionIndicativePrice = md.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
            final IBookReferencePrice auctionSummaryPrice = md.getBook().getRefPriceData(ReferencePoint.AUCTION_SUMMARY);
            final IBookReferencePrice yestClose = md.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);

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
                for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
                    avgPrice += (orderUpdateFromServer.value.getPrice() / n);
                }
                center = avgPrice;
            } else if (auctionSummaryPrice.isValid()) {
                center = auctionSummaryPrice.getPrice();
            } else if (md.getTradeTracker().hasTrade()) {
                center = md.getTradeTracker().getLastPrice();
            } else if (yestClose.isValid()) {
                center = yestClose.getPrice();
            }

            return md.getPriceOperations().tradablePrice(center, Side.BID);
        }
        return 0;
    }

    private void recenterLadderAndDrawPriceLevels() {

        final IMarketData m = this.marketData;
        if (null != m && null != m.getBook()) {

            final int centerLevel = levels / 2;
            topPrice = getPriceNTicksFrom(centerPrice, centerLevel);
            levelByPrice.clear();
            formattedPrices.clear();

            long price = topPrice;
            for (int i = 0; i < levels; ++i) {

                levelByPrice.put(price, i);
                formattedPrices.put(price, m.formatPrice(price));

                bidQty(price, 0);
                askQty(price, 0);
                ui.txt(volumeKey(price), HTML.EMPTY);
                ui.txt(orderKey(price), HTML.EMPTY);

                ui.data(bidKey(price), DataKey.PRICE, price);
                ui.data(offerKey(price), DataKey.PRICE, price);
                ui.data(orderKey(price), DataKey.PRICE, price);

                bottomPrice = price;
                price = getPriceNTicksFrom(price, -1);
            }
        }
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
                clickTradingBoxQty = Integer.valueOf(value);
            } else if (persistentPrefs.contains(label)) {
                ladderPrefsForSymbolUser.set(label, value);
            } else {
                throw new IllegalArgumentException("Update for unknown value: " + label + ' ' + dataArg);
            }
        }
        flush();
    }

    @Override
    public void onScroll(final String direction) {
        if ("up".equals(direction)) {
            centerPrice = marketData.getPriceOperations().nTicksAway(centerPrice, 1, PriceUtils.Direction.Add);
        } else if ("down".equals(direction)) {
            centerPrice = marketData.getPriceOperations().nTicksAway(centerPrice, 1, PriceUtils.Direction.Subtract);
        } else {
            return;
        }
        resetLastCenteredTime();
        recenterLadderAndDrawPriceLevels();
        flush();
    }

    @Override
    public void onKeyDown(final int keyCode) {

        if (keyCode == PG_UP) {
            final int n = levelByPrice.size() - 1;
            centerPrice = getPriceNTicksFrom(centerPrice, n);
        } else if (keyCode == PG_DOWN) {
            final int n = -1 * (levelByPrice.size() - 1);
            centerPrice = getPriceNTicksFrom(centerPrice, n);
        } else if (keyCode == HOME_KEY) {

            if (null != marketData.getBook()) {
                final IBookLevel bestAsk = marketData.getBook().getBestAsk();
                if (null != bestAsk) {
                    centerPrice = bestAsk.getPrice();
                }
            }
        } else if (keyCode == END_KEY) {
            if (null != marketData.getBook()) {
                final IBookLevel bestBid = marketData.getBook().getBestBid();
                if (null != bestBid) {
                    centerPrice = bestBid.getPrice();
                }
            }
        } else {
            return;
        }
        resetLastCenteredTime();
        recenterLadderAndDrawPriceLevels();
        flush();
    }

    private long getPriceNTicksFrom(final long price, final int n) {

        final PriceUtils.Direction direction = n < 0 ? PriceUtils.Direction.Subtract : PriceUtils.Direction.Add;
        return marketData.getPriceOperations().nTicksAway(price, Math.abs(n), direction);
    }

    @Override
    public void onHeartbeat(final long sentTimeMillis) {

        final long returnTimeMillis = System.currentTimeMillis();
        if (lastHeartbeatSentMillis == sentTimeMillis) {
            lastHeartbeatSentMillis = null;
            lastHeartbeatRoundtripMillis = returnTimeMillis - sentTimeMillis;
            heartbeatRoundTripPublisher.publish(
                    new HeartbeatRoundtrip(client.getUserName(), symbol, sentTimeMillis, returnTimeMillis, lastHeartbeatRoundtripMillis));
        } else {
            throw new RuntimeException(
                    "Received heartbeat reply " + sentTimeMillis + " which does not match last sent heartbeat " + lastHeartbeatSentMillis);
        }
    }

    @Override
    public void onDblClick(final String label, final Map<String, String> dataArg) {

        if (null != workingOrdersForSymbol) {
            if (HTML.BUY_QTY.equals(label)) {
                cancelAllForSide(BookSide.BID);
            } else if (HTML.SELL_QTY.equals(label)) {
                cancelAllForSide(BookSide.ASK);
            }
        }
        flush();
    }

    @Override
    public void onClick(final String label, final Map<String, String> data) {
        final String button = data.get("button");
        final LadderPrefsForSymbolUser l = ladderPrefsForSymbolUser;
        final boolean autoHedge = shouldAutoHedge();
        if ("left".equals(button)) {
            if (buttonQty.containsKey(label)) {
                clickTradingBoxQty += buttonQty.get(label);
            } else if (HTML.BUTTON_CLR.equals(label)) {
                clickTradingBoxQty = 0;
            } else if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (l != null) {
                    submitOrderClick(label, data, getPref(l, HTML.ORDER_TYPE_LEFT), autoHedge, ladderClickTradingIssuePublisher);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                final long price = Long.valueOf(data.get("price"));
                if (label.equals(orderKey(price))) {
                    cancelWorkingOrders(price);
                } else {
                    System.out.println("Mismatched label: " + data.get("price") + ' ' + orderKey(price) + ' ' + label);
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
                pricingMode.set(PricingMode.BPS);
            } else if (label.equals(HTML.PRICING_RAW)) {
                pricingMode.set(PricingMode.RAW);
            } else if (label.equals(HTML.PRICING_EFP)) {
                pricingMode.set(PricingMode.EFP);
            } else if (label.startsWith(DataKey.PRICE.key)) {
                pricingMode.next();
            } else if (label.startsWith(HTML.SYMBOL)) {
                if (nextContract()) {
                    return;
                }
            } else if (label.startsWith(HTML.VOLUME)) {
                view.launchBasket(symbol);
            } else if (label.equals(HTML.CLOCK)) {
                if (switchChixSymbol()) {
                    return;
                }
            }
        } else if ("right".equals(button)) {
            if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (l != null) {
                    submitOrderClick(label, data, getPref(l, HTML.ORDER_TYPE_RIGHT), "true".equals(getPref(l, HTML.AUTO_HEDGE_RIGHT)),
                            ladderClickTradingIssuePublisher);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                rightClickModify(data, autoHedge);
            } else if (label.startsWith(HTML.SYMBOL) && null != ladderOptions.basketUrl) {
                view.goToUrl(ladderOptions.basketUrl + '#' + symbol);
            }
        } else if ("middle".equals(button)) {
            if (label.startsWith(HTML.PRICE)) {
                recenterLaddersForUser.publish(new RecenterLaddersForUser(client.getUserName()));
            } else if (label.startsWith(HTML.ORDER)) {
                final String price = data.get("price");
                final String url = String.format("/orders#%s,%s", symbol, price);
                final Collection<Main.WorkingOrderUpdateFromServer> orders = workingOrdersForSymbol.ordersByPrice.get(Long.valueOf(price));
                if (!orders.isEmpty()) {
                    view.popUp(url, "orders", 270, 20 * (1 + orders.size()));
                }
            } else if (label.startsWith(HTML.SYMBOL)) {
                final UserCycleRequest cycleRequest = new UserCycleRequest(client.getUserName());
                userCycleContractPublisher.publish(cycleRequest);
                return;
            }
        }
        flush();
    }

    boolean nextContract() {
        if (dataForSymbol != null && dataForSymbol.spreadContractSet != null) {
            final SpreadContractSet contracts = dataForSymbol.spreadContractSet;
            final String nextContract = contracts.next(symbol);
            if (null != nextContract && !symbol.equals(nextContract)) {
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
        if (null != dataForSymbol && null != dataForSymbol.chixSwitchSymbol) {
            view.goToSymbol(dataForSymbol.chixSwitchSymbol);
            return true;
        } else {
            return false;
        }
    }

    public void recenterLadderForUser(final CenterToPrice centerToPrice) {
        if (client.getUserName().equals(centerToPrice.getUsername()) && symbol.equals(centerToPrice.getSymbol())) {
            setCenterPrice(centerToPrice.getPrice());
        }
    }

    public void setCenterPrice(final long price) {
        centerPrice = price;
        resetLastCenteredTime();
        tryToDrawLadder();
        flush();
    }

    public void recenterLadderForUser(final RecenterLaddersForUser recenterLaddersForUser) {
        if (client.getUserName().equals(recenterLaddersForUser.user)) {
            moveLadderToCenter();
            resetLastCenteredTime();
            recenterLadderAndDrawPriceLevels();
            flush();
        }
    }

    private void moveLadderToCenter() {
        centerPrice = getCenterPrice(marketData);
    }

    public static final AtomicLong heartbeatSeqNo = new AtomicLong(0L);

    public static class HeartbeatRoundtrip extends Struct {

        public final String userName;
        public final String symbol;
        public final long sentTimeMillis;
        public final long returnTimeMillis;
        public final long roundtripMillis;

        public HeartbeatRoundtrip(final String userName, final String symbol, final long sentTimeMillis, final long returnTimeMillis,
                final long roundtripMillis) {

            this.userName = userName;
            this.symbol = symbol;
            this.sentTimeMillis = sentTimeMillis;
            this.returnTimeMillis = returnTimeMillis;
            this.roundtripMillis = roundtripMillis;
        }
    }

    // Click-trading

    private int clickTradingBoxQty = 0;
    private int orderSeqNo = 0;

    public static final Set<String> persistentPrefs = FastUtilCollections.newFastSet();

    static {
        persistentPrefs.add(HTML.WORKING_ORDER_TAG);
        persistentPrefs.add(HTML.INP_RELOAD);
        persistentPrefs.add(HTML.AUTO_HEDGE_LEFT);
        persistentPrefs.add(HTML.AUTO_HEDGE_RIGHT);
        persistentPrefs.add(HTML.ORDER_TYPE_LEFT);
        persistentPrefs.add(HTML.ORDER_TYPE_RIGHT);
        persistentPrefs.add(HTML.RANDOM_RELOAD);
    }

    public final Map<String, String> defaultPrefs = FastUtilCollections.newFastMap();

    private void initDefaultPrefs() {
        defaultPrefs.put(HTML.WORKING_ORDER_TAG, "CHAD");
        defaultPrefs.put(HTML.INP_RELOAD, "50");
        defaultPrefs.put(HTML.AUTO_HEDGE_LEFT, "true");
        defaultPrefs.put(HTML.AUTO_HEDGE_RIGHT, "true");
        defaultPrefs.put(HTML.ORDER_TYPE_LEFT, "HAWK");
        defaultPrefs.put(HTML.ORDER_TYPE_RIGHT, "MANUAL");
        defaultPrefs.put(HTML.RANDOM_RELOAD, "true");
    }

    public void drawClickTrading() {
        final LadderPrefsForSymbolUser l = ladderPrefsForSymbolUser;
        if (!pendingRefDataAndSettle && l != null) {

            ui.txt(HTML.INP_QTY, clickTradingBoxQty);

            for (final String pref : persistentPrefs) {
                ui.txt(pref, getPref(ladderPrefsForSymbolUser, pref));
            }

            final String leftOrderPricePref = getPref(l, HTML.ORDER_TYPE_LEFT);
            for (final CSSClass type : ladderOptions.orderTypesLeft) {
                ui.cls(HTML.ORDER_TYPE_LEFT, type, type.name().equals(leftOrderPricePref));
            }

            final String rightOrderPricePref = getPref(l, HTML.ORDER_TYPE_RIGHT);
            for (final CSSClass type : ladderOptions.orderTypesRight) {
                ui.cls(HTML.ORDER_TYPE_RIGHT, type, type.name().equals(rightOrderPricePref));
            }

            for (final LongMapNode<?> priceNode : levelByPrice) {
                final long price = priceNode.key;
                ui.cls(orderKey(price), CSSClass.MODIFY_PRICE_SELECTED, null != modifyFromPrice && price == modifyFromPrice);
            }

        }

        if (dataForSymbol != null) {
            ui.cls(HTML.OFFSET_CONTROL, CSSClass.INVISIBLE, !dataForSymbol.symbolAvailable);
        }
    }

    private String getPref(final LadderPrefsForSymbolUser l, final String id) {
        return l.get(id, defaultPrefs.get(id));
    }

    private void cancelAllForSide(final BookSide side) {
        for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
            if (convertSide(orderUpdateFromServer.value.getSide()) == side) {
                cancelOrder(orderUpdateFromServer);
            }
        }
        orderUpdatesForSymbol.updatesByKey.values().stream().forEach(update -> {
            if (convertSide(update.update.getOrder().getSide()) == side) {
                cancelManagedOrder(update);
            }
        });
    }

    private static BookSide convertSide(final OrderSide s1) {
        if (s1 == OrderSide.BUY) {
            return BookSide.BID;
        } else {
            return BookSide.ASK;
        }
    }

    private static BookSide convertSide(final com.drwtrading.london.protocols.photon.execution.Side s1) {
        if (s1 == com.drwtrading.london.protocols.photon.execution.Side.BID) {
            return BookSide.BID;
        } else {
            return BookSide.ASK;
        }
    }

    private void rightClickModify(final Map<String, String> data, final boolean autoHedge) {
        if (workingOrdersForSymbol != null) {
            final long price = Long.valueOf(data.get("price"));
            if (modifyFromPrice != null) {
                if (workingOrdersForSymbol != null && modifyFromPrice != price) {
                    for (final Main.WorkingOrderUpdateFromServer order : workingOrdersForSymbol.ordersByPrice.get(modifyFromPrice)) {
                        final WorkingOrderUpdate workingOrderUpdate = order.value;
                        modifyOrder(autoHedge, price, order, workingOrderUpdate, workingOrderUpdate.getTotalQuantity());
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
        if (modifyFromPrice != null && modifyFromPriceSelectedTime != null &&
                System.currentTimeMillis() > modifyFromPriceSelectedTime + MODIFY_TIMEOUT_MS) {
            modifyFromPrice = null;
        }
    }

    public static RemoteOrder getRemoteOrderFromWorkingOrder(final boolean autoHedge, final long price,
            final WorkingOrderUpdate workingOrderUpdate, final int totalQuantity) {
        final RemoteOrderType remoteOrderType = getRemoteOrderType(workingOrderUpdate.getWorkingOrderType().toString());
        return new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), price, totalQuantity, remoteOrderType,
                autoHedge, workingOrderUpdate.getTag());
    }

    private final Set<String> managedOrderTypes =
            Arrays.asList(ManagedOrderType.values()).stream().map(Enum::toString).collect(Collectors.toSet());
    private final Set<String> oldOrderTypes =
            Arrays.asList(RemoteOrderType.values()).stream().map(Enum::toString).collect(Collectors.toSet());

    private void submitOrderClick(final String label, final Map<String, String> data, final String orderType, final boolean autoHedge,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssuesPublisher) {

        if (ladderOptions.traders.contains(client.getUserName())) {

            final long price = Long.valueOf(data.get("price"));

            final com.drwtrading.london.protocols.photon.execution.Side side =
                    label.equals(bidKey(price)) ? com.drwtrading.london.protocols.photon.execution.Side.BID :
                            label.equals(offerKey(price)) ? com.drwtrading.london.protocols.photon.execution.Side.OFFER : null;

            final String tag = ladderPrefsForSymbolUser.get(HTML.WORKING_ORDER_TAG);

            if (side == null) {
                throw new IllegalArgumentException("Price " + price + " did not match key " + label);
            } else if (null == tag) {
                throw new IllegalArgumentException("No tag provided.");
            }

            if (orderType != null && clickTradingBoxQty > 0) {
                if (managedOrderTypes.contains(orderType)) {
                    submitManagedOrder(orderType, autoHedge, price, side, tag);
                } else if (oldOrderTypes.contains(orderType)) {
                    submitOrder(orderType, autoHedge, price, side, tag, ladderClickTradingIssuesPublisher);
                } else {
                    clickTradingIssue(new LadderClickTradingIssue(symbol, "Unknown order type: " + orderType));
                    return;
                }
            }

            final boolean randomReload = "true".equals(getPref(ladderPrefsForSymbolUser, HTML.RANDOM_RELOAD));
            final int reloadBoxQty = Integer.valueOf(getPref(ladderPrefsForSymbolUser, HTML.INP_RELOAD));

            if (randomReload) {
                clickTradingBoxQty = Math.max(0, reloadBoxQty - (int) (Math.random() * ladderOptions.randomReloadFraction * reloadBoxQty));
            } else {
                clickTradingBoxQty = Math.max(0, reloadBoxQty);
            }

        }
    }

    private void submitManagedOrder(final String orderType, final boolean autoHedge, final long price,
            final com.drwtrading.london.protocols.photon.execution.Side side, final String tag) {
        int tradingBoxQty = this.clickTradingBoxQty;
        trace.publish(new CommandTrace("submitManaged", client.getUserName(), symbol, orderType, autoHedge, price, side.toString(), tag,
                tradingBoxQty, orderSeqNo++));
        final OrderEntryClient.SymbolOrderChannel symbolOrderChannel = orderEntryMap.get(symbol);
        if (null != symbolOrderChannel) {
            final ManagedOrderType managedOrderType = ManagedOrderType.valueOf(orderType);
            if (!symbolOrderChannel.supportedTypes.contains(managedOrderType)) {
                clickTradingIssue(
                        new LadderClickTradingIssue(symbol, "Does not support order type " + orderType + " for symbol " + symbol));
                return;
            }
            tradingBoxQty = managedOrderType.getQty(tradingBoxQty);
            if (tradingBoxQty == 0) {
                tradingBoxQty = clickTradingBoxQty;
            }
            final com.drwtrading.london.photons.eeifoe.RemoteOrder remoteOrder =
                    new com.drwtrading.london.photons.eeifoe.RemoteOrder(symbol,
                            side == com.drwtrading.london.protocols.photon.execution.Side.BID ? OrderSide.BUY : OrderSide.SELL, price,
                            tradingBoxQty, client.getUserName(), managedOrderType.getOrder(price, tradingBoxQty),
                            new ObjectArrayList<>(Arrays.asList(LADDER_SOURCE_METADATA, new Metadata("TAG", tag))));
            final Submit submit = new Submit(remoteOrder);
            symbolOrderChannel.publisher.publish(submit);
        } else {
            clickTradingIssue(
                    new LadderClickTradingIssue(symbol, "Cannot find server to send order type " + orderType + " for symbol " + symbol));
        }
    }

    public static class InboundDataTrace extends Struct {

        public final String remote;
        public final String user;
        public final String[] inbound;
        public final Map<String, String> dataArg;

        public InboundDataTrace(final String remote, final String user, final String[] inbound, final Map<String, String> dataArg) {
            this.remote = remote;
            this.user = user;
            this.inbound = inbound;
            this.dataArg = dataArg;
        }
    }

    public static class CommandTrace extends Struct {

        public final String command;
        public final String user;
        public final String symbol;
        public final String orderType;
        public final boolean autoHedge;
        public final long price;
        public final String side;
        public final String tag;
        public final int quantity;
        public final int chainId;

        public CommandTrace(final String command, final String user, final String symbol, final String orderType, final boolean autoHedge,
                final long price, final String side, final String tag, final int quantity, final int chainId) {
            this.command = command;
            this.user = user;
            this.symbol = symbol;
            this.orderType = orderType;
            this.autoHedge = autoHedge;
            this.price = price;
            this.side = side;
            this.tag = tag;
            this.quantity = quantity;
            this.chainId = chainId;
        }
    }

    private void submitOrder(final String orderType, final boolean autoHedge, final long price,
            final com.drwtrading.london.protocols.photon.execution.Side side, final String tag,
            final Publisher<LadderClickTradingIssue> ladderClickTradingIssues) {

        final int sequenceNumber = orderSeqNo++;

        trace.publish(new CommandTrace("submit", client.getUserName(), symbol, orderType, autoHedge, price, side.toString(), tag,
                clickTradingBoxQty, sequenceNumber));

        if (clientSpeedState == ClientSpeedState.TOO_SLOW) {
            final String message =
                    "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", client " + client.getUserName() +
                            " is " + clientSpeedState + " speed: " + getClientSpeedMillis() + "ms";
            statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, message));
            ladderClickTradingIssues.publish(new LadderClickTradingIssue(symbol, message));
            return;
        }

        final String serverName = ladderOptions.serverResolver.resolveToServerName(symbol, orderType);

        final RemoteOrderType remoteOrderType = getRemoteOrderType(orderType);

        if (serverName == null) {
            final String message = "Cannot submit order " + orderType + ' ' + side + ' ' + clickTradingBoxQty + " for " + symbol +
                    ", no valid server found.";
            statsPublisher.publish(new AdvisoryStat("Click-Trading", AdvisoryStat.Level.WARNING, message));
            ladderClickTradingIssues.publish(new LadderClickTradingIssue(symbol, message));
        } else {

            System.out.println("Order: " + symbol + ' ' + remoteOrderType.toString() + " resolved to " + serverName);

            final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus =
                    tradingStatusForAll.serverTradingStatusMap.get(serverName);

            if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
                final String message =
                        "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", server " + serverName +
                                " has status " + (serverTradingStatus == null ? null : serverTradingStatus.toString());
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, message));
                ladderClickTradingIssues.publish(new LadderClickTradingIssue(symbol, message));
            } else {

                final RemoteSubmitOrder remoteSubmitOrder = new RemoteSubmitOrder(serverName, client.getUserName(), sequenceNumber,
                        new RemoteOrder(symbol, side, price, clickTradingBoxQty, remoteOrderType, autoHedge, tag));
                remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(serverName, remoteSubmitOrder));
            }
        }
    }

    private void modifyOrder(final boolean autoHedge, final long price, final Main.WorkingOrderUpdateFromServer order,
            final WorkingOrderUpdate workingOrderUpdate, final int totalQuantity) {
        final RemoteModifyOrder remoteModifyOrder =
                new RemoteModifyOrder(order.fromServer, client.getUserName(), workingOrderUpdate.getChainId(),
                        getRemoteOrderFromWorkingOrder(autoHedge, workingOrderUpdate.getPrice(), workingOrderUpdate,
                                workingOrderUpdate.getTotalQuantity()),
                        getRemoteOrderFromWorkingOrder(autoHedge, price, workingOrderUpdate, totalQuantity));

        trace.publish(
                new CommandTrace("modify", client.getUserName(), symbol, order.value.getWorkingOrderType().toString(), autoHedge, price,
                        order.value.getSide().toString(), order.value.getTag(), clickTradingBoxQty, order.value.getChainId()));

        if (ladderOptions.traders.contains(client.getUserName())) {

            final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus =
                    tradingStatusForAll.serverTradingStatusMap.get(order.fromServer);
            if (clientSpeedState == ClientSpeedState.TOO_SLOW) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING,
                        "Cannot modify order , client " + client.getUserName() + " is " + clientSpeedState + " speed: " +
                                getClientSpeedMillis() + "ms"));
            } else if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING,
                        "Cannot modify order: server " + order.fromServer + " has status " +
                                (serverTradingStatus == null ? null : serverTradingStatus.toString())));
            } else {
                remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(order.fromServer, remoteModifyOrder));
            }

        }
    }

    private void cancelWorkingOrders(final Long price) {
        final WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null) {
            for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : w.ordersByPrice.get(price)) {
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
        trace.publish(new CommandTrace("cancelManaged", client.getUserName(), symbol, "MANAGED", false,
                updateFromServer.update.getIndicativePrice(), order.getSide().name(), "?", clickTradingBoxQty,
                updateFromServer.update.getSystemOrderId()));
        if (ladderOptions.traders.contains(client.getUserName())) {
            eeifCommandToServer.publish(new OrderEntryCommandToServer(updateFromServer.server,
                    new Cancel(updateFromServer.update.getSystemOrderId(), updateFromServer.update.getOrder())));
        }
    }

    private void cancelOrder(final Main.WorkingOrderUpdateFromServer order) {
        trace.publish(new CommandTrace("cancel", client.getUserName(), symbol, order.value.getWorkingOrderType().toString(), false,
                order.value.getPrice(), order.value.getSide().toString(), order.value.getTag(), clickTradingBoxQty,
                order.value.getChainId()));

        if (ladderOptions.traders.contains(client.getUserName())) {
            final WorkingOrderUpdate workingOrderUpdate = order.value;
            final String orderType = getOrderType(workingOrderUpdate.getWorkingOrderType());
            final RemoteOrder remoteOrder =
                    new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), workingOrderUpdate.getPrice(),
                            workingOrderUpdate.getTotalQuantity(), getRemoteOrderType(orderType), false, workingOrderUpdate.getTag());
            remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(order.fromServer,
                    new RemoteCancelOrder(workingOrderUpdate.getServerName(), client.getUserName(), workingOrderUpdate.getChainId(),
                            remoteOrder)));
        }
    }

    public static String getOrderType(final WorkingOrderType workingOrderType) {
        if (workingOrderType == WorkingOrderType.MARKET) {
            return "MKT_CLOSE";
        }
        return workingOrderType.name();
    }

    public static RemoteOrderType getRemoteOrderType(final String orderType) {
        for (final RemoteOrderType remoteOrderType : RemoteOrderType.values()) {
            if (remoteOrderType.toString().toUpperCase().equals(orderType.toUpperCase())) {
                return remoteOrderType;
            }
        }
        return RemoteOrderType.MANUAL;
    }

    public void onSingleOrderCommand(final OrdersPresenter.SingleOrderCommand singleOrderCommand) {

        final Main.WorkingOrderUpdateFromServer orderUpdateFromServer =
                workingOrdersForSymbol.ordersByKey.get(singleOrderCommand.getOrderKey());
        if (orderUpdateFromServer == null) {
            statsPublisher.publish(
                    new AdvisoryStat("Reddal", AdvisoryStat.Level.WARNING, "Could not find order for command: " + singleOrderCommand));
        } else if (singleOrderCommand instanceof OrdersPresenter.CancelOrder) {
            cancelOrder(orderUpdateFromServer);
        } else if (singleOrderCommand instanceof OrdersPresenter.ModifyOrderQuantity) {
            final int totalQuantity = orderUpdateFromServer.value.getFilledQuantity() +
                    ((OrdersPresenter.ModifyOrderQuantity) singleOrderCommand).newRemainingQuantity;
            modifyOrder(shouldAutoHedge(), orderUpdateFromServer.value.getPrice(), orderUpdateFromServer, orderUpdateFromServer.value,
                    totalQuantity);
        }
    }

    private boolean shouldAutoHedge() {
        return null != ladderPrefsForSymbolUser && "true".equals(getPref(ladderPrefsForSymbolUser, HTML.AUTO_HEDGE_LEFT));
    }

    // Heartbeats

    public static enum ClientSpeedState {
        TOO_SLOW(10000),
        SLOW(5000),
        FINE(0);

        public final int thresholdMillis;

        ClientSpeedState(final int thresholdMillis) {
            this.thresholdMillis = thresholdMillis;
        }
    }

    public void sendHeartbeat() {
        if (lastHeartbeatSentMillis == null) {
            lastHeartbeatSentMillis = System.currentTimeMillis();
            ui.send(UiPipeImpl.cmd("heartbeat", lastHeartbeatSentMillis, heartbeatSeqNo.getAndIncrement()));
        }
    }

    public long getClientSpeedMillis() {
        return Math.max(null == lastHeartbeatSentMillis ? 0L : System.currentTimeMillis() - lastHeartbeatSentMillis,
                lastHeartbeatRoundtripMillis);
    }

    public void checkClientSpeed() {

        final long clientSpeed = getClientSpeedMillis();
        if (ClientSpeedState.TOO_SLOW.thresholdMillis < clientSpeed) {
            clientSpeedState = ClientSpeedState.TOO_SLOW;
        } else if (ClientSpeedState.SLOW.thresholdMillis < clientSpeed) {
            clientSpeedState = ClientSpeedState.SLOW;
        } else {
            clientSpeedState = ClientSpeedState.FINE;
        }
    }

    // Update helpers

    public void bidQty(final long price, final long qty) {
        ui.txt(bidKey(price), formatMktQty(qty));
        ui.cls(bidKey(price), CSSClass.BID_ACTIVE, 0 < qty);
    }

    public void askQty(final long price, final long qty) {
        ui.txt(offerKey(price), formatMktQty(qty));
        ui.cls(offerKey(price), CSSClass.ASK_ACTIVE, 0 < qty);
    }

    public String formatMktQty(final long qty) {
        if (qty <= 0) {
            return HTML.EMPTY;
        } else if (REALLY_BIG_NUMBER_THRESHOLD <= qty) {
            final double d = qty / 1000000d;
            return bigNumberDF.format(d);
        } else {
            return Long.toString(qty);
        }
    }

    private static String laserKey(final String name) {
        return HTML.LASER + name;
    }

    private String bidKey(final long price) {
        final int level = levelByPrice.get(price);
        return ladderHTMLKeys.getBidKey(level);
    }

    private String offerKey(final long price) {
        final int level = levelByPrice.get(price);
        return ladderHTMLKeys.getOfferKey(level);
    }

    private String priceKey(final long price) {
        final int level = levelByPrice.get(price);
        return ladderHTMLKeys.getPriceKey(level);
    }

    private String orderKey(final long price) {
        final int level = levelByPrice.get(price);
        return ladderHTMLKeys.getOrderKey(level);
    }

    private String volumeKey(final long price) {
        final int level = levelByPrice.get(price);
        return ladderHTMLKeys.getVolumeKey(level);
    }

    private String tradeKey(final long price) {
        final int level = levelByPrice.get(price);
        return ladderHTMLKeys.getTradeKey(level);
    }
}
