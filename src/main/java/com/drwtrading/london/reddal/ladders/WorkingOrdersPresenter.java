package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Scheduler;

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
    private final Map<String, InstrumentDefinitionEvent> defs = new HashMap<>();
    private final Publisher<StatsMsg> statsMsgPublisher;
    private final Publisher<Main.RemoteOrderCommandToServer> commands;

    public WorkingOrdersPresenter(Scheduler scheduler, Publisher<StatsMsg> statsMsgPublisher, Publisher<Main.RemoteOrderCommandToServer> commands) {
        this.statsMsgPublisher = statsMsgPublisher;
        this.commands = commands;
        scheduler.scheduleWithFixedDelay(this::repaint, 100, 250, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        defs.put(instrumentDefinitionEvent.getSymbol(), instrumentDefinitionEvent);
    }

    public void onWorkingOrderBatch(List<Main.WorkingOrderUpdateFromServer> batch) {
        batch.stream()
                .filter(WORKING_ORDER_FILTER)
                .forEach(this::onWorkingOrder);
    }

    @Subscribe
    public void onConnected(WebSocketConnected connected) {
        WorkingOrderView view = views.register(connected);
        for (Main.WorkingOrderUpdateFromServer update : workingOrders.values()) {
            publishWorkingOrderUpdate(view, update);
        }
    }

    @Subscribe
    public void onDisconnected(WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @Subscribe
    public void onMessage(WebSocketInboundData msg) {
        views.invoke(msg);
    }

    // --------------

    @FromWebSocketView
    public void cancelOrder(String key, WebSocketInboundData data) {
        String user = data.getClient().getUserName();
        Main.WorkingOrderUpdateFromServer order = workingOrders.get(key);
        if (null == order) {
            statsMsgPublisher.publish(new AdvisoryStat("Reddal Working Orders", AdvisoryStat.Level.INFO, "Tried to cancel non-existent order [" + key + "]"));
        } else {
            cancel(user, order);
        }
    }

    @FromWebSocketView
    public void cancelAll(WebSocketInboundData data) {
        String user = data.getClient().getUserName();
        workingOrders.values().stream()
                .forEach(order -> {
                    cancel(user, order);
                });
    }

    @FromWebSocketView
    public void cancelNonGTC(WebSocketInboundData data) {
        String user = data.getClient().getUserName();
        workingOrders.values().stream()
                .filter(NON_GTC_FILTER)
                .forEach(order -> {
                    cancel(user, order);
                });
    }

    // -----------------

    public static boolean workingOrderFilter(Main.WorkingOrderUpdateFromServer order) {
        return !order.fromServer.equals("synthetic");
    }

    public static boolean nonGTCFilter(Main.WorkingOrderUpdateFromServer order) {
        return !(order.fromServer.toUpperCase().contains("GTC") ||
                order.value.getTag().toUpperCase().contains("GTC") ||
                order.value.getWorkingOrderType().name().toUpperCase().contains("GTC"));
    }

    private void cancel(String user, Main.WorkingOrderUpdateFromServer order) {
        commands.publish(new Main.RemoteOrderCommandToServer(order.fromServer,
                new RemoteCancelOrder(order.fromServer, user, order.value.getChainId(),
                        LadderView.getRemoteOrderFromWorkingOrder(false, order.value.getPrice(), order.value,
                                order.value.getTotalQuantity()))));
    }

    private void repaint() {
        for (Main.WorkingOrderUpdateFromServer workingOrderUpdate : dirty.values()) {
            publishWorkingOrderUpdate(views.all(), workingOrderUpdate);
        }
        dirty.clear();
    }

    private void onWorkingOrder(Main.WorkingOrderUpdateFromServer order) {
        if (order.value.getWorkingOrderState() == WorkingOrderState.DEAD) {
            workingOrders.remove(order.key());
        } else {
            workingOrders.put(order.key(), order);
        }
        dirty.put(order.key(), order);
    }

    private void publishWorkingOrderUpdate(WorkingOrderView view, Main.WorkingOrderUpdateFromServer order) {
        WorkingOrderUpdate update = order.value;
        InstrumentDefinitionEvent def = defs.get(update.getSymbol());
        String price;
        if (null != def) {
            price = PriceFormats.from(def.getPriceStructure().getTickStructure()).format(update.getPrice());
        } else {
            price = update.getPrice() + " (raw)";
        }
        view.updateWorkingOrder(order.key(),
                update.getSymbol(),
                update.getSide().toString(),
                price,
                update.getFilledQuantity(),
                update.getTotalQuantity(),
                update.getWorkingOrderState().toString(),
                update.getWorkingOrderType().toString(),
                update.getTag(),
                order.fromServer,
                update.getWorkingOrderState() == WorkingOrderState.DEAD
        );
    }

    public interface WorkingOrderView {
        void updateWorkingOrder(String key,
                                String instrument,
                                String side,
                                String price,
                                int filledQuantity,
                                int quantity,
                                String state,
                                String orderAlgoType,
                                String tag,
                                String server,
                                boolean isDead
        );
    }
}
