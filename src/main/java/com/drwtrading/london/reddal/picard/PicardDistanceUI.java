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

public class PicardDistanceUI {

    private static final int FLUSH_PERIOD_MILLIS = 600;

    private final UILogger webLog;

    private final Map<BookSide, Map<String, PicardDistanceData>> allDatas;
    private final Map<BookSide, PicardDistanceTable> sideViews;

    private final WebSocketViews<IPicardDistanceView> views;

    public PicardDistanceUI(final UILogger webLog) {

        this.webLog = webLog;

        this.allDatas = new EnumMap<>(BookSide.class);
        this.sideViews = new EnumMap<>(BookSide.class);

        for (final BookSide side : BookSide.values()) {
            this.allDatas.put(side, new HashMap<>());
            this.sideViews.put(side, new PicardDistanceTable());
        }

        this.views = new WebSocketViews<>(IPicardDistanceView.class, this);
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {

        final PicardDistanceTable bidDistances = sideViews.get(BookSide.BID);
        bidDistances.setDisplaySymbol(displaySymbol.marketDataSymbol, displaySymbol.displaySymbol);

        final PicardDistanceTable askDistances = sideViews.get(BookSide.ASK);
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

        final IPicardDistanceView view = views.register(connected);

        final PicardDistanceTable bidDistances = sideViews.get(BookSide.BID);
        bidDistances.flushTo(view);

        final PicardDistanceTable askDistances = sideViews.get(BookSide.ASK);
        askDistances.flushTo(view);

    }

    public void setDistanceData(final PicardDistanceData data) {

        final Map<String, PicardDistanceData> sideData = allDatas.get(data.side);
        final PicardDistanceData oldData = sideData.put(data.symbol, data);

        final PicardDistanceTable table = this.sideViews.get(data.side);

        if (null != oldData) {
            table.removeData(oldData);
        }

        table.setData(data);
    }

    public long updateUI() {

        final PicardDistanceTable bidDistances = sideViews.get(BookSide.BID);
        bidDistances.update(views.all());

        final PicardDistanceTable askDistances = sideViews.get(BookSide.ASK);
        askDistances.update(views.all());

        return FLUSH_PERIOD_MILLIS;
    }
}
