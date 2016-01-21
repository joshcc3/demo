package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.collections.SlidingWindow;
import com.drwtrading.london.fastui.UiPipe;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommand;
import com.drwtrading.london.photons.eeifoe.OrderSide;
import com.drwtrading.london.photons.eeifoe.Submit;
import com.drwtrading.london.photons.reddal.CenterToPrice;
import com.drwtrading.london.photons.reddal.Command;
import com.drwtrading.london.photons.reddal.Direction;
import com.drwtrading.london.photons.reddal.ReddalCommand;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.photons.reddal.UpdateOffset;
import com.drwtrading.london.prices.NormalizedPrice;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteModifyOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.drwtrading.london.protocols.photon.execution.RemoteSubmitOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderType;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BestPrice;
import com.drwtrading.london.protocols.photon.marketdata.BookState;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.ForexPairStructure;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.orderentry.ManagedOrderType;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderentry.UpdateFromServer;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.util.EnumSwitcher;
import com.drwtrading.london.reddal.util.Mathematics;
import com.drwtrading.london.reddal.util.PriceUtils;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.websockets.WebSocketClient;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import drw.london.json.Jsonable;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;
import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class LadderView implements UiPipe.UiEventHandler {

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
    public static final int BIG_NUMBER_THRESHOLD = 99999;
    public static final int REALLY_BIG_NUMBER_THRESHOLD = 100000;
    public static final String BID_LOWERCASE = "bid";
    public static final String OFFER_LOWERCASE = "offer";
    // Click-trading
    public final Map<String, Integer> buttonQty = new HashMap<>();

    public static class Html {

        public static final String EMPTY = " ";

        // Key prefixes
        public static final String BID = "bid_";
        public static final String OFFER = "offer_";
        public static final String PRICE = "price_";
        public static final String ORDER = "order_";
        public static final String TRADE = "trade_";
        public static final String VOLUME = "volume_";
        public static final String TEXT = "text_";

        // Classes
        public static final String PRICE_TRADED = "price_traded";
        public static final String BID_ACTIVE = "bid_active";
        public static final String OFFER_ACTIVE = "offer_active";
        public static final String WORKING_QTY = "working_qty";
        public static final String PRICE_KEY = "price";
        public static final String LASER = "laser_";
        public static final String HIDDEN = "invisible";
        public static final String TRADED_UP = "traded_up";
        public static final String TRADED_DOWN = "traded_down";
        public static final String TRADED_AGAIN = "traded_again";
        public static final String POSITIVE = "positive";
        public static final String NEGATIVE = "negative";
        public static final String LAST_BUY = "last_buy";
        public static final String LAST_SELL = "last_sell";
        public static final String BLANK = " ";

        public static final String BUTTON_CLR = "btn_clear";
        public static final String INP_RELOAD = "inp_reload";
        public static final String WORKING_ORDER_TAG = "working_order_tags";
        public static final String INP_QTY = "inp_qty";

        public static final String ORDER_TYPE_LEFT = "order_type_left";
        public static final String ORDER_TYPE_RIGHT = "order_type_right";
        public static final String AUTO_HEDGE_LEFT = "chk_auto_hedge_left";
        public static final String AUTO_HEDGE_RIGHT = "chk_auto_hedge_right";
        public static final String WORKING_BID = "working_bid";
        public static final String WORKING_OFFER = "working_offer";
        public static final String BUTTONS = "button";
        public static final String RANDOM_RELOAD = "chk_random_reload";
        public static final String WORKING_ORDER_TYPE = "working_order_type_";
        public static final String MODIFY_PRICE_SELECTED = "modify_price_selected";

        public static final String EEIF_ORDER_TYPE = "eeif_order_type_managed";


        // Divs
        public static final String CLICK_TRADING_ISSUES = "click_trading_issues";
        public static final String DESK_POSITION = "desk_position";
        public static final String TOTAL_TRADED_VOLUME = "total_traded_volume";
        public static final String LAST_TRADE_COD = "last_trade_cod";
        public static final String BUY_QTY = "buy_qty";
        public static final String SELL_QTY = "sell_qty";
        public static final String CLOCK = "clock";
        public static final String POSITION = "position";
        public static final String LADDER = "ladder";
        public static final String RECENTERING = "recentering";
        public static final String SYMBOL = "symbol";

        public static final String BUY_OFFSET_UP = "buy_offset_up";
        public static final String BUY_OFFSET_DOWN = "buy_offset_down";
        public static final String SELL_OFFSET_UP = "sell_offset_up";
        public static final String SELL_OFFSET_DOWN = "sell_offset_down";
        public static final String OFFSET_CONTROL = "offset_control";
        public static final String START_BUY = "start_buy";
        public static final String STOP_BUY = "stop_buy";
        public static final String START_SELL = "start_sell";
        public static final String STOP_SELL = "stop_sell";

        public static final String PRICING_BPS = "pricing_BPS";
        public static final String PRICING_RAW = "pricing_RAW";
        public static final String PRICING_EFP = "pricing_EFP";

        public static final String PRICING = "pricing_";
        public static final String BIG_NUMBER = "big_number";
        public static final String BOOK_STATE = "symbol";
    }

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
    private final LadderPresenter.View view;
    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher;
    private final LadderOptions ladderOptions;
    private final Publisher<ReddalMessage> commandPublisher;
    private final Publisher<LadderPresenter.RecenterLaddersForUser> recenterLaddersForUser;
    private final Publisher<Jsonable> trace;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher;
    private final Map<String, Publisher<OrderEntryCommand>> orderEntryMap;
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer;
    private final UiPipeImpl ui;
    private final Publisher<StatsMsg> statsPublisher;
    private final TradingStatusForAll tradingStatusForAll;
    private final Publisher<HeartbeatRoundtrip> heartbeatRoundtripPublisher;
    private final Publisher<UserCycleRequest> userCycleContractPublisher;
    private final SlidingWindow flushWindow = new SlidingWindow(10, 100);
    public String symbol;
    private MarketDataForSymbol marketDataForSymbol;
    private WorkingOrdersForSymbol workingOrdersForSymbol;
    private ExtraDataForSymbol dataForSymbol;
    private int levels;
    private LadderPrefsForSymbolUser ladderPrefsForSymbolUser;
    private OrderUpdatesForSymbol orderUpdatesForSymbol;

    Map<Long, Integer> levelByPrice = new HashMap<>();

    private boolean pendingRefDataAndSettle = true;
    private boolean isCashEquityOrFX = false;

    private long centerPrice;
    private long topPrice;
    private long bottomPrice;

    public EnumSwitcher<PricingMode> pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.allOf(PricingMode.class));
    public long lastCenteredTime = 0;

    public LadderView(final WebSocketClient client, final UiPipe ui, final LadderPresenter.View view,
                      final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher, final LadderOptions ladderOptions,
                      final Publisher<StatsMsg> statsPublisher, final TradingStatusForAll tradingStatusForAll,
                      final Publisher<HeartbeatRoundtrip> heartbeatRoundtripPublisher, final Publisher<ReddalMessage> commandPublisher,
                      final Publisher<LadderPresenter.RecenterLaddersForUser> recenterLaddersForUser, final Publisher<Jsonable> trace,
                      final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher, final Publisher<UserCycleRequest> userCycleContractPublisher,
                      final Map<String, Publisher<OrderEntryCommand>> orderEntryMap, final Publisher<OrderEntryCommandToServer> orderEntryCommandToServerPublisher) {
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
        this.ui = (UiPipeImpl) ui;
        this.statsPublisher = statsPublisher;
        this.tradingStatusForAll = tradingStatusForAll;
        this.heartbeatRoundtripPublisher = heartbeatRoundtripPublisher;
        this.userCycleContractPublisher = userCycleContractPublisher;
        this.ui.setHandler(this);
        initDefaultPrefs();
    }

    public void replaceSymbol(final ReplaceCommand replaceCommand) {
        if (view != null) {
            System.out.println("Replacing: " + symbol + " -> " + symbol.replace(replaceCommand.from, replaceCommand.to));
            view.goToSymbol(symbol.replace(replaceCommand.from, replaceCommand.to));
        }
    }

    public void subscribeToSymbol(final String symbol, final int levels, final MarketDataForSymbol marketDataForSymbol,
                                  final WorkingOrdersForSymbol workingOrdersForSymbol, final ExtraDataForSymbol extraDataForSymbol,
                                  final LadderPrefsForSymbolUser ladderPrefsForSymbolUser, final OrderUpdatesForSymbol orderUpdatesForSymbol) {
        this.symbol = symbol;
        this.marketDataForSymbol = marketDataForSymbol;
        this.workingOrdersForSymbol = workingOrdersForSymbol;
        this.dataForSymbol = extraDataForSymbol;
        this.levels = levels;
        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;
        this.orderUpdatesForSymbol = orderUpdatesForSymbol;
        this.pendingRefDataAndSettle = true;
        tryToDrawLadder();
        updateEverything();
        flush();
        sendHeartbeat();
    }

    private void updateEverything() {
        drawBook();
        drawTradedVolumes();
        drawLastTrade();
        drawWorkingOrders();
        drawMetaData();
        drawClock();
        drawClientSpeedState();
        drawPricingButtons();
        drawPriceLevels(dataForSymbol);
        drawClickTrading();
    }

    private void drawPricingButtons() {
        for (final PricingMode mode : pricingMode.getUniverse()) {
            ui.cls(Html.PRICING + mode, "invisible", !pricingMode.isValidChoice(mode));
            ui.cls(Html.PRICING + mode, "active_mode", pricingMode.get() == mode);
        }
    }

    public void fastMdFlush() {
    }

    public void fastInputFlush() {
        drawClickTrading();
        ui.flush();
    }

    public void flush() {
        checkClientSpeed();
        drawLadderIfRefDataHasJustComeIn();
        recenterIfTimeoutElapsed();
        clearModifyPriceIfTimedOut();
        updateEverything();
        ui.flush();
    }


    private void drawLadderIfRefDataHasJustComeIn() {
        if (pendingRefDataAndSettle) {
            tryToDrawLadder();
        }
    }

    private void drawClock() {
        ui.txt(Html.CLOCK, SIMPLE_DATE_FORMAT.format(new Date()));
    }

    // Drawing
    public void clickTradingIssue(final LadderClickTradingIssue ladderClickTradingIssue) {
        ui.txt(Html.CLICK_TRADING_ISSUES, ladderClickTradingIssue.issue);

    }

    private void drawClientSpeedState() {
        ui.cls(Html.CLOCK, "slow", clientSpeedState == ClientSpeedState.Slow);
        ui.cls(Html.CLOCK, "very-slow", clientSpeedState == ClientSpeedState.TooSlow);
    }

    private void drawMetaData() {
        final ExtraDataForSymbol d = dataForSymbol;
        final MarketDataForSymbol m = marketDataForSymbol;
        if (!pendingRefDataAndSettle && d != null && m != null) {
            drawLaserLines(d, m);

            // Desk position
            if (d.deskPosition != null && d.deskPosition.getPosition() != null && !"".equals(d.deskPosition.getPosition())) {
                try {
                    final BigDecimal decimal = new BigDecimal(d.deskPosition.getPosition());
                    ui.txt(Html.DESK_POSITION, formatPosition(decimal.doubleValue()));
                    decorateUpDown(Html.DESK_POSITION, decimal.longValue());
                } catch (final NumberFormatException exception) {
//                    exception.printStackTrace();
                }
            }

            // Day position
            if (d.dayPosition != null) {
                ui.txt(Html.POSITION, formatPosition(d.dayPosition.getNet()));
                decorateUpDown(Html.POSITION, d.dayPosition.getNet());
            }

            // Change on day
            final Long lastTradeChangeOnDay = getLastTradeChangeOnDay(m);
            if (lastTradeChangeOnDay != null) {
                drawPrice(m, lastTradeChangeOnDay, Html.LAST_TRADE_COD);
            }
            decorateUpDown(Html.LAST_TRADE_COD, lastTradeChangeOnDay);

            // Update for laserlines
            drawPriceLevels(dataForSymbol);

            // Ladder info
            if (dataForSymbol.infoOnLadder != null) {
                ui.txt(Html.TEXT + "info", dataForSymbol.infoOnLadder.getValue());
            }

            // Ladder text
            for (final LadderText ladderText : dataForSymbol.ladderTextByPosition.values()) {
                ui.txt(Html.TEXT + ladderText.getCell(), ladderText.getText());
            }

            // Last trade
            for (final Map.Entry<Long, Integer> entry : levelByPrice.entrySet()) {
                ui.cls(priceKey(entry.getKey()), Html.LAST_BUY, d.lastBuy != null && d.lastBuy.getPrice() == entry.getKey());
                ui.cls(priceKey(entry.getKey()), Html.LAST_SELL, d.lastSell != null && d.lastSell.getPrice() == entry.getKey());
            }
        }
    }

    private void drawLaserLines(final ExtraDataForSymbol d, final MarketDataForSymbol m) {
        // Display laserlines
        for (final LaserLine laserLine : d.laserLineByName.values()) {
            final String laserKey = laserKey(laserLine.getId());
            ui.cls(laserKey, Html.HIDDEN, true);
            if (laserLine.isValid()) {
                long price = bottomPrice;
                ui.cls(laserKey, Html.HIDDEN, false);
                if (laserLine.getPrice() > topPrice) {
                    ui.height(laserKey, priceKey(topPrice), 0.5);
                } else if (laserLine.getPrice() < bottomPrice) {
                    ui.height(laserKey, priceKey(bottomPrice), -0.5);
                } else {
                    while (price <= topPrice) {
                        final long priceAbove = m.priceOperations.nTicksAway(price, 1, PriceUtils.Direction.Add);
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

    static final ImmutableSet<String> invertedMarkets = ImmutableSet.of("6R");

    private void drawPriceLevels(final ExtraDataForSymbol d) {
        if (!pendingRefDataAndSettle) {
            LaserLine theo = new LaserLine(symbol, ladderOptions.theoLaserLine, Long.MIN_VALUE, false, "");
            if (d != null && d.laserLineByName.containsKey(ladderOptions.theoLaserLine)) {
                theo = d.laserLineByName.get(ladderOptions.theoLaserLine);
            }

            for (final Long price : levelByPrice.keySet()) {
                if (pricingMode.get() == PricingMode.BPS && theo.isValid()) {
                    final double points = (10000.0 * (price - theo.getPrice())) / theo.getPrice();
                    ui.txt(priceKey(price), BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (pricingMode.get() == PricingMode.BPS && !theo.isValid() && isCashEquityOrFX && hasBestBid()) {
                    final long basePrice = marketDataForSymbol.topOfBook.getBestBid().getPrice();
                    final double points = (10000.0 * (price - basePrice)) / basePrice;
                    ui.txt(priceKey(price), BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (pricingMode.get() == PricingMode.EFP && marketDataForSymbol != null && marketDataForSymbol.priceFormat != null &&
                        theo.isValid()) {
                    final double efp = marketDataForSymbol.priceFormat.toBigDecimal(new NormalizedPrice(price - theo.getPrice())).setScale(2,
                            RoundingMode.HALF_EVEN).doubleValue();
                    ui.txt(priceKey(price), EFP_DECIMAL_FORMAT.format(efp));
                } else if (pricingMode.get() == PricingMode.EFP && marketDataForSymbol != null &&
                        invertedMarkets.contains(marketDataForSymbol.refData.getMarket())) {
                    final double invertedPrice = 1.0 / marketDataForSymbol.priceFormat.toBigDecimal(new NormalizedPrice(price)).setScale(8,
                            RoundingMode.HALF_EVEN).doubleValue();
                    ui.txt(priceKey(price), FX_DECIMAL_FORMAT.format(invertedPrice));
                } else {
                    drawPrice(marketDataForSymbol, price, priceKey(price));
                }
            }

        }
    }

    private boolean hasBestBid() {
        return marketDataForSymbol != null && marketDataForSymbol.topOfBook != null &&
                marketDataForSymbol.topOfBook.getBestBid().isExists();
    }

    private void decorateUpDown(final String key, final Long value) {
        if (value == null)
            return;
        ui.cls(key, Html.POSITIVE, value > 0);
        ui.cls(key, Html.NEGATIVE, value < 0);
    }

    private static Long getLastTradeChangeOnDay(final MarketDataForSymbol m) {
        if (m.lastTrade == null) {
            return null;
        }
        if (m.settle == null) {
            return null;
        }
        final long lastTradePrice = m.lastTrade.getPrice();
        final long settlementPrice = m.settle.getSettlementPrice();
        return lastTradePrice - settlementPrice;
    }


    private void drawWorkingOrders() {
        final WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null && orderUpdatesForSymbol != null) {


            for (final Long price : levelByPrice.keySet()) {
                int totalQty = 0;
                int hiddenTickTakerQty = 0;
                String side = "";
                final List<String> keys = new ArrayList<>();
                final Set<WorkingOrderType> orderTypes = new HashSet<>();
                for (final Main.WorkingOrderUpdateFromServer orderFromServer : w.ordersByPrice.get(price)) {
                    final WorkingOrderUpdate order = orderFromServer.value;
                    final int orderQty = order.getTotalQuantity() - order.getFilledQuantity();
                    side = order.getSide().toString().toLowerCase();
                    keys.add(orderFromServer.key());
                    if (orderQty > 0) {
                        orderTypes.add(orderFromServer.value.getWorkingOrderType());
                    }
                    if (order.getWorkingOrderType() == WorkingOrderType.HIDDEN_TICKTAKER) {
                        hiddenTickTakerQty += orderQty;
                    } else {
                        totalQty += orderQty;
                    }
                }


                final List<String> eeifKeys = new ArrayList<>();


                int managedOrderQty = 0;
                for (final UpdateFromServer update : orderUpdatesForSymbol.updatesByPrice.get(price).values()) {
                    managedOrderQty += update.update.getRemainingQty();
                    side = update.update.getOrder().getSide() == OrderSide.BUY ? BID_LOWERCASE : OFFER_LOWERCASE;
                    keys.add(update.key());
                    eeifKeys.add(update.key());
                }

                totalQty += Math.max(managedOrderQty, hiddenTickTakerQty);

                workingQty(price, totalQty, side, orderTypes, managedOrderQty > 0);
                ui.data(orderKey(price), "orderKeys", Joiner.on('!').join(keys));
                ui.data(orderKey(price), "eeifKeys", Joiner.on('!').join(eeifKeys));
            }

            int buyQty = 0;
            int sellQty = 0;

            int buyHiddenTTQty = 0;
            int sellHiddenTTQty = 0;

            int buyManagedQty = 0;
            int sellManagedQty = 0;

            for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : w.ordersByKey.values()) {
                if (orderUpdateFromServer.value.getWorkingOrderState() != WorkingOrderState.DEAD) {
                    final int remainingQty = orderUpdateFromServer.value.getTotalQuantity() - orderUpdateFromServer.value.getFilledQuantity();
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

            ui.cls(Html.BUY_QTY, Html.HIDDEN, buyQty == 0);
            ui.txt(Html.BUY_QTY, buyQty);

            ui.cls(Html.SELL_QTY, Html.HIDDEN, sellQty == 0);
            ui.txt(Html.SELL_QTY, sellQty);
        }
    }

    private void drawLastTrade() {
        final MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.tradeTracker != null && m.tradeTracker.quantityTraded != 0) {
            for (final Long price : levelByPrice.keySet()) {
                if (price != m.tradeTracker.lastTrade.getPrice()) {
                    ui.txt(tradeKey(price), Html.BLANK);
                    ui.cls(tradeKey(price), Html.HIDDEN, true);
                    ui.cls(volumeKey(price), Html.HIDDEN, false);
                    ui.cls(tradeKey(price), Html.TRADED_UP, false);
                    ui.cls(tradeKey(price), Html.TRADED_DOWN, false);
                    ui.cls(tradeKey(price), Html.TRADED_AGAIN, false);
                } else {
                    ui.txt(tradeKey(price), formatMktQty(m.tradeTracker.quantityTraded));
                    ui.cls(tradeKey(price), Html.HIDDEN, false);
                    ui.cls(volumeKey(price), Html.HIDDEN, true);
                    ui.cls(tradeKey(price), Html.TRADED_UP, m.tradeTracker.lastTradedUp);
                    ui.cls(tradeKey(price), Html.TRADED_DOWN, m.tradeTracker.lastTradedDown);
                    ui.cls(tradeKey(price), Html.TRADED_AGAIN, m.tradeTracker.tradedOnSameLevel);
                }
            }
        }
    }

    private void drawTradedVolumes() {
        final MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.totalTradedVolumeByPrice != null) {

            final Long minTradedPrice = m.totalTradedVolumeByPrice.isEmpty() ? null : Collections.min(m.totalTradedVolumeByPrice.keySet());
            final Long maxTradedPrice = m.totalTradedVolumeByPrice.isEmpty() ? null : Collections.max(m.totalTradedVolumeByPrice.keySet());

            for (final Long price : levelByPrice.keySet()) {
                final TotalTradedVolumeByPrice volumeByPrice = m.totalTradedVolumeByPrice.get(price);
                if (volumeByPrice != null) {
                    ui.txt(volumeKey(price), formatMktQty(volumeByPrice.getQuantityTraded()));
                    ui.cls(priceKey(price), Html.PRICE_TRADED, volumeByPrice.getQuantityTraded() > 0);
                } else {
                    ui.txt(volumeKey(price), Html.EMPTY);
                    ui.cls(priceKey(price), Html.PRICE_TRADED, false);
                }
            }

            ui.txt(Html.VOLUME + '0', marketDataForSymbol.refData.getPriceStructure().getCurrency().toString());
            ui.cls(Html.VOLUME + '0', "currency", true);

            for (final Long price : levelByPrice.keySet()) {
                final boolean withinTradedRange =
                        minTradedPrice != null && maxTradedPrice != null && price >= minTradedPrice && price <= maxTradedPrice;
                ui.cls(priceKey(price), Html.PRICE_TRADED, withinTradedRange);
            }

            int quantityTraded = 0;

            for (final TotalTradedVolumeByPrice totalTradedVolumeByPrice : m.totalTradedVolumeByPrice.values()) {
                quantityTraded += totalTradedVolumeByPrice.getQuantityTraded();
            }

            if (quantityTraded > 1000000) {
                ui.txt(Html.TOTAL_TRADED_VOLUME, BASIS_POINT_DECIMAL_FORMAT.format(1.0 / 1000000 * quantityTraded) + 'M');
            } else if (quantityTraded > 1000) {
                ui.txt(Html.TOTAL_TRADED_VOLUME, BASIS_POINT_DECIMAL_FORMAT.format(1.0 / 1000 * quantityTraded) + 'K');
            } else {
                ui.txt(Html.TOTAL_TRADED_VOLUME, quantityTraded);
            }

        }
    }

    private void drawBook() {
        final MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.book != null) {
            if (m.bookState == null) {
                ui.txt(Html.SYMBOL, getSymbol() + " - ?");
                ui.cls(Html.BOOK_STATE, "AUCTION", false);
                ui.cls(Html.BOOK_STATE, "NO_BOOK_STATE", true);
            } else if (m.bookState.getState().equals(BookState.AUCTION)) {
                ui.txt(Html.SYMBOL, getSymbol() + " - AUC");
                ui.cls(Html.BOOK_STATE, "AUCTION", true);
                ui.cls(Html.BOOK_STATE, "NO_BOOK_STATE", false);
            } else {
                ui.txt(Html.SYMBOL, getSymbol());
                ui.cls(Html.BOOK_STATE, "AUCTION", false);
                ui.cls(Html.BOOK_STATE, "NO_BOOK_STATE", false);
            }
            ui.title(getSymbol());

            if (dataForSymbol != null && dataForSymbol.spreadContractSet != null) {
                final SpreadContractSet contracts = dataForSymbol.spreadContractSet;
                ui.cls(Html.SYMBOL, "spread", symbol.equals(contracts.spread));
                ui.cls(Html.SYMBOL, "back", symbol.equals(contracts.back));
            }

            for (final Long price : levelByPrice.keySet()) {
                if (m.topOfBook != null && m.topOfBook.getBestBid().isExists() && m.topOfBook.getBestBid().getPrice() == price) {
                    bidQty(price, m.topOfBook.getBestBid().getQuantity());

                } else if (m.bookState != null && m.bookState.getState() == BookState.AUCTION && m.auctionIndicativePrice != null &&
                        m.auctionIndicativePrice.isHasIndicativePrice() && m.auctionIndicativePrice.getIndicativePrice() == price) {
                    if (m.auctionIndicativeSurplus != null && m.auctionIndicativeSurplus.getSurplusSide() == Side.BID) {
                        bidQty(price, m.auctionIndicativePrice.getQuantity() + m.auctionIndicativeSurplus.getSurplusQuantity());
                    } else {
                        bidQty(price, m.auctionIndicativePrice.getQuantity());
                    }
                } else {
                    bidQty(price, m.book.getLevel(price, Side.BID).getQuantity());
                }
                if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists() && m.topOfBook.getBestOffer().getPrice() == price) {
                    offerQty(price, m.topOfBook.getBestOffer().getQuantity());
                } else if (m.bookState != null && m.bookState.getState() == BookState.AUCTION && m.auctionIndicativePrice != null &&
                        m.auctionIndicativePrice.isHasIndicativePrice() && m.auctionIndicativePrice.getIndicativePrice() == price) {
                    if (m.auctionIndicativeSurplus != null && m.auctionIndicativeSurplus.getSurplusSide() == Side.OFFER) {
                        offerQty(price, m.auctionIndicativePrice.getQuantity() + m.auctionIndicativeSurplus.getSurplusQuantity());
                    } else {
                        offerQty(price, m.auctionIndicativePrice.getQuantity());
                    }
                } else {
                    offerQty(price, m.book.getLevel(price, Side.OFFER).getQuantity());
                }

                if (m.impliedTopOfBook != null) {
                    ui.cls(bidKey(price), "impliedBid",
                            m.impliedTopOfBook.getBestBid().isExists() && m.impliedTopOfBook.getBestBid().getPrice() == price);
                    ui.cls(offerKey(price), "impliedOffer",
                            m.impliedTopOfBook.getBestOffer().isExists() && m.impliedTopOfBook.getBestOffer().getPrice() == price);
                }

            }
        }
    }

    private void tryToDrawLadder() {
        final MarketDataForSymbol m = this.marketDataForSymbol;
        if (m != null && m.refData != null) {
            ui.clear();
            ui.clickable('#' + Html.SYMBOL);
            if (pendingRefDataAndSettle) {
                onRefDataAndSettleFirstAppeared();
            }
            centerPrice = this.marketDataForSymbol.priceOperations.tradeablePrice(centerPrice, Side.BID);
            pendingRefDataAndSettle = false;
            recenter();
            recenterLadderAndDrawPriceLevels();
            setUpBasketLink();
            setUpClickTrading();
        }
    }

    private void setUpBasketLink() {
        if (marketDataForSymbol.refData != null) {
            for (final Integer level : levelByPrice.values()) {
                ui.clickable('#' + Html.VOLUME + level);
            }
        }
    }

    static final double DEFAULT_EQUITY_NOTIONAL_EUR = 100000.0;

    private void onRefDataAndSettleFirstAppeared() {
        if (marketDataForSymbol.refData.getInstrumentStructure() instanceof CashOutrightStructure) {
            isCashEquityOrFX = true;
            pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.of(PricingMode.BPS, PricingMode.RAW));
            pricingMode.set(PricingMode.BPS);
            if (marketDataForSymbol.settle != null) {
                final long settlePrice = marketDataForSymbol.settle.getSettlementPrice();
                int quantity = (int) Mathematics.toQuantityFromNotionalInSafetyCurrency(DEFAULT_EQUITY_NOTIONAL_EUR, settlePrice,
                        marketDataForSymbol.refData);
                quantity = Math.max(50, quantity);
                defaultPrefs.put(Html.INP_RELOAD, "" + quantity);
            }
        } else if (marketDataForSymbol.refData.getInstrumentStructure() instanceof ForexPairStructure) {
            isCashEquityOrFX = true;
            pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.of(PricingMode.BPS, PricingMode.RAW));
            pricingMode.set(PricingMode.BPS);
            defaultPrefs.put(Html.INP_RELOAD, "1000000");
        } else {
            pricingMode = new EnumSwitcher<>(PricingMode.class, EnumSet.of(PricingMode.EFP, PricingMode.RAW));
            pricingMode.set(PricingMode.EFP);
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

    private String getSymbol() {
        if (dataForSymbol != null && dataForSymbol.displaySymbol != null) {
            return dataForSymbol.displaySymbol;
        }
        return symbol;
    }

    private void setUpClickTrading() {
        final boolean clickTradingEnabled = ladderOptions.traders.contains(client.getUserName());
        setupButtons();

        view.trading(clickTradingEnabled,
                filterUsableOrderTypes(ladderOptions.orderTypesLeft),
                filterUsableOrderTypes(ladderOptions.orderTypesRight));

        for (final Map.Entry<String, Integer> entry : buttonQty.entrySet()) {
            final String display;
            display = formatClickQty(entry.getValue());
            ui.txt(entry.getKey(), display);
        }
        for (int i = 0; i < levels; i++) {
            ui.clickable('#' + Html.PRICE + i);
            ui.clickable('#' + Html.BID + i);
            ui.clickable('#' + Html.OFFER + i);
            ui.clickable('#' + Html.ORDER + i);
        }
        ui.clickable(Html.BUTTONS);
        ui.scrollable('#' + Html.LADDER);
        ui.clickable('#' + Html.BUY_QTY);
        ui.clickable('#' + Html.SELL_QTY);

        ui.clickable('#' + Html.BUY_OFFSET_UP);
        ui.clickable('#' + Html.BUY_OFFSET_DOWN);
        ui.clickable('#' + Html.SELL_OFFSET_UP);
        ui.clickable('#' + Html.SELL_OFFSET_DOWN);

        ui.clickable('#' + Html.START_BUY);
        ui.clickable('#' + Html.STOP_BUY);
        ui.clickable('#' + Html.START_SELL);
        ui.clickable('#' + Html.STOP_SELL);

        ui.cls(Html.OFFSET_CONTROL, Html.HIDDEN, false);

    }

    private static String formatPosition(final double qty) {
        final String display;
        final double absQty = Math.abs(qty);
        if (absQty < 10000) {
            display = ((int) qty) + "";
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
            display = qty + "";
        } else if (qty < 100000) {
            display = qty / 1000 + "K";
        } else {
            display = MILLIONS_QTY_FORMAT.format((double) qty / 1000000) + 'M';

        }
        return display;
    }

    private Collection<String> filterUsableOrderTypes(final Collection<String> types) {
        return types.stream().filter(input -> {
            boolean oldOrderType = ladderOptions.serverResolver.resolveToServerName(symbol, input) != null;
            boolean newOrderType = orderEntryMap.containsKey(symbol) && managedOrderTypes.contains(input);
            return oldOrderType || newOrderType;
        }).collect(Collectors.toList());
    }

    private void setupButtons() {
        if (marketDataForSymbol.refData.getInstrumentStructure() instanceof CashOutrightStructure) {
            buttonQty.put("btn_qty_1", 1);
            buttonQty.put("btn_qty_2", 10);
            buttonQty.put("btn_qty_3", 100);
            buttonQty.put("btn_qty_4", 1000);
            buttonQty.put("btn_qty_5", 5000);
            buttonQty.put("btn_qty_6", 10000);
        } else if (marketDataForSymbol.refData.getInstrumentStructure() instanceof ForexPairStructure) {
            buttonQty.put("btn_qty_1", 100000);
            buttonQty.put("btn_qty_2", 200000);
            buttonQty.put("btn_qty_3", 500000);
            buttonQty.put("btn_qty_4", 1000000);
            buttonQty.put("btn_qty_5", 2500000);
            buttonQty.put("btn_qty_6", 5000000);
        } else {
            buttonQty.put("btn_qty_1", 1);
            buttonQty.put("btn_qty_2", 5);
            buttonQty.put("btn_qty_3", 10);
            buttonQty.put("btn_qty_4", 25);
            buttonQty.put("btn_qty_5", 50);
            buttonQty.put("btn_qty_6", 100);
        }
    }

    public void recenterIfTimeoutElapsed() {
        final MarketDataForSymbol m = marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null) {
            if (getCenterPrice(m) >= bottomPrice && getCenterPrice(m) <= topPrice) {
                resetLastCenteredTime();
            } else if (System.currentTimeMillis() - lastCenteredTime > RECENTER_TIME_MS) {
                recenter();
                recenterLadderAndDrawPriceLevels();
                updateEverything();
            }
            ui.cls(Html.LADDER, Html.RECENTERING,
                    lastCenteredTime > 0 && RECENTER_WARN_TIME_MS <= System.currentTimeMillis() - lastCenteredTime);
        }
    }

    private void moveLadderTowardCenter() {
        final int direction = (int) Math.signum(getCenterPrice(marketDataForSymbol) - centerPrice);
        centerPrice = getPriceNTicksFrom(centerPrice, AUTO_RECENTER_TICKS * direction);
    }

    private void resetLastCenteredTime() {
        lastCenteredTime = System.currentTimeMillis();
    }

    private long getCenterPrice(final MarketDataForSymbol m) {
        if (!pendingRefDataAndSettle) {
            long center = 0;
            if (m.settle != null) {
                center = m.settle.getSettlementPrice();
            }
            if (m.lastTrade != null) {
                center = m.lastTrade.getPrice();
            }
            if (m.auctionTradeUpdate != null && m.auctionTradeUpdate.getQuantity() > 0) {
                center = m.auctionTradeUpdate.getPrice();
            }
            if (workingOrdersForSymbol != null && !workingOrdersForSymbol.ordersByKey.isEmpty()) {
                final long n = workingOrdersForSymbol.ordersByKey.size();
                long avgPrice = 0L;
                for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
                    avgPrice += (orderUpdateFromServer.value.getPrice() / n);
                }
                center = avgPrice;
            }
            if (m.auctionIndicativePrice != null && m.auctionIndicativePrice.isHasIndicativePrice() &&
                    m.auctionIndicativePrice.getQuantity() > 0) {
                center = m.auctionIndicativePrice.getIndicativePrice();
            }
            if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists()) {
                center = m.topOfBook.getBestOffer().getPrice();
            }
            if (m.topOfBook != null && m.topOfBook.getBestBid().isExists()) {
                center = m.topOfBook.getBestBid().getPrice();
            }
            if (dataForSymbol != null && dataForSymbol.theoreticalValue != null) {
                center = dataForSymbol.theoreticalValue;
            }
            if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists() && m.topOfBook.getBestBid().isExists()) {
                center = getMidPrice(m);
            }
            return m.priceOperations.tradeablePrice(center, Side.BID);
        }
        return 0;
    }

    private static long getMidPrice(final MarketDataForSymbol m) {
        final long midPrice = m.topOfBook.getBestBid().getPrice() / 2 + m.topOfBook.getBestOffer().getPrice() / 2;
        return m.priceOperations.tradeablePrice(midPrice, Side.BID);
    }

    private void recenterLadderAndDrawPriceLevels() {
        final MarketDataForSymbol m = this.marketDataForSymbol;
        if (m != null && m.refData != null) {

            final int centerLevel = levels / 2;
            long price = centerPrice;

            for (int i = 0; i < centerLevel; i++) {
                price = getPriceNTicksFrom(price, 1);
                topPrice = price;
            }
            levelByPrice.clear();

            price = topPrice;
            for (int i = 0; i < levels; i++) {
                levelByPrice.put(price, i);

                bidQty(price, 0);
                offerQty(price, 0);
                ui.txt(volumeKey(price), Html.EMPTY);
                ui.txt(orderKey(price), Html.EMPTY);

                ui.data(bidKey(price), Html.PRICE_KEY, price);
                ui.data(offerKey(price), Html.PRICE_KEY, price);
                ui.data(orderKey(price), Html.PRICE_KEY, price);

                bottomPrice = price;
                price = getPriceNTicksFrom(price, -1);
            }

            drawPriceLevels(dataForSymbol);
        }
    }

    private void drawPrice(final MarketDataForSymbol m, final long price, final String key) {
        ui.txt(key, m.priceFormat.format(price));
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
            if (Html.INP_QTY.equals(label)) {
                clickTradingBoxQty = Integer.valueOf(value);
            } else if (persistentPrefs.contains(label)) {
                ladderPrefsForSymbolUser.set(label, value);
            } else {
                throw new IllegalArgumentException("Update for unknown value: " + label + ' ' + dataArg);
            }
        }
        drawClickTrading();
        flush();

    }

    @Override
    public void onScroll(final String direction) {
        if ("up".equals(direction)) {
            centerPrice = marketDataForSymbol.priceOperations.nTicksAway(centerPrice, 1, PriceUtils.Direction.Add);
        } else if ("down".equals(direction)) {
            centerPrice = marketDataForSymbol.priceOperations.nTicksAway(centerPrice, 1, PriceUtils.Direction.Subtract);
        } else {
            return;
        }
        resetLastCenteredTime();
        recenterLadderAndDrawPriceLevels();
        updateEverything();
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
            if (null != marketDataForSymbol.topOfBook) {
                final BestPrice ask = marketDataForSymbol.topOfBook.getBestOffer();
                if (ask.isExists()) {
                    centerPrice = ask.getPrice();
                }
            }
        } else if (keyCode == END_KEY) {
            if (null != marketDataForSymbol.topOfBook) {
                final BestPrice bid = marketDataForSymbol.topOfBook.getBestBid();
                if (bid.isExists()) {
                    centerPrice = bid.getPrice();
                }
            }
        } else {
            return;
        }
        resetLastCenteredTime();
        recenterLadderAndDrawPriceLevels();
        updateEverything();
        flush();
    }

    private long getPriceNTicksFrom(final long price, final int n) {
        return marketDataForSymbol.priceOperations.nTicksAway(price, Math.abs(n),
                n >= 0 ? PriceUtils.Direction.Add : PriceUtils.Direction.Subtract);
    }

    @Override
    public void onIncoming(final String[] args) {
        final String cmd = args[0];
        if ("heartbeat".equals(cmd)) {
            onHeartbeat(Long.valueOf(args[1]), Long.valueOf(args[2]));
        }
    }

    @Override
    public void onDblClick(final String label, final Map<String, String> dataArg) {
        if (workingOrdersForSymbol != null) {
            if (Html.BUY_QTY.equals(label)) {
                cancelAllForSide(com.drwtrading.london.protocols.photon.execution.Side.BID);
            } else if (Html.SELL_QTY.equals(label)) {
                cancelAllForSide(com.drwtrading.london.protocols.photon.execution.Side.OFFER);
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
            } else if (Html.BUTTON_CLR.equals(label)) {
                clickTradingBoxQty = 0;
            } else if (label.startsWith(Html.BID) || label.startsWith(Html.OFFER)) {
                if (l != null) {
                    submitOrderClick(label, data, getPref(l, Html.ORDER_TYPE_LEFT), autoHedge, ladderClickTradingIssuePublisher);
                }
            } else if (label.startsWith(Html.ORDER)) {
                final long price = Long.valueOf(data.get("price"));
                if (label.equals(orderKey(price))) {
                    cancelWorkingOrders(price, data);
                } else {
                    System.out.println("Mismatched label: " + data.get("price") + ' ' + orderKey(price) + ' ' + label);
                }
            } else if (label.equals(Html.BUY_OFFSET_UP)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.BID, Direction.UP));
            } else if (label.equals(Html.BUY_OFFSET_DOWN)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.BID, Direction.DOWN));
            } else if (label.equals(Html.SELL_OFFSET_UP)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.OFFER, Direction.UP));
            } else if (label.equals(Html.SELL_OFFSET_DOWN)) {
                commandPublisher.publish(new UpdateOffset(symbol, com.drwtrading.london.photons.reddal.Side.OFFER, Direction.DOWN));
            } else if (label.equals(Html.START_BUY)) {
                commandPublisher.publish(new Command(ReddalCommand.START, symbol, com.drwtrading.london.photons.reddal.Side.BID));
            } else if (label.equals(Html.START_SELL)) {
                commandPublisher.publish(new Command(ReddalCommand.START, symbol, com.drwtrading.london.photons.reddal.Side.OFFER));
            } else if (label.equals(Html.STOP_BUY)) {
                commandPublisher.publish(new Command(ReddalCommand.STOP, symbol, com.drwtrading.london.photons.reddal.Side.BID));
            } else if (label.equals(Html.STOP_SELL)) {
                commandPublisher.publish(new Command(ReddalCommand.STOP, symbol, com.drwtrading.london.photons.reddal.Side.OFFER));
            } else if (label.equals(Html.PRICING_BPS)) {
                pricingMode.set(PricingMode.BPS);
            } else if (label.equals(Html.PRICING_RAW)) {
                pricingMode.set(PricingMode.RAW);
            } else if (label.equals(Html.PRICING_EFP)) {
                pricingMode.set(PricingMode.EFP);
            } else if (label.startsWith(Html.PRICE_KEY)) {
                pricingMode.next();
            } else if (label.startsWith(Html.SYMBOL)) {
                if (nextContract()) {
                    return;
                }
            } else if (label.startsWith(Html.VOLUME)) {
                view.launchBasket(symbol);
            }
        } else if ("right".equals(button)) {
            if (label.startsWith(Html.BID) || label.startsWith(Html.OFFER)) {
                if (l != null) {
                    submitOrderClick(label, data, getPref(l, Html.ORDER_TYPE_RIGHT), "true".equals(getPref(l, Html.AUTO_HEDGE_RIGHT)),
                            ladderClickTradingIssuePublisher);
                }
            } else if (label.startsWith(Html.ORDER)) {
                rightClickModify(data, autoHedge);
            } else if (label.startsWith(Html.SYMBOL)) {
                if (dataForSymbol != null && dataForSymbol.spreadContractSet != null) {
                    final SpreadContractSet contracts = dataForSymbol.spreadContractSet;
                    final String prevContract = contracts.prev(symbol);
                    if (prevContract != null && !symbol.equals(prevContract)) {
                        view.goToSymbol(prevContract);
                        return;
                    }
                }
            }
        } else if ("middle".equals(button)) {
            if (label.startsWith(Html.PRICE)) {
                recenterLaddersForUser.publish(new LadderPresenter.RecenterLaddersForUser(client.getUserName()));
            } else if (label.startsWith(Html.ORDER)) {
                final String price = data.get("price");
                final String url = String.format("/orders#%s,%s", symbol, price);
                final Collection<Main.WorkingOrderUpdateFromServer> orders = workingOrdersForSymbol.ordersByPrice.get(Long.valueOf(price));
                if (orders.size() > 0) {
                    view.popUp(url, "orders", 270, 20 * (1 + orders.size()));
                }
            } else if (label.startsWith(Html.SYMBOL)) {
                final UserCycleRequest cycleRequest = new UserCycleRequest(client.getUserName());
                userCycleContractPublisher.publish(cycleRequest);
                return;
            }
        }
        updateEverything();
        drawClickTrading();
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

    private boolean shouldAutoHedge() {
        if (ladderPrefsForSymbolUser != null) {
            return "true".equals(getPref(ladderPrefsForSymbolUser, Html.AUTO_HEDGE_LEFT));
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
        updateEverything();
        flush();
    }

    public void recenterLadderForUser(final LadderPresenter.RecenterLaddersForUser recenterLaddersForUser) {
        if (client.getUserName().equals(recenterLaddersForUser.user)) {
            moveLadderToCenter();
            resetLastCenteredTime();
            recenterLadderAndDrawPriceLevels();
            updateEverything();
            flush();
        }
    }

    private void moveLadderToCenter() {
        centerPrice = getCenterPrice(marketDataForSymbol);
    }

    public static final AtomicLong heartbeatSeqNo = new AtomicLong(0L);

    public static class HeartbeatRoundtrip extends Struct {

        public final String userName;
        public final String symbol;
        public final long sentTimeMillis;
        public final long returnTimeMillis;
        public final long roundtripMillis;

        public HeartbeatRoundtrip(final String userName, final String symbol, final long sentTimeMillis, final long returnTimeMillis, final long roundtripMillis,
                                  final long seqNo) {
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

    public static final Set<String> persistentPrefs = newFastSet();

    static {
        persistentPrefs.add(Html.WORKING_ORDER_TAG);
        persistentPrefs.add(Html.INP_RELOAD);
        persistentPrefs.add(Html.AUTO_HEDGE_LEFT);
        persistentPrefs.add(Html.AUTO_HEDGE_RIGHT);
        persistentPrefs.add(Html.ORDER_TYPE_LEFT);
        persistentPrefs.add(Html.ORDER_TYPE_RIGHT);
        persistentPrefs.add(Html.RANDOM_RELOAD);
    }

    public final Map<String, String> defaultPrefs = newFastMap();

    private void initDefaultPrefs() {
        defaultPrefs.put(Html.WORKING_ORDER_TAG, "CHAD");
        defaultPrefs.put(Html.INP_RELOAD, "50");
        defaultPrefs.put(Html.AUTO_HEDGE_LEFT, "true");
        defaultPrefs.put(Html.AUTO_HEDGE_RIGHT, "true");
        defaultPrefs.put(Html.ORDER_TYPE_LEFT, "HAWK");
        defaultPrefs.put(Html.ORDER_TYPE_RIGHT, "MANUAL");
        defaultPrefs.put(Html.RANDOM_RELOAD, "true");
    }

    public void drawClickTrading() {
        final LadderPrefsForSymbolUser l = ladderPrefsForSymbolUser;
        if (!pendingRefDataAndSettle && l != null) {

            ui.txt(Html.INP_QTY, clickTradingBoxQty);

            for (final String pref : persistentPrefs) {
                ui.txt(pref, getPref(ladderPrefsForSymbolUser, pref));
            }

            for (final String type : ladderOptions.orderTypesLeft) {
                ui.cls(Html.ORDER_TYPE_LEFT, type, type.equals(getPref(l, Html.ORDER_TYPE_LEFT)));
            }

            for (final String type : ladderOptions.orderTypesRight) {
                ui.cls(Html.ORDER_TYPE_RIGHT, type, type.equals(getPref(l, Html.ORDER_TYPE_RIGHT)));
            }

            for (final Long price : levelByPrice.keySet()) {
                ui.cls(orderKey(price), Html.MODIFY_PRICE_SELECTED, price.equals(modifyFromPrice));
            }

        }

        if (dataForSymbol != null) {
            ui.cls(Html.OFFSET_CONTROL, Html.HIDDEN, !dataForSymbol.symbolAvailable);
        }
    }

    private String getPref(final LadderPrefsForSymbolUser l, final String id) {
        return l.get(id, defaultPrefs.get(id));
    }

    private void cancelAllForSide(final com.drwtrading.london.protocols.photon.execution.Side side) {
        for (final Main.WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
            if (orderUpdateFromServer.value.getSide() == side) {
                cancelOrder(orderUpdateFromServer);
            }
        }
        orderUpdatesForSymbol.updatesByKey.values().stream().forEach(update -> {
            if (convertSide(update.update.getOrder().getSide()) == side) {
                cancelManagedOrder(update);
            }
        });
    }

    private static com.drwtrading.london.protocols.photon.execution.Side convertSide(final OrderSide s1) {
        final com.drwtrading.london.protocols.photon.execution.Side s;
        if (s1 == OrderSide.BUY) {
            s = com.drwtrading.london.protocols.photon.execution.Side.BID;
        } else {
            s = com.drwtrading.london.protocols.photon.execution.Side.OFFER;
        }
        return s;
    }

    Long modifyFromPrice = null;
    Long modifyFromPriceSelectedTime = 0L;

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


    private final Set<String> managedOrderTypes = Arrays.asList(ManagedOrderType.values()).stream().map(Enum::toString).collect(Collectors.toSet());
    private final Set<String> oldOrderTypes = Arrays.asList(RemoteOrderType.values()).stream().map(Enum::toString).collect(Collectors.toSet());

    private void submitOrderClick(final String label, final Map<String, String> data, final String orderType, final boolean autoHedge,
                                  final Publisher<LadderClickTradingIssue> ladderClickTradingIssuesPublisher) {

        if (ladderOptions.traders.contains(client.getUserName())) {


            final long price = Long.valueOf(data.get("price"));

            final com.drwtrading.london.protocols.photon.execution.Side side =
                    label.equals(bidKey(price)) ? com.drwtrading.london.protocols.photon.execution.Side.BID :
                            label.equals(offerKey(price)) ? com.drwtrading.london.protocols.photon.execution.Side.OFFER : null;

            final String tag = ladderPrefsForSymbolUser.get(Html.WORKING_ORDER_TAG);

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

            final boolean randomReload = "true".equals(getPref(ladderPrefsForSymbolUser, Html.RANDOM_RELOAD));
            final int reloadBoxQty = Integer.valueOf(getPref(ladderPrefsForSymbolUser, Html.INP_RELOAD));

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

        final Publisher<OrderEntryCommand> publisher = orderEntryMap.get(symbol);

        if (null != publisher) {
            final ManagedOrderType managedOrderType = ManagedOrderType.valueOf(orderType);
            tradingBoxQty = managedOrderType.getQty(tradingBoxQty);
            if (tradingBoxQty == 0) {
                tradingBoxQty = clickTradingBoxQty;
            }
            final com.drwtrading.london.photons.eeifoe.RemoteOrder remoteOrder = new com.drwtrading.london.photons.eeifoe.RemoteOrder(
                    symbol, side == com.drwtrading.london.protocols.photon.execution.Side.BID ? OrderSide.BUY : OrderSide.SELL, price, tradingBoxQty,
                    client.getUserName(), managedOrderType.getOrder(price, tradingBoxQty)
            );
            final Submit submit = new Submit(remoteOrder);
            publisher.publish(submit);
        } else {
            clickTradingIssue(new LadderClickTradingIssue(symbol, "Cannot find server to send order type " + orderType + " for symbol " + symbol));
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

        public CommandTrace(final String command, final String user, final String symbol, final String orderType, final boolean autoHedge, final long price, final String side,
                            final String tag, final int quantity, final int chainId) {
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

    private void submitOrder(final String orderType, final boolean autoHedge, final long price, final com.drwtrading.london.protocols.photon.execution.Side side,
                             final String tag, final Publisher<LadderClickTradingIssue> ladderClickTradingIssues) {

        final int sequenceNumber = orderSeqNo++;

        trace.publish(new CommandTrace("submit", client.getUserName(), symbol, orderType, autoHedge, price, side.toString(), tag,
                clickTradingBoxQty, sequenceNumber));

        if (clientSpeedState == ClientSpeedState.TooSlow) {
            final String message =
                    "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", client " + client.getUserName() +
                            " is " + clientSpeedState.toString() + " speed: " + getClientSpeedMillis() + "ms";
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
            return;
        }

        System.out.println("Order: " + symbol + ' ' + remoteOrderType.toString() + " resolved to " + serverName);

        final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus = tradingStatusForAll.serverTradingStatusMap.get(serverName);

        if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
            final String message = "Cannot submit order " + side + ' ' + clickTradingBoxQty + " for " + symbol + ", server " + serverName +
                    " has status " + (serverTradingStatus == null ? null : serverTradingStatus.toString());
            statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, message));
            ladderClickTradingIssues.publish(new LadderClickTradingIssue(symbol, message));
            return;
        }

        final RemoteSubmitOrder remoteSubmitOrder = new RemoteSubmitOrder(serverName, client.getUserName(), sequenceNumber,
                new RemoteOrder(symbol, side, price, clickTradingBoxQty, remoteOrderType, autoHedge, tag));

        remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(serverName, remoteSubmitOrder));

    }

    private void modifyOrder(final boolean autoHedge, final long price, final Main.WorkingOrderUpdateFromServer order,
                             final WorkingOrderUpdate workingOrderUpdate, final int totalQuantity) {
        final RemoteModifyOrder remoteModifyOrder = new RemoteModifyOrder(order.fromServer, client.getUserName(), workingOrderUpdate.getChainId(),
                getRemoteOrderFromWorkingOrder(autoHedge, workingOrderUpdate.getPrice(), workingOrderUpdate,
                        workingOrderUpdate.getTotalQuantity()),
                getRemoteOrderFromWorkingOrder(autoHedge, price, workingOrderUpdate, totalQuantity));

        trace.publish(
                new CommandTrace("modify", client.getUserName(), symbol, order.value.getWorkingOrderType().toString(), autoHedge, price,
                        order.value.getSide().toString(), order.value.getTag(), clickTradingBoxQty, order.value.getChainId()));

        if (ladderOptions.traders.contains(client.getUserName())) {

            final TradingStatusWatchdog.ServerTradingStatus serverTradingStatus =
                    tradingStatusForAll.serverTradingStatusMap.get(order.fromServer);
            if (clientSpeedState == ClientSpeedState.TooSlow) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING,
                        "Cannot modify order , client " + client.getUserName() + " is " + clientSpeedState.toString() + " speed: " +
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

    private void cancelWorkingOrders(final Long price, final Map<String, String> data) {
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

    public static RemoteOrderType getRemoteOrderType(final String orderType) {
        for (final RemoteOrderType remoteOrderType : RemoteOrderType.values()) {
            if (remoteOrderType.toString().toUpperCase().equals(orderType.toUpperCase())) {
                return remoteOrderType;
            }
        }
        return RemoteOrderType.MANUAL;
    }

    public static String getOrderType(final WorkingOrderType workingOrderType) {
        if (workingOrderType == WorkingOrderType.MARKET) {
            return "MKT_CLOSE";
        }
        return workingOrderType.name();
    }

    public void onSingleOrderCommand(final OrdersPresenter.SingleOrderCommand singleOrderCommand) {
        final Main.WorkingOrderUpdateFromServer orderUpdateFromServer = workingOrdersForSymbol.ordersByKey.get(singleOrderCommand.getOrderKey());
        if (orderUpdateFromServer == null) {
            statsPublisher.publish(new AdvisoryStat("Reddal", AdvisoryStat.Level.WARNING,
                    "Could not find order for command: " + singleOrderCommand.toString()));
            return;
        }
        if (singleOrderCommand instanceof OrdersPresenter.CancelOrder) {
            cancelOrder(orderUpdateFromServer);
        } else if (singleOrderCommand instanceof OrdersPresenter.ModifyOrderQuantity) {
            final int totalQuantity = orderUpdateFromServer.value.getFilledQuantity() +
                    ((OrdersPresenter.ModifyOrderQuantity) singleOrderCommand).newRemainingQuantity;
            modifyOrder(shouldAutoHedge(), orderUpdateFromServer.value.getPrice(), orderUpdateFromServer, orderUpdateFromServer.value,
                    totalQuantity);
        }
    }

    // Heartbeats

    public static enum ClientSpeedState {
        TooSlow(10000),
        Slow(5000),
        Fine(0);
        public final int thresholdMillis;

        ClientSpeedState(final int thresholdMillis) {
            this.thresholdMillis = thresholdMillis;
        }
    }

    private Long lastHeartbeatSentMillis = null;
    private long lastHeartbeatRoundtripMillis = 0;
    private ClientSpeedState clientSpeedState = ClientSpeedState.Fine;

    public void sendHeartbeat() {
        if (lastHeartbeatSentMillis == null) {
            lastHeartbeatSentMillis = System.currentTimeMillis();
            ui.send(UiPipeImpl.cmd("heartbeat", lastHeartbeatSentMillis, heartbeatSeqNo.getAndIncrement()));
        }
    }

    private void onHeartbeat(final long sentTimeMillis, final long seqNo) {
        final long returnTimeMillis = System.currentTimeMillis();
        if (lastHeartbeatSentMillis == sentTimeMillis) {
            lastHeartbeatSentMillis = null;
            lastHeartbeatRoundtripMillis = returnTimeMillis - sentTimeMillis;
            heartbeatRoundtripPublisher.publish(
                    new HeartbeatRoundtrip(client.getUserName(), symbol, sentTimeMillis, returnTimeMillis, lastHeartbeatRoundtripMillis,
                            seqNo));
        } else {
            throw new RuntimeException(
                    "Received heartbeat reply " + sentTimeMillis + " which does not match last sent heartbeat " + lastHeartbeatSentMillis);
        }
    }

    public long getClientSpeedMillis() {
        return Math.max(lastHeartbeatSentMillis == null ? 0L : System.currentTimeMillis() - lastHeartbeatSentMillis,
                lastHeartbeatRoundtripMillis);
    }

    public void checkClientSpeed() {
        if (getClientSpeedMillis() > ClientSpeedState.TooSlow.thresholdMillis) {
            clientSpeedState = ClientSpeedState.TooSlow;
        } else if (getClientSpeedMillis() > ClientSpeedState.Slow.thresholdMillis) {
            clientSpeedState = ClientSpeedState.Slow;
        } else {
            clientSpeedState = ClientSpeedState.Fine;
        }
    }

    // Update helpers

    public void bidQty(final long price, final int qty) {
        ui.txt(bidKey(price), formatMktQty(qty));
        ui.cls(bidKey(price), Html.BID_ACTIVE, qty > 0);
        styleBigNumber(bidKey(price), qty);
    }

    private void styleBigNumber(final String key, final int qty) {
        ui.cls(key, Html.BIG_NUMBER, qty > BIG_NUMBER_THRESHOLD && qty < REALLY_BIG_NUMBER_THRESHOLD);
    }

    public void offerQty(final long price, final int qty) {
        ui.txt(offerKey(price), formatMktQty(qty));
        ui.cls(offerKey(price), Html.OFFER_ACTIVE, qty > 0);
        styleBigNumber(offerKey(price), qty);
    }

    public static String formatMktQty(final int qty) {
        if (qty <= 0) {
            return Html.EMPTY;
        }
        if (qty >= REALLY_BIG_NUMBER_THRESHOLD) {
            final double d = (double) qty / 1000000;
            return new BigDecimal(d).round(new MathContext(2)).toString() + 'M';
        }
        return Integer.toString(qty);
    }

    public void workingQty(final long price, final int qty, final String side, final Set<WorkingOrderType> orderTypes, boolean hasEeifOEOrder) {
        ui.txt(orderKey(price), formatMktQty(qty));
        ui.cls(orderKey(price), Html.WORKING_QTY, qty > 0);
        styleBigNumber(orderKey(price), qty);
        ui.cls(orderKey(price), Html.WORKING_BID, BID_LOWERCASE.equals(side));
        ui.cls(orderKey(price), Html.WORKING_OFFER, OFFER_LOWERCASE.equals(side));
        for (final WorkingOrderType workingOrderType : WorkingOrderType.values()) {
            ui.cls(orderKey(price), Html.WORKING_ORDER_TYPE + getOrderType(workingOrderType).toLowerCase(),
                    !hasEeifOEOrder && orderTypes.contains(workingOrderType));
        }
        ui.cls(orderKey(price), Html.EEIF_ORDER_TYPE, hasEeifOEOrder);
    }

    private static String laserKey(final String name) {
        return Html.LASER + name;
    }

    private String bidKey(final long price) {
        return Html.BID + levelByPrice.get(price);
    }

    private String offerKey(final long price) {
        return Html.OFFER + levelByPrice.get(price);
    }

    private String priceKey(final long price) {
        return Html.PRICE + levelByPrice.get(price);
    }

    private String orderKey(final long price) {
        return Html.ORDER + levelByPrice.get(price);
    }

    private String volumeKey(final long price) {
        return Html.VOLUME + levelByPrice.get(price);
    }

    private String tradeKey(final long price) {
        return Html.TRADE + levelByPrice.get(price);
    }

}
