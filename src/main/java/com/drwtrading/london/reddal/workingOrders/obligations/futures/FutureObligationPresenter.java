package com.drwtrading.london.reddal.workingOrders.obligations.futures;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.PriceQtyPair;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByBestPrice;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.photons.eeif.configuration.EeifConfiguration;
import com.drwtrading.photons.eeif.configuration.QuotingObligation;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class FutureObligationPresenter implements IWorkingOrdersCallback {

    private static final long OBLIGATION_CALC_PERIOD_MILLIS = 1000;

    private final Map<String, FutureObligationPerformance> performanceMap;
    private final Map<String, WorkingOrdersByBestPrice> workingOrders;
    private final Map<String, QuotingObligation> obligations;

    private final WebSocketViews<IFutureObligationView> views;

    private final DecimalFormat oneDP;

    public FutureObligationPresenter() {

        this.performanceMap = new HashMap<>();
        this.workingOrders = new HashMap<>();
        this.obligations = new HashMap<>();

        this.views = new WebSocketViews<>(IFutureObligationView.class, this);

        this.oneDP = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 1);
    }

    public void start(final SelectIO selectIO) {
        selectIO.addDelayedAction(OBLIGATION_CALC_PERIOD_MILLIS, this::calcObligations);
    }

    public void setEeifConfig(final EeifConfiguration eeifConfig) {

        if (eeifConfig instanceof QuotingObligation) {

            final QuotingObligation quotingObligation = (QuotingObligation) eeifConfig;
            obligations.put(quotingObligation.getSymbol(), quotingObligation);
        }
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByBestPrice orderedWorkingOrders =
                workingOrders.computeIfAbsent(workingOrder.order.getSymbol(), WorkingOrdersByBestPrice::new);
        orderedWorkingOrders.setWorkingOrder(workingOrder);
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.get(workingOrder.order.getSymbol());
        orderedWorkingOrders.removeWorkingOrder(workingOrder);
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        for (final WorkingOrdersByBestPrice orderedWorkingOrders : workingOrders.values()) {
            orderedWorkingOrders.connectionLost(source);
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);
        }
    }

    public void onConnected(final WebSocketConnected connected) {

        final IFutureObligationView view = views.register(connected);
        for (final FutureObligationPerformance obligation : performanceMap.values()) {
            update(view, obligation);
        }
    }

    public void onDisconnected(final WebSocketDisconnected disconnected) {

        views.unregister(disconnected);
    }

    private long calcObligations() {

        for (final QuotingObligation obligation : obligations.values()) {

            final WorkingOrdersByBestPrice workingOrders = this.workingOrders.get(obligation.getSymbol());
            final FutureObligationPerformance obligationPerformance = getObligationPerformance(obligation, workingOrders);

            performanceMap.put(obligation.getSymbol(), obligationPerformance);

            update(views.all(), obligationPerformance);
        }
        return OBLIGATION_CALC_PERIOD_MILLIS;
    }

    public void update(final IFutureObligationView view, final FutureObligationPerformance performance) {

        final QuotingObligation obligation = performance.getObligation();

        final String bpsObligation = Integer.toString(obligation.getBpsWide());
        final String qtyObligation = Integer.toString(obligation.getQuantity());

        final boolean isObligationMet = performance.isObligationMet();

        final String bpsWide = oneDP.format(performance.getBpsWide());
        final String qtyShowing = Long.toString(performance.getQtyShowing());

        view.setObligation(obligation.getSymbol(), bpsObligation, qtyObligation, isObligationMet, bpsWide, qtyShowing);
    }

    private static FutureObligationPerformance getObligationPerformance(final QuotingObligation obligation,
            final WorkingOrdersByBestPrice workingOrders) {

        if (null == workingOrders) {

            return new FutureObligationPerformance(obligation, false, Double.POSITIVE_INFINITY, 0);
        } else {

            final PriceQtyPair bidShowing = workingOrders.getPriceToQty(BookSide.BID, obligation.getQuantity());
            final PriceQtyPair askShowing = workingOrders.getPriceToQty(BookSide.ASK, obligation.getQuantity());

            final double bpsWide;

            if (0 < bidShowing.qty && 0 < askShowing.qty) {
                bpsWide = (askShowing.price - bidShowing.price) * 10000d / bidShowing.price;
            } else {
                bpsWide = Double.POSITIVE_INFINITY;
            }

            final long qtyShowing = Math.min(bidShowing.qty, askShowing.qty);

            final boolean isObligationMet = bpsWide <= obligation.getBpsWide() && obligation.getQuantity() <= qtyShowing;

            return new FutureObligationPerformance(obligation, isObligationMet, bpsWide, qtyShowing);
        }
    }

}
