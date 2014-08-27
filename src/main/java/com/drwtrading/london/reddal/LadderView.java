package com.drwtrading.london.reddal;

import com.drwtrading.london.fastui.UiPipe;
import com.drwtrading.london.fastui.UiPipeImpl;
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
import com.drwtrading.london.protocols.photon.marketdata.BookState;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.util.PriceUtils;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.websockets.WebSocketClient;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;
import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class LadderView implements UiPipe.UiEventHandler {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final DecimalFormat BASIS_POINT_DECIMAL_FORMAT = new DecimalFormat(".0");
    public static final DecimalFormat EFP_DECIMAL_FORMAT = new DecimalFormat("0.00");
    public static final int MODIFY_TIMEOUT_MS = 5000;
    public static final int AUTO_RECENTER_TICKS = 3;

    public static final int RECENTER_TIME_MS = 11000;
    public static final int RECENTER_WARN_TIME_MS = 9000;


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

        // Click-trading
        public static final Map<String, Integer> BUTTON_QTY = new HashMap<String, Integer>();

        static {
            BUTTON_QTY.put("btn_qty_1", 1);
            BUTTON_QTY.put("btn_qty_2", 10);
            BUTTON_QTY.put("btn_qty_3", 100);
            BUTTON_QTY.put("btn_qty_4", 1000);
            BUTTON_QTY.put("btn_qty_5", 5000);
            BUTTON_QTY.put("btn_qty_6", 10000);
        }

        public static final String BUTTON_CLR = "btn_clear";
        public static final String INP_RELOAD = "inp_reload";
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

        // Divs
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
    }

    public static final int PG_UP = 33;
    public static final int PG_DOWN = 34;

    public static enum PricingMode {BPS, EFP, RAW}

    private final WebSocketClient client;
    private final LadderPresenter.View view;
    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher;
    private final LadderOptions ladderOptions;
    private final Publisher<ReddalMessage> commandPublisher;
    private final UiPipeImpl ui;
    private final Publisher<StatsMsg> statsPublisher;
    private final TradingStatusForAll tradingStatusForAll;
    private final Publisher<HeartbeatRoundtrip> heartbeatRoundtripPublisher;

    public String symbol;
    private MarketDataForSymbol marketDataForSymbol;
    private WorkingOrdersForSymbol workingOrdersForSymbol;
    private ExtraDataForSymbol dataForSymbol;
    private int levels;
    private LadderPrefsForSymbolUser ladderPrefsForSymbolUser;

    Map<Long, Integer> levelByPrice = new HashMap<Long, Integer>();

    private boolean pendingRefDataAndSettle = true;

    private long centerPrice;
    private long topPrice;
    private long bottomPrice;

    public PricingMode pricingMode = PricingMode.RAW;
    public long lastCenteredTime = 0;

    public LadderView(WebSocketClient client, UiPipe ui, LadderPresenter.View view, Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher, LadderOptions ladderOptions, Publisher<StatsMsg> statsPublisher, TradingStatusForAll tradingStatusForAll, Publisher<HeartbeatRoundtrip> heartbeatRoundtripPublisher, Publisher<ReddalMessage> commandPublisher) {
        this.client = client;
        this.view = view;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.ladderOptions = ladderOptions;
        this.commandPublisher = commandPublisher;
        this.ui = (UiPipeImpl) ui;
        this.statsPublisher = statsPublisher;
        this.tradingStatusForAll = tradingStatusForAll;
        this.heartbeatRoundtripPublisher = heartbeatRoundtripPublisher;
        this.ui.setHandler(this);
    }

    public void subscribeToSymbol(String symbol, int levels, MarketDataForSymbol marketDataForSymbol, WorkingOrdersForSymbol workingOrdersForSymbol, ExtraDataForSymbol extraDataForSymbol, LadderPrefsForSymbolUser ladderPrefsForSymbolUser) {
        this.symbol = symbol;
        this.marketDataForSymbol = marketDataForSymbol;
        this.workingOrdersForSymbol = workingOrdersForSymbol;
        this.dataForSymbol = extraDataForSymbol;
        this.levels = levels;
        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;
        this.pendingRefDataAndSettle = true;
        drawLadder();
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
        for (PricingMode mode : PricingMode.values()) {
            ui.cls(Html.PRICING + mode.toString(), "active_mode", pricingMode == mode);
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

    private void drawLadderIfRefDataHasJustComeIn() {
        if (pendingRefDataAndSettle) {
            drawLadder();
        }
    }

    // Drawing

    private void drawClock() {
        ui.txt(Html.CLOCK, SIMPLE_DATE_FORMAT.format(new Date()));
    }

    private void drawClientSpeedState() {
        ui.cls(Html.CLOCK, "slow", clientSpeedState == ClientSpeedState.Slow);
        ui.cls(Html.CLOCK, "very-slow", clientSpeedState == ClientSpeedState.TooSlow);
    }

    private void drawMetaData() {
        ExtraDataForSymbol d = dataForSymbol;
        MarketDataForSymbol m = marketDataForSymbol;
        if (!pendingRefDataAndSettle && d != null && m != null) {
            drawLaserLines(d, m);

            // Desk position
            if (d.deskPosition != null && d.deskPosition.getPosition() != null && !d.deskPosition.getPosition().equals("")) {
                ui.txt(Html.DESK_POSITION, d.deskPosition.getPosition());
                decorateUpDown(Html.DESK_POSITION, new BigDecimal(d.deskPosition.getPosition()).longValue());
            }

            // Day position
            if (d.dayPosition != null) {
                ui.txt(Html.POSITION, d.dayPosition.getNet());
                decorateUpDown(Html.POSITION, d.dayPosition.getNet());
            }

            // Change on day
            Long lastTradeChangeOnDay = getLastTradeChangeOnDay(m);
            if(lastTradeChangeOnDay != null) {
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
            for (LadderText ladderText : dataForSymbol.ladderTextByPosition.values()) {
                ui.txt(Html.TEXT + ladderText.getCell(), ladderText.getText());
            }

            // Last trade
            for (Map.Entry<Long, Integer> entry : levelByPrice.entrySet()) {
                ui.cls(priceKey(entry.getKey()), Html.LAST_BUY, d.lastBuy != null && d.lastBuy.getPrice() == entry.getKey());
                ui.cls(priceKey(entry.getKey()), Html.LAST_SELL, d.lastSell != null && d.lastSell.getPrice() == entry.getKey());
            }
        }
    }

    private void drawLaserLines(ExtraDataForSymbol d, MarketDataForSymbol m) {
        // Display laserlines
        for (LaserLine laserLine : d.laserLineByName.values()) {
            String laserKey = laserKey(laserLine.getId());
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
                        long priceAbove = m.priceOperations.nTicksAway(price, 1, PriceUtils.Direction.Add);
                        if (price <= laserLine.getPrice() && laserLine.getPrice() <= priceAbove && levelByPrice.containsKey(price)) {
                            long fractionalPrice = laserLine.getPrice() - price;
                            double tickFraction = 1.0 * fractionalPrice / (priceAbove - price);
                            ui.height(laserKey, priceKey(price), tickFraction);
                            break;
                        }
                        price = priceAbove;
                    }
                }
            }
        }
    }

    private void drawPriceLevels(final ExtraDataForSymbol d) {
        if (!pendingRefDataAndSettle) {
            LaserLine theo = new LaserLine(symbol, ladderOptions.theoLaserLine, Long.MIN_VALUE, false, "");
            if (d != null && d.laserLineByName.containsKey(ladderOptions.theoLaserLine)) {
                theo = d.laserLineByName.get(ladderOptions.theoLaserLine);
            }

            for (Long price : levelByPrice.keySet()) {
                if (pricingMode == PricingMode.BPS && theo.isValid()) {
                    double points = (10000.0 * (price - theo.getPrice())) / theo.getPrice();
                    ui.txt(priceKey(price), BASIS_POINT_DECIMAL_FORMAT.format(points));
                } else if (pricingMode == PricingMode.EFP && marketDataForSymbol != null && marketDataForSymbol.priceFormat != null && theo.isValid()) {
                    double efp = marketDataForSymbol.priceFormat.toBigDecimal(new NormalizedPrice(price - theo.getPrice())).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
                    ui.txt(priceKey(price), EFP_DECIMAL_FORMAT.format(efp));
                } else {
                    drawPrice(marketDataForSymbol, price, priceKey(price));
                }
            }
        }
    }

    private void decorateUpDown(final String key, Long value) {
        if (value == null) return;
        ui.cls(key, Html.POSITIVE, value > 0);
        ui.cls(key, Html.NEGATIVE, value < 0);
    }

    private String stripPriceZeros(MarketDataForSymbol m, Long price) {
        return price == null ? "" : m.priceFormat.toBigDecimal(m.priceFormat.protocolsPriceToNormalized(price)).stripTrailingZeros().toPlainString();
    }

    private Long getLastTradeChangeOnDay(MarketDataForSymbol m) {
        if (m.lastTrade == null) {
            return null;
        }
        if (m.settle == null) {
            return null;
        }
        long lastTradePrice = m.lastTrade.getPrice();
        long settlementPrice = m.settle.getSettlementPrice();
        return lastTradePrice - settlementPrice;
    }

    private void drawWorkingOrders() {
        WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null) {
            for (Long price : levelByPrice.keySet()) {
                int totalQty = 0;
                String side = "";
                List<String> keys = new ArrayList<String>();
                Set<WorkingOrderType> orderTypes = new HashSet<WorkingOrderType>();
                for (Main.WorkingOrderUpdateFromServer orderFromServer : w.ordersByPrice.get(price)) {
                    WorkingOrderUpdate order = orderFromServer.value;
                    int orderQty = order.getTotalQuantity() - order.getFilledQuantity();
                    totalQty += orderQty;
                    side = order.getSide().toString().toLowerCase();
                    keys.add(orderFromServer.key());
                    if (orderQty > 0) {
                        orderTypes.add(orderFromServer.value.getWorkingOrderType());
                    }
                }
                workingQty(price, totalQty, side, orderTypes);
                ui.data(orderKey(price), "orderKeys", Joiner.on('!').join(keys));
            }

            int buyQty = 0;
            int sellQty = 0;

            for (Main.WorkingOrderUpdateFromServer orderUpdateFromServer : w.ordersByKey.values()) {
                if (orderUpdateFromServer.value.getWorkingOrderState() != WorkingOrderState.DEAD) {
                    int remainingQty = orderUpdateFromServer.value.getTotalQuantity() - orderUpdateFromServer.value.getFilledQuantity();
                    if (orderUpdateFromServer.value.getSide() == com.drwtrading.london.protocols.photon.execution.Side.BID) {
                        buyQty += remainingQty;
                    } else if (orderUpdateFromServer.value.getSide() == com.drwtrading.london.protocols.photon.execution.Side.OFFER) {
                        sellQty += remainingQty;
                    }
                }
            }

            ui.cls(Html.BUY_QTY, Html.HIDDEN, buyQty == 0);
            ui.txt(Html.BUY_QTY, buyQty);

            ui.cls(Html.SELL_QTY, Html.HIDDEN, sellQty == 0);
            ui.txt(Html.SELL_QTY, sellQty);

        }
    }

    private void drawLastTrade() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.tradeTracker != null && m.tradeTracker.quantityTraded != 0) {
            for (Long price : levelByPrice.keySet()) {
                if (price != m.tradeTracker.lastTrade.getPrice()) {
                    ui.txt(tradeKey(price), Html.BLANK);
                    ui.cls(tradeKey(price), Html.HIDDEN, true);
                    ui.cls(volumeKey(price), Html.HIDDEN, false);
                    ui.cls(tradeKey(price), Html.TRADED_UP, false);
                    ui.cls(tradeKey(price), Html.TRADED_DOWN, false);
                    ui.cls(tradeKey(price), Html.TRADED_AGAIN, false);
                } else {
                    ui.txt(tradeKey(price), m.tradeTracker.quantityTraded);
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
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.totalTradedVolumeByPrice != null) {

            Long minTradedPrice = m.totalTradedVolumeByPrice.isEmpty() ? null : Collections.min(m.totalTradedVolumeByPrice.keySet());
            Long maxTradedPrice = m.totalTradedVolumeByPrice.isEmpty() ? null : Collections.max(m.totalTradedVolumeByPrice.keySet());

            for (Long price : levelByPrice.keySet()) {
                TotalTradedVolumeByPrice volumeByPrice = m.totalTradedVolumeByPrice.get(price);
                if (volumeByPrice != null) {
                    ui.txt(volumeKey(price), volumeByPrice.getQuantityTraded());
                    ui.cls(priceKey(price), Html.PRICE_TRADED, volumeByPrice.getQuantityTraded() > 0);
                } else {
                    ui.txt(volumeKey(price), Html.EMPTY);
                    ui.cls(priceKey(price), Html.PRICE_TRADED, false);
                }
            }

            for (Long price : levelByPrice.keySet()) {
                boolean withinTradedRange = minTradedPrice != null && maxTradedPrice != null && price >= minTradedPrice && price <= maxTradedPrice;
                ui.cls(priceKey(price), Html.PRICE_TRADED, withinTradedRange);
            }

            int quantityTraded = 0;

            if (m.totalTradedVolume != null) {
                quantityTraded = m.totalTradedVolume.getQuantityTraded();
            } else {
                for (TotalTradedVolumeByPrice totalTradedVolumeByPrice : m.totalTradedVolumeByPrice.values()) {
                    quantityTraded += totalTradedVolumeByPrice.getQuantityTraded();
                }
            }

            if (quantityTraded > 1000000) {
                ui.txt(Html.TOTAL_TRADED_VOLUME, BASIS_POINT_DECIMAL_FORMAT.format(1.0 / 1000000 * quantityTraded) + "M");
            } else if (quantityTraded > 1000) {
                ui.txt(Html.TOTAL_TRADED_VOLUME, BASIS_POINT_DECIMAL_FORMAT.format(1.0 / 1000 * quantityTraded) + "K");
            } else {
                ui.txt(Html.TOTAL_TRADED_VOLUME, quantityTraded);
            }

        }
    }

    private void drawBook() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.book != null) {
            ui.txt(Html.SYMBOL, getSymbol());
            ui.title(getSymbol());
            for (Long price : levelByPrice.keySet()) {
                if (m.topOfBook != null && m.topOfBook.getBestBid().isExists() && m.topOfBook.getBestBid().getPrice() == price) {
                    bidQty(price, m.topOfBook.getBestBid().getQuantity());
                } else if (m.bookState != null && m.bookState.getState() == BookState.AUCTION && m.auctionIndicativePrice != null && m.auctionIndicativePrice.isHasIndicativePrice() && m.auctionIndicativePrice.getIndicativePrice() == price) {
                    bidQty(price, m.auctionIndicativePrice.getQuantity());
                } else {
                    bidQty(price, m.book.getLevel(price, Side.BID).getQuantity());
                }
                if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists() && m.topOfBook.getBestOffer().getPrice() == price) {
                    offerQty(price, m.topOfBook.getBestOffer().getQuantity());
                } else if (m.bookState != null && m.bookState.getState() == BookState.AUCTION && m.auctionIndicativePrice != null && m.auctionIndicativePrice.isHasIndicativePrice() && m.auctionIndicativePrice.getIndicativePrice() == price) {
                    offerQty(price, m.auctionIndicativePrice.getQuantity());
                } else {
                    offerQty(price, m.book.getLevel(price, Side.OFFER).getQuantity());
                }
            }
        }
    }

    private void drawLadder() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (m != null && m.refData != null) {
            ui.clear();
            pendingRefDataAndSettle = false;
            recenter();
            recenterLadderAndDrawPriceLevels();
            setUpClickTrading();
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
        if (dataForSymbol != null && dataForSymbol.displaySymbol != null && !symbol.contains(dataForSymbol.displaySymbol)) {
            return symbol + " (" + dataForSymbol.displaySymbol + ")";
        }
        return symbol;
    }

    private void setUpClickTrading() {
        boolean clickTradingEnabled = ladderOptions.traders.contains(client.getUserName());

        view.trading(clickTradingEnabled, ladderOptions.orderTypesLeft, ladderOptions.orderTypesRight);

        for (Map.Entry<String, Integer> entry : Html.BUTTON_QTY.entrySet()) {
            ui.txt(entry.getKey(), entry.getValue() < 1000 ? entry.getValue() : entry.getValue() / 1000 + "K");
        }
        for (int i = 0; i < levels; i++) {
            ui.clickable("#" + Html.PRICE + i);
            ui.clickable("#" + Html.BID + i);
            ui.clickable("#" + Html.OFFER + i);
            ui.clickable("#" + Html.ORDER + i);
        }
        ui.clickable(Html.BUTTONS);
        ui.scrollable("#" + Html.LADDER);
        ui.clickable("#" + Html.BUY_QTY);
        ui.clickable("#" + Html.SELL_QTY);

        ui.clickable("#" + Html.BUY_OFFSET_UP);
        ui.clickable("#" + Html.BUY_OFFSET_DOWN);
        ui.clickable("#" + Html.SELL_OFFSET_UP);
        ui.clickable("#" + Html.SELL_OFFSET_DOWN);

        ui.clickable("#" + Html.START_BUY);
        ui.clickable("#" + Html.STOP_BUY);
        ui.clickable("#" + Html.START_SELL);
        ui.clickable("#" + Html.STOP_SELL);

        ui.cls(Html.OFFSET_CONTROL, Html.HIDDEN, false);

    }

    public void recenterIfTimeoutElapsed() {
        MarketDataForSymbol m = marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null) {
            if (getCenterPrice(m) >= bottomPrice && getCenterPrice(m) <= topPrice) {
                resetLastCenteredTime();
            } else if (System.currentTimeMillis() - lastCenteredTime > RECENTER_TIME_MS) {
                recenter();
                recenterLadderAndDrawPriceLevels();
                updateEverything();
            }
            ui.cls(Html.LADDER, Html.RECENTERING, lastCenteredTime > 0 && RECENTER_WARN_TIME_MS <= System.currentTimeMillis() - lastCenteredTime);
        }
    }

    private void moveLadderTowardCenter() {
        int direction = (int) Math.signum(getCenterPrice(marketDataForSymbol) - centerPrice);
        centerPrice = getPriceNTicksFrom(centerPrice, AUTO_RECENTER_TICKS * direction);
    }

    private void resetLastCenteredTime() {
        lastCenteredTime = System.currentTimeMillis();
    }

    private long getCenterPrice(MarketDataForSymbol m) {
        if (!pendingRefDataAndSettle) {
            long center = 0;
            if (m.settle != null) {
                center = m.settle.getSettlementPrice();
            }
            if (m.lastTrade != null) {
                center = m.lastTrade.getPrice();
            }
            if (m.auctionIndicativePrice != null && m.auctionIndicativePrice.isHasIndicativePrice() && m.auctionIndicativePrice.getQuantity() > 0) {
                center = m.auctionIndicativePrice.getIndicativePrice();
            }
            if (m.auctionTradeUpdate != null && m.auctionTradeUpdate.getQuantity() > 0) {
                center = m.auctionTradeUpdate.getPrice();
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
            long roundedCenter = m.priceOperations.tradeablePrice(center, Side.BID);
            return roundedCenter;
        }
        return 0;
    }

    private long getMidPrice(MarketDataForSymbol m) {
        long midPrice = m.topOfBook.getBestBid().getPrice() / 2 + m.topOfBook.getBestOffer().getPrice() / 2;
        return m.priceOperations.tradeablePrice(midPrice, Side.BID);
    }

    private void recenterLadderAndDrawPriceLevels() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (m != null && m.refData != null) {

            int centerLevel = levels / 2;
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

    private void drawPrice(MarketDataForSymbol m, long price, final String key) {
        ui.txt(key, m.priceFormat.format(price));
    }

    // Inbound

    public void onRawInboundData(final String data) {
        ui.onInbound(data);
    }

    @Override
    public void onUpdate(String label, Map<String, String> dataArg) {
        String value = dataArg.get("value");
        if (value != null) {
            if (Html.INP_QTY.equals(label)) {
                clickTradingBoxQty = Integer.valueOf(value);
            } else if (persistentPrefs.contains(label)) {
                ladderPrefsForSymbolUser.set(label, value);
            } else {
                throw new IllegalArgumentException("Update for unknown value: " + label + " " + dataArg);
            }
        }
        drawClickTrading();
        flush();

    }

    @Override
    public void onScroll(final String direction) {
        if (direction.equals("up")) {
            centerPrice = marketDataForSymbol.priceOperations.nTicksAway(centerPrice, 1, PriceUtils.Direction.Add);
        } else if (direction.equals("down")) {
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
            int n = levelByPrice.size() - 1;
            centerPrice = getPriceNTicksFrom(centerPrice, n);
        } else if (keyCode == PG_DOWN) {
            int n = -1 * (levelByPrice.size() - 1);
            centerPrice = getPriceNTicksFrom(centerPrice, n);
        } else {
            return;
        }
        resetLastCenteredTime();
        recenterLadderAndDrawPriceLevels();
        updateEverything();
        flush();
    }

    private long getPriceNTicksFrom(final long price, final int n) {
        return marketDataForSymbol.priceOperations.nTicksAway(price, Math.abs(n), n >= 0 ? PriceUtils.Direction.Add : PriceUtils.Direction.Subtract);
    }

    @Override
    public void onIncoming(final String[] args) {
        String cmd = args[0];
        if (cmd.equals("heartbeat")) {
            onHeartbeat(Long.valueOf(args[1]), Long.valueOf(args[2]));
        }
    }


    @Override
    public void onDblClick(String label, Map<String, String> dataArg) {
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
    public void onClick(String label, Map<String, String> data) {
        String button = data.get("button");
        LadderPrefsForSymbolUser l = ladderPrefsForSymbolUser;
        boolean autoHedge = "true".equals(getPref(l, Html.AUTO_HEDGE_LEFT));
        if ("left".equals(button)) {
            if (Html.BUTTON_QTY.containsKey(label)) {
                clickTradingBoxQty += Html.BUTTON_QTY.get(label);
            } else if (Html.BUTTON_CLR.equals(label)) {
                clickTradingBoxQty = 0;
            } else if (label.startsWith(Html.BID) || label.startsWith(Html.OFFER)) {
                if (l != null) {
                    submitOrderClick(label, data, getPref(l, Html.ORDER_TYPE_LEFT), autoHedge);
                }
            } else if (label.startsWith(Html.ORDER)) {
                long price = Long.valueOf(data.get("price"));
                if (label.equals(orderKey(price))) {
                    cancelWorkingOrders(price, data);
                } else {
                    System.out.println("Mismatched label: " + data.get("price") + " " + orderKey(price) + " " + label);
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
                pricingMode = PricingMode.BPS;
            } else if (label.equals(Html.PRICING_RAW)) {
                pricingMode = PricingMode.RAW;
            } else if (label.equals(Html.PRICING_EFP)) {
                pricingMode = PricingMode.EFP;
            }
        } else if ("right".equals(button)) {
            if (label.startsWith(Html.BID) || label.startsWith(Html.OFFER)) {
                if (l != null) {
                    submitOrderClick(label, data, getPref(l, Html.ORDER_TYPE_RIGHT), "true".equals(getPref(l, Html.AUTO_HEDGE_RIGHT)));
                }
            } else if (label.startsWith(Html.ORDER)) {
                rightClickModify(data, autoHedge);
            }
        } else if ("middle".equals(button)) {
            if (label.startsWith(Html.PRICE)) {
                moveLadderToCenter();
                resetLastCenteredTime();
                recenterLadderAndDrawPriceLevels();
                updateEverything();
            }
        }
        updateEverything();
        drawClickTrading();
        flush();
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

        public HeartbeatRoundtrip(String userName, String symbol, long sentTimeMillis, long returnTimeMillis, long roundtripMillis, long seqNo) {
            this.userName = userName;
            this.symbol = symbol;
            this.sentTimeMillis = sentTimeMillis;
            this.returnTimeMillis = returnTimeMillis;
            this.roundtripMillis = roundtripMillis;
        }
    }

    private Map<String, String> getDataArg(String[] args) {
        Map<String, String> data = new HashMap<String, String>();
        int arg = 2;
        if (args.length > arg) {
            String[] unpacked = args[arg].split("\2");
            for (int i = 0; i < unpacked.length - 1; i += 2) {
                data.put(unpacked[i], unpacked[i + 1]);
            }
        }
        return data;
    }

    // Click-trading

    private int clickTradingBoxQty = 0;
    private int orderSeqNo = 0;

    public static final Set<String> persistentPrefs = newFastSet();

    static {
        persistentPrefs.add(Html.INP_RELOAD);
        persistentPrefs.add(Html.AUTO_HEDGE_LEFT);
        persistentPrefs.add(Html.AUTO_HEDGE_RIGHT);
        persistentPrefs.add(Html.ORDER_TYPE_LEFT);
        persistentPrefs.add(Html.ORDER_TYPE_RIGHT);
        persistentPrefs.add(Html.RANDOM_RELOAD);
    }

    public static final Map<String, String> defaultPrefs = newFastMap();

    static {
        defaultPrefs.put(Html.INP_RELOAD, "50");
        defaultPrefs.put(Html.AUTO_HEDGE_LEFT, "true");
        defaultPrefs.put(Html.AUTO_HEDGE_RIGHT, "true");
        defaultPrefs.put(Html.ORDER_TYPE_LEFT, "HAWK");
        defaultPrefs.put(Html.ORDER_TYPE_RIGHT, "MANUAL");
        defaultPrefs.put(Html.RANDOM_RELOAD, "true");
    }

    public void drawClickTrading() {
        LadderPrefsForSymbolUser l = ladderPrefsForSymbolUser;
        if (!pendingRefDataAndSettle && l != null) {

            ui.txt(Html.INP_QTY, clickTradingBoxQty);

            for (String pref : persistentPrefs) {
                ui.txt(pref, getPref(ladderPrefsForSymbolUser, pref));
            }

            for (String type : ladderOptions.orderTypesLeft) {
                ui.cls(Html.ORDER_TYPE_LEFT, type, type.equals(getPref(l, Html.ORDER_TYPE_LEFT)));
            }

            for (String type : ladderOptions.orderTypesRight) {
                ui.cls(Html.ORDER_TYPE_RIGHT, type, type.equals(getPref(l, Html.ORDER_TYPE_RIGHT)));
            }

            for (Long price : levelByPrice.keySet()) {
                ui.cls(orderKey(price), Html.MODIFY_PRICE_SELECTED, price.equals(modifyFromPrice));
            }

        }

        if (dataForSymbol != null) {
            ui.cls(Html.OFFSET_CONTROL, Html.HIDDEN, !dataForSymbol.symbolAvailable);
        }
    }

    private String getPref(LadderPrefsForSymbolUser l, String id) {
        return l.get(id, defaultPrefs.get(id));
    }

    private void cancelAllForSide(com.drwtrading.london.protocols.photon.execution.Side side) {
        for (Main.WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
            if (orderUpdateFromServer.value.getSide() == side) {
                cancelOrder(orderUpdateFromServer);
            }
        }
    }

    Long modifyFromPrice = null;
    Long modifyFromPriceSelectedTime = 0L;

    private void rightClickModify(final Map<String, String> data, final boolean autoHedge) {
        if (workingOrdersForSymbol != null) {
            final long price = Long.valueOf(data.get("price"));
            if (modifyFromPrice != null) {
                if (workingOrdersForSymbol != null && modifyFromPrice != price) {
                    for (Main.WorkingOrderUpdateFromServer order : workingOrdersForSymbol.ordersByPrice.get(modifyFromPrice)) {
                        WorkingOrderUpdate workingOrderUpdate = order.value;
                        modifyOrder(autoHedge, price, order, workingOrderUpdate);
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
        if (modifyFromPrice != null && modifyFromPriceSelectedTime != null && System.currentTimeMillis() > modifyFromPriceSelectedTime + MODIFY_TIMEOUT_MS) {
            modifyFromPrice = null;
        }
    }

    private RemoteOrder getRemoteOrderFromWorkingOrder(final boolean autoHedge, final long price, final WorkingOrderUpdate workingOrderUpdate) {
        RemoteOrderType remoteOrderType = getRemoteOrderType(workingOrderUpdate.getWorkingOrderType().toString());
        System.out.println(workingOrderUpdate + "->" + remoteOrderType);
        return new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), price, workingOrderUpdate.getTotalQuantity(), remoteOrderType, autoHedge, workingOrderUpdate.getTag());
    }

    private void submitOrderClick(String label, Map<String, String> data, String orderType, boolean autoHedge) {
        long price = Long.valueOf(data.get("price"));
        com.drwtrading.london.protocols.photon.execution.Side side = label.equals(bidKey(price))
                ? com.drwtrading.london.protocols.photon.execution.Side.BID
                : label.equals(offerKey(price))
                ? com.drwtrading.london.protocols.photon.execution.Side.OFFER
                : null;

        if (side == null) {
            throw new IllegalArgumentException("Price " + price + " did not match key " + label);
        }

        if (orderType != null && clickTradingBoxQty > 0) {
            submitOrder(orderType, autoHedge, price, side);
        }

        boolean randomReload = "true".equals(getPref(ladderPrefsForSymbolUser, Html.RANDOM_RELOAD));
        int reloadBoxQty = Integer.valueOf(getPref(ladderPrefsForSymbolUser, Html.INP_RELOAD));

        if (randomReload) {
            clickTradingBoxQty = Math.max(0, reloadBoxQty - (int) (Math.random() * ladderOptions.randomReloadFraction * reloadBoxQty));
        } else {
            clickTradingBoxQty = Math.max(0, reloadBoxQty);
        }
    }

    private void submitOrder(String orderType, boolean autoHedge, long price, com.drwtrading.london.protocols.photon.execution.Side side) {
        if (ladderOptions.traders.contains(client.getUserName())) {

            if (clientSpeedState == ClientSpeedState.TooSlow) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, "Cannot submit order " + side + " " + clickTradingBoxQty + " for " + symbol + ", client " + client.getUserName() + " is " + clientSpeedState.toString() + " speed: " + getClientSpeedMillis() + "ms"));
                return;
            }

            RemoteOrderType remoteOrderType = getRemoteOrderType(orderType);
            String serverName = ladderOptions.serverResolver.resolveToServerName(symbol, remoteOrderType);

            System.out.println("Order: " + symbol + " " + remoteOrderType.toString() + " resolved to " + serverName);

            TradingStatusWatchdog.ServerTradingStatus serverTradingStatus = tradingStatusForAll.serverTradingStatusMap.get(serverName);

            if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, "Cannot submit order " + side + " " + clickTradingBoxQty + " for " + symbol + ", server " + serverName + " has status " + (serverTradingStatus == null ? null : serverTradingStatus.toString())));
                return;
            }

            RemoteSubmitOrder remoteSubmitOrder = new RemoteSubmitOrder(
                    serverName, client.getUserName(), orderSeqNo++, new RemoteOrder(
                    symbol, side, price, clickTradingBoxQty, remoteOrderType, autoHedge, ladderOptions.tag));

            remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(serverName, remoteSubmitOrder));

        }
    }

    private void modifyOrder(final boolean autoHedge, final long price, final Main.WorkingOrderUpdateFromServer order, final WorkingOrderUpdate workingOrderUpdate) {
        RemoteModifyOrder remoteModifyOrder = new RemoteModifyOrder(
                order.fromServer, client.getUserName(), workingOrderUpdate.getChainId(),
                getRemoteOrderFromWorkingOrder(autoHedge, workingOrderUpdate.getPrice(), workingOrderUpdate),
                getRemoteOrderFromWorkingOrder(autoHedge, price, workingOrderUpdate)
        );
        if (ladderOptions.traders.contains(client.getUserName())) {

            TradingStatusWatchdog.ServerTradingStatus serverTradingStatus = tradingStatusForAll.serverTradingStatusMap.get(order.fromServer);
            if (clientSpeedState == ClientSpeedState.TooSlow) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, "Cannot modify order , client " + client.getUserName() + " is " + clientSpeedState.toString() + " speed: " + getClientSpeedMillis() + "ms"));
            } else if (serverTradingStatus == null || serverTradingStatus.tradingStatus != TradingStatusWatchdog.Status.OK) {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, "Cannot modify order: server " + order.fromServer + " has status " + (serverTradingStatus == null ? null : serverTradingStatus.toString())));
            } else {
                remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(order.fromServer, remoteModifyOrder));
            }

        }
    }

    private void cancelWorkingOrders(Long price, Map<String, String> data) {
        WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null) {
            for (Main.WorkingOrderUpdateFromServer orderUpdateFromServer : w.ordersByPrice.get(price)) {
                cancelOrder(orderUpdateFromServer);
            }
        }
    }

    private void cancelOrder(Main.WorkingOrderUpdateFromServer orderUpdateFromServer) {
        if (ladderOptions.traders.contains(client.getUserName())) {
            WorkingOrderUpdate workingOrderUpdate = orderUpdateFromServer.value;
            String orderType = getOrderType(workingOrderUpdate.getWorkingOrderType());
            RemoteOrder order = new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), workingOrderUpdate.getPrice(), workingOrderUpdate.getTotalQuantity(), getRemoteOrderType(orderType), false, workingOrderUpdate.getTag());
            remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(orderUpdateFromServer.fromServer, new RemoteCancelOrder(workingOrderUpdate.getServerName(), client.getUserName(), workingOrderUpdate.getChainId(), order)));
        }
    }

    private RemoteOrderType getRemoteOrderType(String orderType) {
        for (RemoteOrderType remoteOrderType : RemoteOrderType.values()) {
            if (remoteOrderType.toString().toUpperCase().equals(orderType.toUpperCase())) {
                return remoteOrderType;
            }
        }
        return RemoteOrderType.MANUAL;
    }

    private String getOrderType(final WorkingOrderType workingOrderType) {
        return workingOrderType.toString().replace("MARKET", "MKT_CLOSE");
    }

    // Heartbeats

    public static enum ClientSpeedState {
        TooSlow(1000),
        Slow(500),
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

    private void onHeartbeat(long sentTimeMillis, long seqNo) {
        long returnTimeMillis = System.currentTimeMillis();
        if (lastHeartbeatSentMillis == sentTimeMillis) {
            lastHeartbeatSentMillis = null;
            lastHeartbeatRoundtripMillis = returnTimeMillis - sentTimeMillis;
            heartbeatRoundtripPublisher.publish(new HeartbeatRoundtrip(client.getUserName(), symbol, sentTimeMillis, returnTimeMillis, lastHeartbeatRoundtripMillis, seqNo));
        } else {
            throw new RuntimeException("Received heartbeat reply " + sentTimeMillis + " which does not match last sent heartbeat " + lastHeartbeatSentMillis);
        }
    }

    public long getClientSpeedMillis() {
        return Math.max(lastHeartbeatSentMillis == null ? 0L : System.currentTimeMillis() - lastHeartbeatSentMillis, lastHeartbeatRoundtripMillis);
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

    public void bidQty(long price, int qty) {
        ui.txt(bidKey(price), qty > 0 ? qty : Html.EMPTY);
        ui.cls(bidKey(price), Html.BID_ACTIVE, qty > 0);
    }

    public void offerQty(long price, int qty) {
        ui.txt(offerKey(price), qty > 0 ? qty : Html.EMPTY);
        ui.cls(offerKey(price), Html.OFFER_ACTIVE, qty > 0);
    }

    public void workingQty(long price, int qty, String side, Set<WorkingOrderType> orderTypes) {
        ui.txt(orderKey(price), qty > 0 ? qty : Html.EMPTY);
        ui.cls(orderKey(price), Html.WORKING_QTY, qty > 0);
        ui.cls(orderKey(price), Html.WORKING_BID, side.equals("bid"));
        ui.cls(orderKey(price), Html.WORKING_OFFER, side.equals("offer"));
        for (WorkingOrderType workingOrderType : WorkingOrderType.values()) {
            ui.cls(orderKey(price), Html.WORKING_ORDER_TYPE + getOrderType(workingOrderType).toLowerCase(), orderTypes.contains(workingOrderType));
        }
    }

    private String laserKey(String name) {
        return Html.LASER + name;
    }

    private String bidKey(long price) {
        return Html.BID + levelByPrice.get(price);
    }

    private String offerKey(long price) {
        return Html.OFFER + levelByPrice.get(price);
    }

    private String priceKey(long price) {
        return Html.PRICE + levelByPrice.get(price);
    }

    private String orderKey(long price) {
        return Html.ORDER + levelByPrice.get(price);
    }

    private String volumeKey(long price) {
        return Html.VOLUME + levelByPrice.get(price);
    }

    private String tradeKey(long price) {
        return Html.TRADE + levelByPrice.get(price);
    }

}
