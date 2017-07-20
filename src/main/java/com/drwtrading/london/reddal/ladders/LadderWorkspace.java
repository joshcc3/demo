package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.ReplaceCommand;
import com.drwtrading.london.reddal.SpreadContractSet;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LadderWorkspace {

    private final UILogger webLog;

    private final Publisher<ReplaceCommand> replaceCommand;
    private final WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    private final Multimap<String, View> workspacesByHost = ArrayListMultimap.create();
    private final Multimap<String, View> setsByHost = ArrayListMultimap.create();
    private final Set<View> lockedViews = new HashSet<>();

    private final Map<String, SpreadContractSet> contractSets;

    public LadderWorkspace(final UILogger webLog, final Publisher<ReplaceCommand> replaceCommand) {

        this.webLog = webLog;

        this.replaceCommand = replaceCommand;

        this.contractSets = new HashMap<>();
    }

    @Subscribe
    public void on(final WebSocketConnected webSocketConnected) {
        final View view = views.register(webSocketConnected);
        workspacesByHost.put(webSocketConnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(final WebSocketDisconnected webSocketDisconnected) {
        final View view = views.unregister(webSocketDisconnected);
        workspacesByHost.remove(webSocketDisconnected.getClient().getHost(), view);
        lockedViews.remove(view);
        setsByHost.remove(webSocketDisconnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(final WebSocketInboundData msg) {
        webLog.write("LadderWorkspace", msg);
        views.invoke(msg);
    }

    @Subscribe
    public void on(final SpreadContractSet contractSet) {

        contractSets.put(contractSet.front, contractSet);

        contractSets.putIfAbsent(contractSet.spread, contractSet);
        contractSets.putIfAbsent(contractSet.back, contractSet);
    }

    @FromWebSocketView
    public void setify(final WebSocketInboundData data) {
        final View view = views.get(data.getOutboundChannel());
        workspacesByHost.remove(data.getClient().getHost(), view);
        setsByHost.put(data.getClient().getHost(), view);
    }

    @FromWebSocketView
    public void unsetify(final WebSocketInboundData data) {
        final View view = views.get(data.getOutboundChannel());
        setsByHost.remove(data.getClient().getHost(), view);
        workspacesByHost.put(data.getClient().getHost(), view);
    }

    @FromWebSocketView
    public void lock(final WebSocketInboundData data) {
        final View view = views.get(data.getOutboundChannel());
        lockedViews.add(view);
    }

    @FromWebSocketView
    public void unlock(final WebSocketInboundData data) {
        final View view = views.get(data.getOutboundChannel());
        lockedViews.remove(view);
    }

    @FromWebSocketView
    public void replace(final String from, final String to, final WebSocketInboundData data) {
        for (final View view : workspacesByHost.values()) {
            view.replace(from, to);
        }
        final ReplaceCommand command = new ReplaceCommand(data.getClient().getUserName(), from, to);
        replaceCommand.publish(command);
    }

    public boolean openLadderForUser(final String user, String symbol) {

        boolean openedWorkspace = false;
        if (workspacesByHost.containsKey(user)) {

            final ArrayDeque<View> viewsList = new ArrayDeque<>(workspacesByHost.get(user));

            while (!openedWorkspace && !viewsList.isEmpty()) {

                final View view = viewsList.pollLast();

                if (!lockedViews.contains(view)) {
                    view.addSymbol(symbol);
                    openedWorkspace = true;
                }
            }
        }

        if (symbol.contains(";")) {
            symbol = symbol.split(";")[0];
        }

        final SpreadContractSet contractSet = contractSets.get(symbol);
        final Collection<View> views = setsByHost.get(user);

        boolean openedSpreadWorkspace = false;
        if (null != views && null != contractSet) {

            final ArrayDeque<View> viewsList = new ArrayDeque<>(views);


            while (!openedSpreadWorkspace && !viewsList.isEmpty()) {

                final View view = viewsList.pollLast();

                if (!lockedViews.contains(view)) {
                    view.addSymbol(contractSet.back);
                    view.addSymbol(contractSet.spread);
                    view.addSymbol(contractSet.front);
                    openedSpreadWorkspace = true;
                }
            }
        }

        return openedWorkspace || openedSpreadWorkspace;
    }

    public static interface View {

        public void addSymbol(String symbol);

        public void replace(String from, String to);
    }

}
