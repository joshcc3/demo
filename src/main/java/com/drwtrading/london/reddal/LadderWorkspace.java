package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

public class LadderWorkspace {

    WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    Multimap<String, View> workspacesByHost = ArrayListMultimap.create();
    Multimap<String, View> setsByHost = ArrayListMultimap.create();
    Set<View> lockedViews = new HashSet<>();
    Map<String, SpreadContractSet> contractSets = new HashMap<>();

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
        contractSets.put(contractSet.back, contractSet);
        contractSets.put(contractSet.front, contractSet);
        contractSets.put(contractSet.spread, contractSet);
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

        if (setsByHost.containsKey(user) && contractSets.containsKey(symbol)) {
            SpreadContractSet contractSet = contractSets.get(symbol);
            ArrayDeque<View> viewsList = new ArrayDeque<>(setsByHost.get(user));
            View view;
            while ((view = viewsList.pollLast()) != null) {
                if (!lockedViews.contains(view)) {
                    view.addSymbol(contractSet.spread);
                    view.addSymbol(contractSet.back);
                    view.addSymbol(contractSet.front);
                    openedSomething = true;
                    break;
                }
            }
        }


        return openedSomething;
    }

    public static interface View {
        public void addSymbol(String symbol);
    }

}
