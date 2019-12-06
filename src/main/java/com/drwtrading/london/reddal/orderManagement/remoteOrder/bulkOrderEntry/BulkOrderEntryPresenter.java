package com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPrices;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPricesRequest;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCSupportedSymbol;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.gtc.GTCWorkingOrderMaintainer;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BulkOrderEntryPresenter {

    private static final String PAGE_ID = BulkOrderEntryPresenter.class.getSimpleName();

    private static final long BETTERMENT_RESPONSE_MAX_DELAY_MILLIS = 2_000;
    private static final String BETTERMENT_QTY = "50";

    private final IClock clock;
    private final UILogger webLog;

    private final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher;
    private final Channel<LadderClickTradingIssue> tradingIssues;

    private final Publisher<GTCBettermentPricesRequest> gtcBettermentRequests;

    private final GTCWorkingOrderMaintainer gtcOrders;

    private final WebSocketViews<IBulkOrderEntryView> views;

    private final Set<String> supportedSymbols;
    private final DecimalFormat priceDF;

    private final Map<BookSide, Map<String, Long>> bettermentPriceLimits;

    public BulkOrderEntryPresenter(final IClock clock, final UILogger webLog,
            final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher, final Channel<LadderClickTradingIssue> tradingIssues,
            final Publisher<GTCBettermentPricesRequest> gtcBettermentRequests, final GTCWorkingOrderMaintainer gtcOrders) {

        this.clock = clock;
        this.webLog = webLog;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.tradingIssues = tradingIssues;

        this.gtcBettermentRequests = gtcBettermentRequests;

        this.gtcOrders = gtcOrders;

        this.views = new WebSocketViews<>(IBulkOrderEntryView.class, this);

        this.supportedSymbols = new HashSet<>();
        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 1, 10);

        this.bettermentPriceLimits = new EnumMap<>(BookSide.class);
        bettermentPriceLimits.put(BookSide.BID, Collections.emptyMap());
        bettermentPriceLimits.put(BookSide.ASK, Collections.emptyMap());
    }

    public void setGTCSupportedSymbol(final GTCSupportedSymbol gtcSupportedSymbol) {

        supportedSymbols.add(gtcSupportedSymbol.symbol);
    }

    public void setPriceLimits(final Map<BookSide, Map<String, Long>> values) {

        bettermentPriceLimits.put(BookSide.BID, values.get(BookSide.BID));
        bettermentPriceLimits.put(BookSide.ASK, values.get(BookSide.ASK));
    }

    public void webControl(final WebSocketControlMessage msg) {

        if (msg instanceof WebSocketConnected) {

            webUIConnected((WebSocketConnected) msg);

        } else if (msg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) msg);

        } else if (msg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) msg);
        }
    }

    private void webUIConnected(final WebSocketConnected connected) {

        views.register(connected);
    }

    private void inboundData(final WebSocketInboundData inbound) {

        webLog.write(PAGE_ID, inbound);
        views.invoke(inbound);
    }

    @FromWebSocketView
    public void createOrderLine(final String symbol, final String side, final String qty, final WebSocketInboundData data) {

        final IBulkOrderEntryView view = views.get(data.getOutboundChannel());

        if (supportedSymbols.contains(symbol)) {
            view.addOrder(symbol, side, qty);
        } else {
            view.printError("Symbol [" + symbol + "] not supported for GTC.");
        }
    }

    @FromWebSocketView
    public void createOrderForAllBettermentOrders(final WebSocketInboundData data) {

        final Publisher<WebSocketOutboundData> responseChannel = data.getOutboundChannel();
        final Map<BookSide, Map<String, WorkingOrder>> workingOrders = gtcOrders.getBestWorkingOrders();

        final long milliSinceMidnight = clock.getMillisSinceMidnightUTC();
        final GTCBettermentPricesRequest request = new GTCBettermentPricesRequest(milliSinceMidnight, responseChannel, workingOrders);

        gtcBettermentRequests.publish(request);
    }

    public void bulkOrderResponse(final GTCBettermentPrices gtcBettermentPrices) {

        final long milliSinceMidnight = clock.getMillisSinceMidnightUTC();

        if (milliSinceMidnight - gtcBettermentPrices.requestTimeMilliSinceMidnight < BETTERMENT_RESPONSE_MAX_DELAY_MILLIS) {

            final IBulkOrderEntryView view = views.get(gtcBettermentPrices.responseChannel);

            createAllBettermentOrders(view, BookSide.BID, gtcBettermentPrices.existingOrders);
            createAllBettermentOrders(view, BookSide.ASK, gtcBettermentPrices.existingOrders);
        }
    }

    private void createAllBettermentOrders(final IBulkOrderEntryView view, final BookSide side,
            final EnumMap<BookSide, Map<String, Long>> bettermentPrices) {

        final Map<String, Long> symbolPrices = bettermentPrices.get(side);
        final Map<String, Long> symbolLimits = bettermentPriceLimits.get(side);

        for (final Map.Entry<String, Long> symbolPrice : symbolPrices.entrySet()) {

            final String symbol = symbolPrice.getKey();

            final Long price = symbolPrice.getValue();
            final Long limitPrice = symbolLimits.get(symbol);

            if (null != limitPrice && -1 < side.tradeSignum * Long.compare(limitPrice, price)) {

                if (supportedSymbols.contains(symbol)) {

                    final String humanPrice = priceDF.format(price / (double) Constants.NORMALISING_FACTOR);
                    view.addPricedOrder(symbol, side.name(), humanPrice, BETTERMENT_QTY);
                } else {
                    view.printError("Symbol [" + symbol + "] not supported for GTC.");
                }
            }
        }
    }

    @FromWebSocketView
    public void sendOrders(final WebSocketInboundData data) throws JSONException {

        final String orders = data.getData().split(",", 2)[1];
        final IBulkOrderEntryView view = views.get(data.getOutboundChannel());

        final List<SubmitOrderCmd> cmds = new LinkedList<>();

        final JSONArray jsonOrders = new JSONArray(orders);
        for (int i = 0; i < jsonOrders.length(); ++i) {

            final JSONObject orderDetails = jsonOrders.getJSONObject(i);

            final String user = data.getClient().getUserName();
            final String symbol = orderDetails.getString("symbol");
            final BookSide side = BookSide.valueOf(orderDetails.getString("side"));
            final long price = (long) (orderDetails.getDouble("price") * Constants.NORMALISING_FACTOR);
            final int qty = orderDetails.getInt("qty");

            if (supportedSymbols.contains(symbol)) {

                final SubmitOrderCmd cmd =
                        new SubmitOrderCmd(symbol, tradingIssues, user, side, OrderType.GTC, AlgoType.THOR, "GTC", price, qty);
                cmds.add(cmd);
            } else {

                final String msg = "Symbol [" + symbol + "] not supported for GTC.";
                view.printError(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        for (final SubmitOrderCmd cmd : cmds) {
            remoteOrderCommandToServerPublisher.publish(cmd);
        }
        view.clearOrders();
    }
}
