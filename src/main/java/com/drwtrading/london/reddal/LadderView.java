package com.drwtrading.london.reddal;

import com.drwtrading.london.fastui.UiPipe;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.drwtrading.london.protocols.photon.execution.RemoteSubmitOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BookConsistencyMarker;
import com.drwtrading.london.protocols.photon.marketdata.BookState;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.protocols.photon.marketdata.TopOfBook;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.websockets.WebSocketClient;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LadderView {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final DecimalFormat BASIS_POINT_DECIMAL_FORMAT = new DecimalFormat(".00");


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
        public static final String HIDDEN = "hidden";
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
    }

    private final WebSocketClient client;
    private final LadderPresenter.View view;
    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher;
    private final LadderOptions ladderOptions;
    private final UiPipe ui;
    private final Publisher<StatsMsg> statsPublisher;

    public String symbol;
    private MarketDataForSymbol marketDataForSymbol;
    private WorkingOrdersForSymbol workingOrdersForSymbol;
    private ExtraDataForSymbol dataForSymbol;
    private int levels;

    Map<Long, Integer> levelByPrice = new HashMap<Long, Integer>();

    private boolean pendingRefDataAndSettle = true;
    private long centerPrice;
    private long topPrice;
    private long bottomPrice;

    public int priceShiftIndex = 2;

    public long lastCenteredTime = 0;

    public LadderView(WebSocketClient client, UiPipe ui, LadderPresenter.View view, Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandToServerPublisher, LadderOptions ladderOptions, Publisher<StatsMsg> statsPublisher) {
        this.client = client;
        this.view = view;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.ladderOptions = ladderOptions;
        this.ui = ui;
        this.statsPublisher = statsPublisher;
    }

    public void subscribeToSymbol(String symbol, int levels, MarketDataForSymbol marketDataForSymbol, WorkingOrdersForSymbol workingOrdersForSymbol, ExtraDataForSymbol extraDataForSymbol) {
        this.symbol = symbol;
        this.marketDataForSymbol = marketDataForSymbol;
        this.workingOrdersForSymbol = workingOrdersForSymbol;
        this.dataForSymbol = extraDataForSymbol;
        this.levels = levels;
        this.pendingRefDataAndSettle = true;
        drawLadder();
        updateEverything();
    }

    private void updateEverything() {
        drawBook();
        drawTradedVolumes();
        drawLastTrade();
        drawWorkingOrders();
        drawMetaData();
        drawClock();
        drawPriceLevels(dataForSymbol);
    }

    public void flush() {
        drawClock();
        checkIfNeedToRecenterAndDoItIfTimeoutElapsed();
        ui.flush();
    }

    // Events

    public void onWorkingOrderUpdate(Main.WorkingOrderUpdateFromServer _) {
        drawWorkingOrders();
    }

    public void onMarketDataEvent(MarketDataEvent marketDataEvent) {
        if (symbol == null || marketDataForSymbol == null) {
            throw new IllegalStateException("Should not be receiving market data events before being subscribed.");
        }
        if (pendingRefDataAndSettle) {
            drawLadder();
        } else if (marketDataEvent instanceof TopOfBook || marketDataEvent instanceof BookConsistencyMarker) {
            drawBook();
        } else if (marketDataEvent instanceof TotalTradedVolumeByPrice) {
            drawTradedVolumes();
        } else if (marketDataEvent instanceof TradeUpdate) {
            drawLastTrade();
        }
    }

    public void onMetadata(LadderMetadata _) {
        drawMetaData();
    }

    public void onDayPosition(Position _) {
        drawMetaData();
    }

    public void onServerTradingStatus(TradingStatusWatchdog.ServerTradingStatus serverTradingStatus) {
        drawWorkingOrders();
    }

    // Drawing

    private void drawClock() {
        ui.txt(Html.CLOCK, SIMPLE_DATE_FORMAT.format(new Date()));
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
            ui.txt(Html.LAST_TRADE_COD, stripPriceZeros(m, lastTradeChangeOnDay));
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
                        long priceAbove = m.tickSizeTracker.priceAbove(price);
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
        // Basis points
        if (d != null && ladderOptions.shiftLaserLines.size() > 0) {
            int laserLineIndex = priceShiftIndex % (ladderOptions.shiftLaserLines.size() + 1);
            if (laserLineIndex != ladderOptions.shiftLaserLines.size()) {
                LaserLine laserLine = d.laserLineByName.get(ladderOptions.shiftLaserLines.get(laserLineIndex));
                if (laserLine != null && laserLine.isValid() && laserLine.getPrice() > 0) {
                    for (Long price : levelByPrice.keySet()) {
                        double points = (10000.0 * (price - laserLine.getPrice())) / laserLine.getPrice();
                        ui.txt(priceKey(price), BASIS_POINT_DECIMAL_FORMAT.format(points));
                    }
                    return;
                }
            }
        }
        if (marketDataForSymbol != null) {
            for (Long price : levelByPrice.keySet()) {
                drawPrice(marketDataForSymbol, price);
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
        long aMlastTradePrice = m.lastTrade.getPrice();
        long settlementPrice = m.settle.getSettlementPrice();
        return aMlastTradePrice - settlementPrice;
    }

    private void drawWorkingOrders() {
        WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null) {
            for (Long price : levelByPrice.keySet()) {
                int totalQty = 0;
                String side = "";
                List<String> keys = new ArrayList<String>();
                for (Main.WorkingOrderUpdateFromServer orderFromServer : w.ordersByPrice.get(price)) {
                    WorkingOrderUpdate order = orderFromServer.value;
                    totalQty += order.getTotalQuantity() - order.getFilledQuantity();
                    side = order.getSide().toString().toLowerCase();
                    keys.add(orderFromServer.key());
                }
                workingQty(price, totalQty, side);
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
        if (m != null && m.refData != null && m.settle != null) {
            ui.clear();

            ui.txt("symbol", symbol);

            pendingRefDataAndSettle = false;
            centerPrice = getCenterPrice(m);

            boolean clickTradingEnabled = ladderOptions.traders.contains(client.getUserName());

            view.draw(levels);
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

            recenterLadderAndDrawPriceLevels();
            ui.flush();
        }
    }

    public void checkIfNeedToRecenterAndDoItIfTimeoutElapsed() {
        MarketDataForSymbol m = marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null) {
            long now = System.currentTimeMillis();
            long center = getCenterPrice(m);
            if (center >= bottomPrice && center <= topPrice) {
                lastCenteredTime = now;
            } else if (now - lastCenteredTime > 5000) {
                drawLadder();
                updateEverything();
            }
            ui.cls(Html.LADDER, Html.RECENTERING, lastCenteredTime > 0 && 4000 <= now - lastCenteredTime);
        }
    }

    private long getCenterPrice(MarketDataForSymbol m) {
        long center;
        center = m.settle.getSettlementPrice();
        if (m.lastTrade != null) {
            center = m.lastTrade.getPrice();
        }
        if (m.auctionIndicativePrice != null) {
            center = m.auctionIndicativePrice.getIndicativePrice();
        }
        if (m.auctionTradeUpdate != null) {
            center = m.auctionTradeUpdate.getPrice();
        }
        if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists()) {
            center = m.topOfBook.getBestOffer().getPrice();
        }
        if (m.topOfBook != null && m.topOfBook.getBestBid().isExists()) {
            center = m.topOfBook.getBestBid().getPrice();
        }
        if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists() && m.topOfBook.getBestBid().isExists()) {
            center = getMidPrice(m);
        }
        return center;
    }

    private long getMidPrice(MarketDataForSymbol m) {
        int levels = 0;

        long price = m.topOfBook.getBestBid().getPrice();
        while (price < m.topOfBook.getBestOffer().getPrice()) {
            price = m.tickSizeTracker.priceAbove(price);
            levels++;
        }

        levels /= 2;
        price = m.topOfBook.getBestBid().getPrice();
        for (int i = 0; i < levels; i++) {
            price = m.tickSizeTracker.priceAbove(price);
        }
        return price;
    }

    private void recenterLadderAndDrawPriceLevels() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (m != null && m.refData != null && m.settle != null) {

            int centerLevel = levels / 2;
            long price = centerPrice;

            for (int i = 0; i < centerLevel; i++) {
                price = m.tickSizeTracker.priceAbove(price);
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
                price = m.tickSizeTracker.priceBelow(price);
            }

            drawPriceLevels(dataForSymbol);
        }
    }

    private void drawPrice(MarketDataForSymbol m, long price) {
        ui.txt(priceKey(price), m.priceFormat.format(price));
    }

    // Inbound

    public void onInbound(String[] args) {
        String cmd = args[0];
        if (cmd.equals("click")) {
            String label = args[1];
            onClick(label, getDataArg(args));
        } else if (cmd.equals("scroll")) {
            String direction = args[1];
            if (direction.equals("up")) {
                centerPrice = marketDataForSymbol.tickSizeTracker.priceAbove(centerPrice);
            } else if (direction.equals("down")) {
                centerPrice = marketDataForSymbol.tickSizeTracker.priceBelow(centerPrice);
            }
            recenterLadderAndDrawPriceLevels();
            updateEverything();
        } else if (cmd.equals("dblclick")) {
            String label = args[1];
            onDoubleClick(label, getDataArg(args));
        } else if (cmd.equals("update")) {
            onUpdate(args[1], getDataArg(args));
        } else {
            throw new IllegalStateException("Unknown incoming: " + Arrays.asList(args));
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
    private int reloadBoxQty = 0;
    private boolean autoHedgeLeft = true;
    private boolean autoHedgeRight = true;
    private boolean randomReload = false;
    private String orderTypeLeft = "";
    private String orderTypeRight = "";
    private int orderSeqNo = 0;

    public void drawClickTrading() {
        if (!pendingRefDataAndSettle) {
            ui.txt(Html.INP_QTY, clickTradingBoxQty);
            ui.txt(Html.INP_RELOAD, reloadBoxQty);
            ui.txt(Html.AUTO_HEDGE_LEFT, autoHedgeLeft);
            ui.txt(Html.AUTO_HEDGE_RIGHT, autoHedgeRight);
            ui.txt(Html.ORDER_TYPE_LEFT, orderTypeLeft);
            ui.txt(Html.ORDER_TYPE_RIGHT, orderTypeRight);
            for (String type : ladderOptions.orderTypesLeft) {
                ui.cls(Html.ORDER_TYPE_LEFT, type, type.equals(orderTypeLeft));
            }
            for (String type : ladderOptions.orderTypesRight) {
                ui.cls(Html.ORDER_TYPE_RIGHT, type, type.equals(orderTypeRight));
            }

        }
    }

    private void onUpdate(String label, Map<String, String> dataArg) {
        String value = dataArg.get("value");
        if (value != null) {
            if (Html.INP_RELOAD.equals(label)) {
                reloadBoxQty = Integer.valueOf(value);
            } else if (Html.INP_QTY.equals(label)) {
                clickTradingBoxQty = Integer.valueOf(value);
            } else if (Html.AUTO_HEDGE_LEFT.equals(label)) {
                autoHedgeLeft = "true".equals(value);
            } else if (Html.AUTO_HEDGE_RIGHT.equals(label)) {
                autoHedgeRight = "true".equals(value);
            } else if (Html.ORDER_TYPE_LEFT.equals(label)) {
                orderTypeLeft = value;
            } else if (Html.ORDER_TYPE_RIGHT.equals(label)) {
                orderTypeRight = value;
            } else if (Html.RANDOM_RELOAD.equals(label)) {
                randomReload = "true".equals(value);
            } else {
                throw new IllegalArgumentException("Update for unknown value: " + label + " " + dataArg);
            }
        }
        drawClickTrading();
    }

    public void onDoubleClick(String label, Map<String, String> dataArg) {
        if (workingOrdersForSymbol != null) {
            if (Html.BUY_QTY.equals(label)) {
                cancelAllForSide(com.drwtrading.london.protocols.photon.execution.Side.BID);
            } else if (Html.SELL_QTY.equals(label)) {
                cancelAllForSide(com.drwtrading.london.protocols.photon.execution.Side.OFFER);
            }
        }
    }

    private void cancelAllForSide(com.drwtrading.london.protocols.photon.execution.Side side) {
        for (Main.WorkingOrderUpdateFromServer orderUpdateFromServer : workingOrdersForSymbol.ordersByKey.values()) {
            if (orderUpdateFromServer.value.getSide() == side) {
                cancelOrder(orderUpdateFromServer);
            }
        }
    }

    private void onClick(String label, Map<String, String> data) {
        String button = data.get("button");
        if ("left".equals(button)) {
            if (Html.BUTTON_QTY.containsKey(label)) {
                clickTradingBoxQty += Html.BUTTON_QTY.get(label);
            } else if (Html.BUTTON_CLR.equals(label)) {
                clickTradingBoxQty = 0;
            } else if (label.startsWith(Html.BID) || label.startsWith(Html.OFFER)) {
                submitOrderClick(label, data, orderTypeLeft, autoHedgeLeft);
            } else if (label.startsWith(Html.ORDER)) {
                long price = Long.valueOf(data.get("price"));
                if (label.equals(orderKey(price))) {
                    cancelWorkingOrders(price, data);
                } else {
                    System.out.println("Mismatched label: " + data.get("price") + " " + orderKey(price) + " " + label);
                }
            } else if (label.startsWith(Html.PRICE)) {
                priceShiftIndex += 1;
            }
        } else if ("right".equals(button)) {
            if (label.startsWith(Html.BID) || label.startsWith(Html.OFFER)) {
                submitOrderClick(label, data, orderTypeRight, autoHedgeRight);
            }
        } else if ("middle".equals(button)) {
            if (label.startsWith(Html.PRICE)) {
                drawLadder();
            }
        }

        updateEverything();
        drawClickTrading();
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

        if (randomReload) {
            clickTradingBoxQty = Math.max(0, reloadBoxQty - (int) (Math.random() * ladderOptions.randomReloadFraction * reloadBoxQty));
        } else {
            clickTradingBoxQty = Math.max(0, reloadBoxQty);
        }
    }

    private void submitOrder(String orderType, boolean autoHedge, long price, com.drwtrading.london.protocols.photon.execution.Side side) {
        if (ladderOptions.traders.contains(client.getUserName())) {
            RemoteOrderType remoteOrderType = RemoteOrderType.valueOf(orderType);
            String serverName = ladderOptions.serverResolver.resolveToServerName(symbol, remoteOrderType);
            TradingStatusWatchdog.ServerTradingStatus serverTradingStatus = workingOrdersForSymbol.tradingStatusByServer.get(serverName);
            if (serverTradingStatus != null && serverTradingStatus.tradingStatus == Main.Status.OK) {
                RemoteSubmitOrder remoteSubmitOrder = new RemoteSubmitOrder(
                        serverName, client.getUserName(), orderSeqNo++, new RemoteOrder(
                        symbol, side, price, clickTradingBoxQty, remoteOrderType, autoHedge, ladderOptions.tag));
                remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(serverName, remoteSubmitOrder));
            } else {
                statsPublisher.publish(new AdvisoryStat("Click-trading", AdvisoryStat.Level.WARNING, "Cannot submit order " + side + " " + clickTradingBoxQty + " for " + symbol + ", server " + serverName + " ses " + (serverTradingStatus == null ? null : serverTradingStatus.toString())));
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
            remoteOrderCommandToServerPublisher.publish(new Main.RemoteOrderCommandToServer(orderUpdateFromServer.fromServer, new RemoteCancelOrder(workingOrderUpdate.getServerName(), client.getUserName(), workingOrderUpdate.getChainId(), new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), workingOrderUpdate.getPrice(), workingOrderUpdate.getTotalQuantity(), RemoteOrderType.MANUAL, false, workingOrderUpdate.getTag()))));
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

    public void workingQty(long price, int qty, String side) {
        ui.txt(orderKey(price), qty > 0 ? qty : Html.EMPTY);
        ui.cls(orderKey(price), Html.WORKING_QTY, qty > 0);
        ui.cls(orderKey(price), Html.WORKING_BID, side.equals("bid"));
        ui.cls(orderKey(price), Html.WORKING_OFFER, side.equals("offer"));
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
