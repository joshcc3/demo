package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LadderWorkspace {

    private final Publisher<ReplaceCommand> replaceCommand;
    WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    Multimap<String, View> workspacesByHost = ArrayListMultimap.create();
    Multimap<String, View> setsByHost = ArrayListMultimap.create();
    Set<View> lockedViews = new HashSet<>();
    HashMultimap<String, String> contractSets = HashMultimap.create();

    public LadderWorkspace(Publisher<ReplaceCommand> replaceCommand) {

        this.replaceCommand = replaceCommand;
    }

    @Subscribe
    public void on(WebSocketConnected webSocketConnected) {
        View view = views.register(webSocketConnected);
        workspacesByHost.put(webSocketConnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(WebSocketDisconnected webSocketDisconnected) {
        View view = views.unregister(webSocketDisconnected);
        workspacesByHost.remove(webSocketDisconnected.getClient().getHost(), view);
        lockedViews.remove(view);
        setsByHost.remove(webSocketDisconnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(WebSocketInboundData data) {
        views.invoke(data);
    }


    @Subscribe
    public void on(SpreadContractSet contractSet) {
        contractSets.put(contractSet.back, contractSet.back);
        contractSets.put(contractSet.back, contractSet.front);
        contractSets.put(contractSet.back, contractSet.spread);

        contractSets.put(contractSet.front, contractSet.back);
        contractSets.put(contractSet.front, contractSet.front);
        contractSets.put(contractSet.front, contractSet.spread);

        contractSets.put(contractSet.spread, contractSet.back);
        contractSets.put(contractSet.spread, contractSet.front);
        contractSets.put(contractSet.spread, contractSet.spread);
    }

    @FromWebSocketView
    public void setify(WebSocketInboundData data) {
        View view = views.get(data.getOutboundChannel());
        workspacesByHost.remove(data.getClient().getHost(), view);
        setsByHost.put(data.getClient().getHost(), view);
    }

    @FromWebSocketView
    public void unsetify(WebSocketInboundData data) {
        View view = views.get(data.getOutboundChannel());
        setsByHost.remove(data.getClient().getHost(), view);
        workspacesByHost.put(data.getClient().getHost(), view);
    }

    @FromWebSocketView
    public void lock(WebSocketInboundData data) {
        View view = views.get(data.getOutboundChannel());
        lockedViews.add(view);
    }


    @FromWebSocketView
    public void unlock(WebSocketInboundData data) {
        View view = views.get(data.getOutboundChannel());
        lockedViews.remove(view);
    }

    @FromWebSocketView
    public void replace(String from, String to, WebSocketInboundData data) {
        for (View view : workspacesByHost.values()) {
            view.replace(from, to);
        }
        ReplaceCommand command = new ReplaceCommand(data.getClient().getUserName(), from, to);
        replaceCommand.publish(command);
    }

    public boolean openLadderForUser(String user, String symbol) {

        boolean openedSomething = false;
        if (workspacesByHost.containsKey(user)) {
            ArrayDeque<View> viewsList = new ArrayDeque<>(workspacesByHost.get(user));
            View view;
            while ((view = viewsList.pollLast()) != null) {
                if (!lockedViews.contains(view)) {
                    view.addSymbol(symbol);
                    openedSomething = true;
                    break;
                }
            }
        }

        if (symbol.contains(";")) {
            symbol = symbol.split(";")[0];
        }

        if (setsByHost.containsKey(user)) {

            Collection<String> contractSet = contractSets.get(symbol);
            if (!contractSet.isEmpty()) {
                View view;
                ArrayDeque<View> viewsList = new ArrayDeque<>(setsByHost.get(user));
                while ((view = viewsList.pollLast()) != null) {
                    if (!lockedViews.contains(view)) {
                        HashSet<String> symbols = new HashSet<>(contractSet);
                        symbols.remove(symbol);
                        contractSet.forEach(view::addSymbol);
                        view.addSymbol(symbol);
                        openedSomething = true;
                        break;
                    }
                }
            }

        }


        return openedSomething;
    }

    public static interface View {
        public void addSymbol(String symbol);
        public void replace(String from, String to);
    }

}
