package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LiquidityFinderViewUI {

    private static final int FLUSH_PERIOD_MILLIS = 600;

    private final UILogger webLog;

    private final Map<BookSide, Map<String, LiquidityFinderData>> allDatas;
    private final Map<BookSide, LiquidityFinderViewTable> sideViews;

    private final WebSocketViews<ILiquidityFinderView> views;

    public LiquidityFinderViewUI(final UILogger webLog) {

        this.webLog = webLog;

        this.allDatas = new EnumMap<>(BookSide.class);
        this.sideViews = new EnumMap<>(BookSide.class);

        for (final BookSide side : BookSide.values()) {
            this.allDatas.put(side, new HashMap<>());
            this.sideViews.put(side, new LiquidityFinderViewTable());
        }

        this.views = new WebSocketViews<>(ILiquidityFinderView.class, this);
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {

        final LiquidityFinderViewTable bidDistances = sideViews.get(BookSide.BID);
        bidDistances.setDisplaySymbol(displaySymbol.marketDataSymbol, displaySymbol.displaySymbol);

        final LiquidityFinderViewTable askDistances = sideViews.get(BookSide.ASK);
        askDistances.setDisplaySymbol(displaySymbol.marketDataSymbol, displaySymbol.displaySymbol);
    }

    public void webControl(final WebSocketControlMessage msg) {

        if (msg instanceof WebSocketConnected) {

            webUIConnected((WebSocketConnected) msg);

        } else if (msg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) msg);
        }
    }

    private void webUIConnected(final WebSocketConnected connected) {

        final ILiquidityFinderView view = views.register(connected);

        final LiquidityFinderViewTable bidDistances = sideViews.get(BookSide.BID);
        bidDistances.flushTo(view);

        final LiquidityFinderViewTable askDistances = sideViews.get(BookSide.ASK);
        askDistances.flushTo(view);

    }

    public void setDistanceData(final LiquidityFinderData data) {

        final Map<String, LiquidityFinderData> sideData = allDatas.get(data.side);
        final LiquidityFinderData oldData = sideData.put(data.symbol, data);

        final LiquidityFinderViewTable table = this.sideViews.get(data.side);

        if (null != oldData) {
            table.removeData(oldData);
        }

        table.setData(data);
    }

    public long updateUI() {

        final LiquidityFinderViewTable bidDistances = sideViews.get(BookSide.BID);
        bidDistances.update(views.all());

        final LiquidityFinderViewTable askDistances = sideViews.get(BookSide.ASK);
        askDistances.update(views.all());

        return FLUSH_PERIOD_MILLIS;
    }
}
