package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.protocols.photon.execution.RemoteCancelOrder;
import com.drwtrading.london.protocols.photon.execution.RemoteShutdownOms;
import com.drwtrading.london.protocols.photon.execution.RemoteStopAllStrategy;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.ladders.LadderView;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class WorkingOrdersPresenter {

    public static final Predicate<WorkingOrderUpdateFromServer> WORKING_ORDER_FILTER = WorkingOrdersPresenter::workingOrderFilter;
    public static final Predicate<WorkingOrderUpdateFromServer> NON_GTC_FILTER = WorkingOrdersPresenter::nonGTCFilter;
    private static final Pattern NIBBLER_REPLACE = Pattern.compile("nibbler-", Pattern.LITERAL);

    private final UILogger webLog;

    private final Publisher<StatsMsg> statsMsgPublisher;
    private final Publisher<Main.RemoteOrderCommandToServer> commands;

    private final Collection<String> nibblers;

    private final WebSocketViews<IWorkingOrderView> views;
    private final Map<String, WorkingOrderUpdateFromServer> workingOrders;
    private final Map<String, WorkingOrderUpdateFromServer> dirty;
    private final Map<String, SearchResult> searchResults;

    private final DecimalFormat df;

    private int numViewers;

    public WorkingOrdersPresenter(final UILogger webLog, final Scheduler scheduler, final Publisher<StatsMsg> statsMsgPublisher,
            final Publisher<Main.RemoteOrderCommandToServer> commands, final Collection<String> nibblers) {

        this.webLog = webLog;

        this.statsMsgPublisher = statsMsgPublisher;
        this.commands = commands;

        this.nibblers = nibblers;

        this.views = WebSocketViews.create(IWorkingOrderView.class, this);
        this.workingOrders = new HashMap<>();
        this.dirty = new HashMap<>();
        this.searchResults = new HashMap<>();

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS);

        this.numViewers = 0;

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

        webLog.write("workingOrders", connected, true, ++numViewers);
        final IWorkingOrderView view = views.register(connected);

        for (final String nibbler : nibblers) {
            view.addNibbler(nibbler);
        }
        for (final WorkingOrderUpdateFromServer update : workingOrders.values()) {
            publishWorkingOrderUpdate(view, update);
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {

        webLog.write("workingOrders", disconnected, false, --numViewers);
        views.unregister(disconnected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        webLog.write("workingOrders", msg);
        views.invoke(msg);
    }

    // --------------

    @FromWebSocketView
    public void shutdownAll(final WebSocketInboundData data) {

        final String user = data.getClient().getUserName();
        for (final String nibbler : nibblers) {
            shutdownOMS(nibbler, user, "Working orders - Shutdown ALL exchanges.");
        }
    }

    @FromWebSocketView
    public void shutdownExchange(final WebSocketInboundData data, final String nibbler) {

        final String user = data.getClient().getUserName();
        shutdownOMS(nibbler, user, "Working orders - Shutdown exchange.");
    }

    private void shutdownOMS(final String nibbler, final String user, final String reason) {

        final RemoteShutdownOms remoteCommand = new RemoteShutdownOms(nibbler, user, reason);
        final Main.RemoteOrderCommandToServer command = new Main.RemoteOrderCommandToServer(nibbler, remoteCommand);
        commands.publish(command);
    }

    @FromWebSocketView
    public void cancelAllNonGTC(final WebSocketInboundData data) {

        final String user = data.getClient().getUserName();
        cancelAllNoneGTC(user, "Working orders - Cancel non-gtc.");
    }

    private void cancelAllNoneGTC(final String user, final String reason) {

        workingOrders.values().stream().filter(NON_GTC_FILTER).forEach(order -> cancel(user, order));
        for (final String nibbler : nibblers) {
            stopAllStrategies(nibbler, user, reason);
        }
    }

    @FromWebSocketView
    public void cancelExchangeNonGTC(final WebSocketInboundData data, final String nibbler) {

        final String user = data.getClient().getUserName();
        workingOrders.values().stream().filter(NON_GTC_FILTER).forEach(order -> {
            if (nibbler.equals(order.fromServer)) {
                cancel(user, order);
            }
        });
        stopAllStrategies(nibbler, user, "Working orders - Cancel exchange non-gtc.");
    }

    @FromWebSocketView
    public void cancelAll(final WebSocketInboundData data) {

        final String user = data.getClient().getUserName();
        workingOrders.values().stream().forEach(order -> cancel(user, order));
        for (final String nibbler : nibblers) {
            stopAllStrategies(nibbler, user, "Working orders - Cancel ALL exchange.");
        }
    }

    @FromWebSocketView
    public void cancelExchange(final WebSocketInboundData data, final String nibbler) {

        final String user = data.getClient().getUserName();
        workingOrders.values().stream().forEach(order -> {
            if (nibbler.equals(order.fromServer)) {
                cancel(user, order);
            }
        });
        stopAllStrategies(nibbler, user, "Working orders - Cancel ALL.");
    }

    private void stopAllStrategies(final String nibbler, final String user, final String reason) {

        final RemoteStopAllStrategy stopCommand = new RemoteStopAllStrategy(nibbler, user, reason);
        final Main.RemoteOrderCommandToServer command = new Main.RemoteOrderCommandToServer(nibbler, stopCommand);
        commands.publish(command);
    }

    @FromWebSocketView
    public void cancelOrder(final String key, final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        final WorkingOrderUpdateFromServer order = workingOrders.get(key);
        if (null == order) {
            statsMsgPublisher.publish(
                    new AdvisoryStat("Reddal Working Orders", AdvisoryStat.Level.INFO, "Tried to cancel non-existent order [" + key + ']'));
        } else {
            cancel(user, order);
        }
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

        if (0 < numViewers) {
            for (final WorkingOrderUpdateFromServer workingOrderUpdate : dirty.values()) {
                publishWorkingOrderUpdate(views.all(), workingOrderUpdate);
            }
        } else {
            cancelAllNoneGTC("AUTOMATED", "Working orders - no users viewing working orders screen.");
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
            price = df.format(update.getPrice() / (double) Constants.NORMALISING_FACTOR);
        } else {
            price = update.getPrice() + " (raw)";
        }

        final String chainID = Integer.toString(order.value.getChainId());
        final String server = NIBBLER_REPLACE.matcher(order.fromServer).replaceAll("");

        view.updateWorkingOrder(order.key(), chainID, update.getSymbol(), update.getSide().toString(), price, update.getFilledQuantity(),
                update.getTotalQuantity(), update.getWorkingOrderState().toString(), update.getWorkingOrderType().toString(),
                update.getTag(), server, update.getWorkingOrderState() == WorkingOrderState.DEAD);
    }
}
