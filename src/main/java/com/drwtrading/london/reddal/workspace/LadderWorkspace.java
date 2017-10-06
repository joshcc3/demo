package com.drwtrading.london.reddal.workspace;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.ReplaceCommand;
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
    private final WebSocketViews<WorkspaceView> views = new WebSocketViews<>(WorkspaceView.class, this);
    private final Multimap<String, WorkspaceView> workspacesByHost = ArrayListMultimap.create();
    private final Multimap<String, WorkspaceView> setsByHost = ArrayListMultimap.create();
    private final Set<WorkspaceView> lockedViews = new HashSet<>();

    private final Map<String, SpreadContractSet> contractSets;

    public LadderWorkspace(final UILogger webLog, final Publisher<ReplaceCommand> replaceCommand) {

        this.webLog = webLog;

        this.replaceCommand = replaceCommand;

        this.contractSets = new HashMap<>();
    }

    @Subscribe
    public void on(final WebSocketConnected webSocketConnected) {
        final WorkspaceView view = views.register(webSocketConnected);
        workspacesByHost.put(webSocketConnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(final WebSocketDisconnected webSocketDisconnected) {
        final WorkspaceView view = views.unregister(webSocketDisconnected);
        workspacesByHost.remove(webSocketDisconnected.getClient().getHost(), view);
        lockedViews.remove(view);
        setsByHost.remove(webSocketDisconnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(final WebSocketInboundData msg) {
        webLog.write("LadderWorkspace", msg);
        views.invoke(msg);
    }

    public void setContractSet(final SpreadContractSet contractSet) {

        contractSets.put(contractSet.symbol, contractSet);

        contractSets.putIfAbsent(contractSet.spread, contractSet);
        contractSets.putIfAbsent(contractSet.backMonth, contractSet);
    }

    @FromWebSocketView
    public void setify(final WebSocketInboundData data) {
        final WorkspaceView view = views.get(data.getOutboundChannel());
        workspacesByHost.remove(data.getClient().getHost(), view);
        setsByHost.put(data.getClient().getHost(), view);
    }

    @FromWebSocketView
    public void unsetify(final WebSocketInboundData data) {
        final WorkspaceView view = views.get(data.getOutboundChannel());
        setsByHost.remove(data.getClient().getHost(), view);
        workspacesByHost.put(data.getClient().getHost(), view);
    }

    @FromWebSocketView
    public void lock(final WebSocketInboundData data) {
        final WorkspaceView view = views.get(data.getOutboundChannel());
        lockedViews.add(view);
    }

    @FromWebSocketView
    public void unlock(final WebSocketInboundData data) {
        final WorkspaceView view = views.get(data.getOutboundChannel());
        lockedViews.remove(view);
    }

    @FromWebSocketView
    public void replace(final String from, final String to, final WebSocketInboundData data) {
        for (final WorkspaceView view : workspacesByHost.values()) {
            view.replace(from, to);
        }
        final ReplaceCommand command = new ReplaceCommand(data.getClient().getUserName(), from, to);
        replaceCommand.publish(command);
    }

    public void openLadder(final HostWorkspaceRequest userRequest) {
        openLadderForUser(userRequest.host, userRequest.symbol);
    }

    public boolean openLadderForUser(final String user, final String ladderSymbol) {

        boolean openedWorkspace = false;
        if (workspacesByHost.containsKey(user)) {

            final ArrayDeque<WorkspaceView> viewsList = new ArrayDeque<>(workspacesByHost.get(user));

            while (!openedWorkspace && !viewsList.isEmpty()) {

                final WorkspaceView view = viewsList.pollLast();

                if (!lockedViews.contains(view)) {
                    view.addSymbol(ladderSymbol);
                    openedWorkspace = true;
                }
            }
        }

        final String symbol;
        if (ladderSymbol.contains(";")) {
            symbol = ladderSymbol.split(";")[0];
        } else {
            symbol = ladderSymbol;
        }

        final SpreadContractSet contractSet = contractSets.get(symbol);
        final Collection<WorkspaceView> views = setsByHost.get(user);

        boolean openedSpreadWorkspace = false;
        if (null != views && null != contractSet) {

            final ArrayDeque<WorkspaceView> viewsList = new ArrayDeque<>(views);

            while (!openedSpreadWorkspace && !viewsList.isEmpty()) {

                final WorkspaceView view = viewsList.pollLast();

                if (!lockedViews.contains(view)) {

                    view.addSymbol(contractSet.backMonth);
                    view.addSymbol(contractSet.spread);

                    if (null != contractSet.leanSymbol) {
                        view.addSymbol(contractSet.leanSymbol);
                    }

                    if (null != contractSet.parentSymbol) {
                        view.addSymbol(contractSet.parentSymbol);
                    } else if (null != contractSet.stackSymbol) {
                        view.addSymbol(contractSet.stackSymbol);
                    }

                    view.addSymbol(contractSet.symbol);
                    view.addSymbol(ladderSymbol);
                    openedSpreadWorkspace = true;
                }
            }
        }
        return openedWorkspace || openedSpreadWorkspace;
    }
}
