package com.drwtrading.london.reddal.orderManagement.remoteOrder.ui;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.ui.msgs.GTCSupportedSymbol;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BulkOrderEntry {

    private static final String PAGE_ID = BulkOrderEntry.class.getSimpleName();

    private final UILogger webLog;

    private final WebSocketViews<IBulkOrderEntryView> views;

    private final Set<String> supportedSymbols;
    private final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher;
    private final Channel<LadderClickTradingIssue> tradingIssues;

    public BulkOrderEntry(final UILogger webLog, final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher,
            final Channel<LadderClickTradingIssue> tradingIssues) {

        this.webLog = webLog;
        this.remoteOrderCommandToServerPublisher = remoteOrderCommandToServerPublisher;
        this.tradingIssues = tradingIssues;

        this.views = new WebSocketViews<>(IBulkOrderEntryView.class, this);

        this.supportedSymbols = new HashSet<>();
    }

    public void setGTCSupportedSymbol(final GTCSupportedSymbol gtcSupportedSymbol) {

        supportedSymbols.add(gtcSupportedSymbol.symbol);
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
