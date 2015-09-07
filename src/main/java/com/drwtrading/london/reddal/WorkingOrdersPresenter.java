package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.core.Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WorkingOrdersPresenter {

    private final WebSocketViews<WorkingOrderView> views = WebSocketViews.create(WorkingOrderView.class, this);
    Map<String, Main.WorkingOrderUpdateFromServer> workingOrders = new HashMap<>();
    Map<String, Main.WorkingOrderUpdateFromServer> dirty = new HashMap<>();
    Map<String, InstrumentDefinitionEvent> defs = new HashMap<>();

    public WorkingOrdersPresenter(Scheduler scheduler) {
        scheduler.scheduleWithFixedDelay(this::repaint, 100, 250, TimeUnit.MILLISECONDS);
    }

    public Runnable repaint() {
        return new Runnable() {
            @Override
            public void run() {
                // Can we always just repaint stuff here? Will we ever get the case where something has been removed and is now dead?
                for (Main.WorkingOrderUpdateFromServer workingOrderUpdate : dirty.values()) {
                    publishWorkingOrderUpdate(views.all(), workingOrderUpdate);
                }
                dirty.clear();
            }
        };
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        defs.put(instrumentDefinitionEvent.getSymbol(), instrumentDefinitionEvent);
    }

    public void onWorkingOrderBatch(List<Main.WorkingOrderUpdateFromServer> batch) {
        batch.forEach(this::on);
    }

    public void on(Main.WorkingOrderUpdateFromServer order) {
        if (order.value.getWorkingOrderState() == WorkingOrderState.DEAD) {
            workingOrders.remove(order.key());
        } else {
            workingOrders.put(order.key(), order);
        }
        dirty.put(order.key(), order);
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
