package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.util.*;

public class FxUi {

    static final List<CCY> CCY_LIST = Arrays.asList(CCY.EUR, CCY.GBP, CCY.USD, CCY.CHF, CCY.NOK, CCY.SEK, CCY.DKK, CCY.RUB, CCY.JPY);

    private final FXCalc<?> fxCalc;
    private final WebSocketViews<FxView> views = new WebSocketViews<>(FxView.class, this);

    public FxUi(final FXCalc<?> fxCalc) {
        this.fxCalc = fxCalc;
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        final FxView register = views.register(connected);
        register.create(CCY_LIST);
    }

    @Subscribe
    public void on(final WebSocketInboundData data) {
        views.invoke(data);
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @FromWebSocketView
    public void convert(final String input, final boolean flip, final WebSocketInboundData data) {
        final FxView fxView = views.get(data.getOutboundChannel());
        final double value;
        try {
            value = Double.valueOf(input);
        } catch (final Throwable t) {
            fxView.error("[" + input + "] isn't a number");
            return;
        }
        final Map<String, Double> result = new HashMap<>();
        for (final CCY from : CCY_LIST) {
            for (final CCY to : CCY_LIST) {
                double rate = fxCalc.getLastValidMid(from, to);
                if (Double.isNaN(rate)) {
                    rate = fxCalc.getLastValidMid(from, CCY.EUR) * fxCalc.getLastValidMid(CCY.EUR, to);
                }
                final double converted =  flip ? rate * value : value / rate ;
                result.put(from.name() + "_" + to.name(), converted);
            }
        }
        fxView.update(result);
    }

    public interface FxView {
        public void create(List<CCY> currencies);
        public void update(Map<String, Double> rates);
        public void error(String err);
    }

}
