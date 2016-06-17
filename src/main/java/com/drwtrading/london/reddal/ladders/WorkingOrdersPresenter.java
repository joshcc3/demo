package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
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

    public static final Predicate<Main.WorkingOrderUpdateFromServer> WORKING_ORDER_FILTER = WorkingOrdersPresenter::workingOrderFilter;
    public static final Predicate<Main.WorkingOrderUpdateFromServer> NON_GTC_FILTER = WorkingOrdersPresenter::nonGTCFilter;
    private final WebSocketViews<WorkingOrderView> views = WebSocketViews.create(WorkingOrderView.class, this);
    private final Map<String, Main.WorkingOrderUpdateFromServer> workingOrders = new HashMap<>();
    private final Map<String, Main.WorkingOrderUpdateFromServer> dirty = new HashMap<>();
    private final Map<String, SearchResult> searchResults = new HashMap<>();
    private final Publisher<StatsMsg> statsMsgPublisher;
    private final Publisher<Main.RemoteOrderCommandToServer> commands;

    private final DecimalFormat df;

    public WorkingOrdersPresenter(final Scheduler scheduler, final Publisher<StatsMsg> statsMsgPublisher,
            final Publisher<Main.RemoteOrderCommandToServer> commands) {
        this.statsMsgPublisher = statsMsgPublisher;
        this.commands = commands;
        scheduler.scheduleWithFixedDelay(this::repaint, 100, 250, TimeUnit.MILLISECONDS);

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS);
    }

    public void addSearchResult(final SearchResult searchResult) {
        searchResults.put(searchResult.symbol, searchResult);
    }

    public void onWorkingOrderBatch(final List<Main.WorkingOrderUpdateFromServer> batch) {
        batch.stream().filter(WORKING_ORDER_FILTER).forEach(this::onWorkingOrder);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final WorkingOrderView view = views.register(connected);
        for (final Main.WorkingOrderUpdateFromServer update : workingOrders.values()) {
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
        final Main.WorkingOrderUpdateFromServer order = workingOrders.get(key);
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

    public static boolean workingOrderFilter(final Main.WorkingOrderUpdateFromServer order) {
        return !"synthetic".equals(order.fromServer);
    }

    public static boolean nonGTCFilter(final Main.WorkingOrderUpdateFromServer order) {
        return !(order.fromServer.toUpperCase().contains("GTC") ||
                order.value.getTag().toUpperCase().contains("GTC") ||
                order.value.getWorkingOrderType().name().toUpperCase().contains("GTC"));
    }

    private void cancel(final String user, final Main.WorkingOrderUpdateFromServer order) {
        commands.publish(new Main.RemoteOrderCommandToServer(order.fromServer,
                new RemoteCancelOrder(order.fromServer, user, order.value.getChainId(),
                        LadderView.getRemoteOrderFromWorkingOrder(false, order.value.getPrice(), order.value,
                                order.value.getTotalQuantity()))));
    }

    private void repaint() {
        for (final Main.WorkingOrderUpdateFromServer workingOrderUpdate : dirty.values()) {
            publishWorkingOrderUpdate(views.all(), workingOrderUpdate);
        }
        dirty.clear();
    }

    private void onWorkingOrder(final Main.WorkingOrderUpdateFromServer order) {
        if (order.value.getWorkingOrderState() == WorkingOrderState.DEAD) {
            workingOrders.remove(order.key());
        } else {
            workingOrders.put(order.key(), order);
        }
        dirty.put(order.key(), order);
    }

    private void publishWorkingOrderUpdate(final WorkingOrderView view, final Main.WorkingOrderUpdateFromServer order) {
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

    public interface WorkingOrderView {

        void updateWorkingOrder(final String key, final String instrument, final String side, final String price, final int filledQuantity,
                final int quantity, final String state, final String orderAlgoType, final String tag, final String server,
                final boolean isDead);
    }
}
