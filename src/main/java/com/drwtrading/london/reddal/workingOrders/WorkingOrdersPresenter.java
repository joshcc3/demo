package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.ladders.LadderView;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Scheduler;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class WorkingOrdersPresenter {

    public static final Predicate<WorkingOrderUpdateFromServer> WORKING_ORDER_FILTER = WorkingOrdersPresenter::workingOrderFilter;
    public static final Predicate<WorkingOrderUpdateFromServer> NON_GTC_FILTER = WorkingOrdersPresenter::nonGTCFilter;

    private final Publisher<StatsMsg> statsMsgPublisher;
    private final Publisher<Main.RemoteOrderCommandToServer> commands;

    private final WebSocketViews<IWorkingOrderView> views;
    private final Map<String, WorkingOrderUpdateFromServer> workingOrders;
    private final Map<String, WorkingOrderUpdateFromServer> dirty;
    private final Map<String, SearchResult> searchResults;

    private final DecimalFormat df;

    public WorkingOrdersPresenter(final Scheduler scheduler, final Publisher<StatsMsg> statsMsgPublisher,
            final Publisher<Main.RemoteOrderCommandToServer> commands) {

        this.statsMsgPublisher = statsMsgPublisher;
        this.commands = commands;

        this.views = WebSocketViews.create(IWorkingOrderView.class, this);
        this.workingOrders = new HashMap<>();
        this.dirty = new HashMap<>();
        this.searchResults = new HashMap<>();

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS);

        scheduler.scheduleWithFixedDelay(this::repaint, 100, 250, TimeUnit.MILLISECONDS);
    }

    public void addSearchResult(final SearchResult searchResult) {
        searchResults.put(searchResult.symbol, searchResult);
    }

    public void onWorkingOrderBatch(final List<WorkingOrderUpdateFromServer> batch) {
        batch.stream().filter(WORKING_ORDER_FILTER).forEach(this::onWorkingOrder);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final IWorkingOrderView view = views.register(connected);
        for (final WorkingOrderUpdateFromServer update : workingOrders.values()) {
            publishWorkingOrderUpdate(view, update);
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        views.invoke(msg);
    }

    // --------------

    @FromWebSocketView
    public void cancelOrder(final String key, final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        final WorkingOrderUpdateFromServer order = workingOrders.get(key);
        if (null == order) {
            statsMsgPublisher.publish(
                    new AdvisoryStat("Reddal Working Orders", AdvisoryStat.Level.INFO, "Tried to cancel non-existent order [" + key + "]"));
        } else {
            cancel(user, order);
        }
    }

    @FromWebSocketView
    public void cancelAll(final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        workingOrders.values().stream().forEach(order -> {
            cancel(user, order);
        });
    }

    @FromWebSocketView
    public void cancelNonGTC(final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        workingOrders.values().stream().filter(NON_GTC_FILTER).forEach(order -> {
            cancel(user, order);
        });
    }

    // -----------------

    public static boolean workingOrderFilter(final WorkingOrderUpdateFromServer order) {
        return !"synthetic".equals(order.fromServer);
    }

    public static boolean nonGTCFilter(final WorkingOrderUpdateFromServer order) {
        return !(order.fromServer.toUpperCase().contains("GTC") ||
                order.value.getTag().toUpperCase().contains("GTC") ||
                order.value.getWorkingOrderType().name().toUpperCase().contains("GTC"));
    }

    private void cancel(final String user, final WorkingOrderUpdateFromServer order) {
        commands.publish(new Main.RemoteOrderCommandToServer(order.fromServer,
                new RemoteCancelOrder(order.fromServer, user, order.value.getChainId(),
                        LadderView.getRemoteOrderFromWorkingOrder(false, order.value.getPrice(), order.value,
                                order.value.getTotalQuantity()))));
    }

    private void repaint() {
        for (final WorkingOrderUpdateFromServer workingOrderUpdate : dirty.values()) {
            publishWorkingOrderUpdate(views.all(), workingOrderUpdate);
        }
        dirty.clear();
    }

    private void onWorkingOrder(final WorkingOrderUpdateFromServer order) {
        if (order.value.getWorkingOrderState() == WorkingOrderState.DEAD) {
            workingOrders.remove(order.key());
        } else {
            workingOrders.put(order.key(), order);
        }
        dirty.put(order.key(), order);
    }

    private void publishWorkingOrderUpdate(final IWorkingOrderView view, final WorkingOrderUpdateFromServer order) {
        final WorkingOrderUpdate update = order.value;
        final SearchResult searchResult = searchResults.get(update.getSymbol());
        final String price;
        if (null != searchResult) {
            df.setMinimumFractionDigits(searchResult.decimalPlaces);
            df.setMaximumFractionDigits(searchResult.decimalPlaces);
            price = df.format(update.getPrice());
        } else {
            price = update.getPrice() + " (raw)";
        }
        view.updateWorkingOrder(order.key(), update.getSymbol(), update.getSide().toString(), price, update.getFilledQuantity(),
                update.getTotalQuantity(), update.getWorkingOrderState().toString(), update.getWorkingOrderType().toString(),
                update.getTag(), order.fromServer, update.getWorkingOrderState() == WorkingOrderState.DEAD);
    }

}
