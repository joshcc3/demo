package com.drwtrading.london.reddal;

import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.drwtrading.london.protocols.photon.execution.RemoteSubmitOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BookConsistencyMarker;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.protocols.photon.marketdata.TopOfBook;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.util.KeyedPublisher;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LadderView {

    public static class Html {
        public static final String EMPTY = " ";

        // Key prefixes
        public static final String BID = "bid_";
        public static final String OFFER = "offer_";
        public static final String PRICE = "price_";
        public static final String ORDER = "order_";
        public static final String TRADE = "trade_";
        public static final String VOLUME = "volume_";

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
        public static final String BLANK = " ";

        // Click-trading
        public static final Map<String, Integer> BUTTON_QTY = new HashMap<String, Integer>();

        static {
            BUTTON_QTY.put("btn_qty_1", 1);
            BUTTON_QTY.put("btn_qty_5", 5);
            BUTTON_QTY.put("btn_qty_10", 10);
            BUTTON_QTY.put("btn_qty_20", 20);
            BUTTON_QTY.put("btn_qty_100", 100);
            BUTTON_QTY.put("btn_qty_69", 69);
        }

        public static final String BUTTON_CLR = "btn_clear";
        public static final String INP_RELOAD = "inp_reload";
        public static final String INP_QTY = "inp_qty";

        public static final String ORDER_TYPE_LEFT = "order_type_left";
        public static final String ORDER_TYPE_RIGHT = "order_type_right";
        public static final String AUTO_HEDGE_LEFT = "chk_auto_hedge_left";
        public static final String AUTO_HEDGE_RIGHT = "chk_auto_hedge_right";
    }

    private final WebSocketClient client;
    private final KeyedPublisher<String, RemoteOrderManagementCommand> remoteOmsByServer;
    private final LadderOptions ladderOptions;
    private final LadderPipe ui;

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

    public LadderView(WebSocketClient client, Publisher<WebSocketOutboundData> ui, KeyedPublisher<String, RemoteOrderManagementCommand> remoteOmsByServer, LadderOptions ladderOptions) {
        this.client = client;
        this.remoteOmsByServer = remoteOmsByServer;
        this.ladderOptions = ladderOptions;
        this.ui = new LadderPipeImpl(ui);
    }

    public void subscribeToSymbol(String symbol, int levels, MarketDataForSymbol marketDataForSymbol, WorkingOrdersForSymbol workingOrdersForSymbol, ExtraDataForSymbol extraDataForSymbol) {

        this.symbol = symbol;
        this.marketDataForSymbol = marketDataForSymbol;
        this.workingOrdersForSymbol = workingOrdersForSymbol;
        dataForSymbol = extraDataForSymbol;
        this.levels = levels;

        pendingRefDataAndSettle = true;
        tryToDrawLadder();
        tryToUpdateEverything();
    }

    private void tryToUpdateEverything() {
        tryToUpdateBook();
        tryToUpdateTradedVolumes();
        tryToUpdateLastTrade();
        tryToUpdateWorkingOrders();
        tryToDrawExtraData();
    }

    // Events

    public void onWorkingOrderUpdate(WorkingOrderUpdate _) {
        tryToUpdateWorkingOrders();
    }

    public void onMarketDataEvent(MarketDataEvent marketDataEvent) {
        if (symbol == null || marketDataForSymbol == null) {
            throw new IllegalStateException("Should not be receiving market data events before being subscribed.");
        }
        if (pendingRefDataAndSettle) {
            tryToDrawLadder();
        } else if (marketDataEvent instanceof TopOfBook || marketDataEvent instanceof BookConsistencyMarker) {
            tryToUpdateBook();
        } else if (marketDataEvent instanceof TotalTradedVolumeByPrice) {
            tryToUpdateTradedVolumes();
        } else if (marketDataEvent instanceof TradeUpdate) {
            tryToUpdateLastTrade();
        }
    }

    public void onMetadata(LadderMetadata _) {
        tryToDrawExtraData();
    }

    // Drawing

    private void tryToDrawExtraData() {
        ExtraDataForSymbol d = dataForSymbol;
        if (!pendingRefDataAndSettle && d != null) {
            for (LaserLine laserLine : d.laserLineByName.values()) {
                String laserKey = laserKey(laserLine.getId());
                ui.cls(laserKey, Html.HIDDEN, true);
                if (laserLine.isValid()) {
                    long price = bottomPrice;
                    while (price <= topPrice) {
                        long priceAbove = marketDataForSymbol.tickSizeTracker.priceAbove(price);
                        if (price <= laserLine.getPrice() && laserLine.getPrice() <= priceAbove && levelByPrice.containsKey(price)) {
                            long fractionalPrice = laserLine.getPrice() - price;
                            double tickFraction = 1.0 * fractionalPrice / (priceAbove - price);
                            ui.cls(laserKey, Html.HIDDEN, false);
                            ui.height(laserKey, priceKey(price), tickFraction);
                            break;
                        }
                        price = priceAbove;
                    }
                }
            }
        }
    }

    private void tryToUpdateWorkingOrders() {
        WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null) {
            for (Long price : levelByPrice.keySet()) {
                int totalQty = 0;
                String side = "";
                List<String> keys = new ArrayList<String>();
                for (WorkingOrderUpdate order : w.ordersByPrice.get(price)) {
                    totalQty += order.getTotalQuantity() - order.getFilledQuantity();
                    side = order.getSide().toString().toLowerCase();
                    keys.add(WorkingOrdersForSymbol.key(order));
                }
                workingQty(price, totalQty, side);
                ui.data(orderKey(price), "orderKeys", Joiner.on('!').join(keys));
            }
        }
    }

    private void tryToUpdateLastTrade() {
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

    private void tryToUpdateTradedVolumes() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.totalTradedVolumeByPrice != null) {
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
        }
    }

    private void tryToUpdateBook() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (!pendingRefDataAndSettle && m != null && m.book != null) {
            for (Long price : levelByPrice.keySet()) {
                if (m.topOfBook != null && m.topOfBook.getBestBid().getPrice() == price) {
                    bidQty(price, m.topOfBook.getBestBid().getQuantity());
                } else {
                    bidQty(price, m.book.getLevel(price, Side.BID).getQuantity());
                }

                if (m.topOfBook != null && m.topOfBook.getBestOffer().getPrice() == price) {
                    offerQty(price, m.topOfBook.getBestOffer().getQuantity());
                } else {
                    offerQty(price, m.book.getLevel(price, Side.OFFER).getQuantity());
                }
            }
        }
    }

    private void tryToDrawLadder() {
        MarketDataForSymbol m = this.marketDataForSymbol;
        if (m != null && m.refData != null && m.settle != null) {
            pendingRefDataAndSettle = false;

            // Choose center price
            centerPrice = m.settle.getSettlementPrice();
            if (m.lastTrade != null) {
                centerPrice = m.lastTrade.getPrice();
            }
            if (m.auctionIndicativePrice != null) {
                centerPrice = m.auctionIndicativePrice.getIndicativePrice();
            }
            if (m.auctionTradeUpdate != null) {
                centerPrice = m.auctionTradeUpdate.getPrice();
            }
            if (m.topOfBook != null && m.topOfBook.getBestOffer().isExists()) {
                centerPrice = m.topOfBook.getBestOffer().getPrice();
            }
            if (m.topOfBook != null && m.topOfBook.getBestBid().isExists()) {
                centerPrice = m.topOfBook.getBestBid().getPrice();
            }

            boolean clickTradingEnabled = true;
            ui.draw(levels, m.refData.getSymbol());
            ui.trading(clickTradingEnabled, ladderOptions.orderTypesLeft, ladderOptions.orderTypesRight);

            drawPricesAndUpdateTopAndBottomPrice();
        }
    }

    private void drawPricesAndUpdateTopAndBottomPrice() {
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
                ui.txt(priceKey(price), m.priceFormat.format(price));
                ui.txt(volumeKey(price), Html.EMPTY);
                ui.txt(orderKey(price), Html.EMPTY);

                ui.data(bidKey(price), Html.PRICE_KEY, price);
                ui.data(offerKey(price), Html.PRICE_KEY, price);
                ui.data(orderKey(price), Html.PRICE_KEY, price);

                bottomPrice = price;
                price = m.tickSizeTracker.priceBelow(price);
            }

            ui.flush();
        }

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
            drawPricesAndUpdateTopAndBottomPrice();
            tryToUpdateEverything();
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


    private int clickTradingBoxQty = 0;
    private int reloadBoxQty = 0;
    private boolean autoHedgeLeft = true;
    private boolean autoHedgeRight = true;
    private String orderTypeLeft = "";
    private String orderTypeRight = "";


    public void tryToDrawClickTrading() {
        if(!pendingRefDataAndSettle) {
            ui.txt(Html.INP_QTY, clickTradingBoxQty);
            ui.txt(Html.INP_RELOAD, reloadBoxQty);
            ui.txt(Html.AUTO_HEDGE_LEFT, autoHedgeLeft);
            ui.txt(Html.AUTO_HEDGE_RIGHT, autoHedgeRight);
            ui.txt(Html.ORDER_TYPE_LEFT, orderTypeLeft);
            ui.txt(Html.ORDER_TYPE_RIGHT, orderTypeRight);
        }
    }

    private void onUpdate(String label, Map<String, String> dataArg) {
        String value = dataArg.get("value");
        if(Html.INP_RELOAD.equals(label)) {
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
        } else {
            throw new IllegalArgumentException("Update for unknown value: " + label + " " + dataArg);
        }
        tryToDrawClickTrading();
    }

    private void onClick(String label, Map<String, String> data) {
        if (Html.BUTTON_QTY.containsKey(label)) {
            clickTradingBoxQty += Html.BUTTON_QTY.get(label);
        } else if (Html.BUTTON_CLR.equals(label)) {
            clickTradingBoxQty = 0;
        } else {
            for (Long price : levelByPrice.keySet()) {
                // Dispatch on cell type
                if (label.equals(orderKey(price))) {
                    if (data.containsKey("price") && !Long.valueOf(data.get("price")).equals(price)) {
                        throw new IllegalArgumentException(client.toString() + ": Received click on box " + label + " for price " + price + " but UI thinks it is price " + data.get("price") + " , data: " + data.toString());
                    }
                    cancelWorkingOrders(price, data);
                } else if (label.equals(bidKey(price))) {
                    clickTradingBoxQty = reloadBoxQty;
                } else if (label.equals(offerKey(price))) {
                    clickTradingBoxQty = reloadBoxQty;
                }
            }
        }
        tryToDrawClickTrading();
    }

    private void cancelWorkingOrders(Long price, Map<String, String> data) {
        WorkingOrdersForSymbol w = this.workingOrdersForSymbol;
        if (!pendingRefDataAndSettle && w != null) {
            for (WorkingOrderUpdate workingOrderUpdate : w.ordersByPrice.get(price)) {
                remoteOmsByServer.publish(workingOrderUpdate.getServerName(), new RemoteCancelOrder(workingOrderUpdate.getServerName(), client.getUserName(), workingOrderUpdate.getChainId(), new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), workingOrderUpdate.getPrice(), workingOrderUpdate.getTotalQuantity(), RemoteOrderType.MANUAL, false, workingOrderUpdate.getTag())));
            }
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
        ui.cls(orderKey(price), "working_bid", side.equals("bid"));
        ui.cls(orderKey(price), "working_offer", side.equals("offer"));
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

    public void flush() {
        ui.flush();
    }

}
