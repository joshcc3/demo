package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class LadderWorkspace {

    WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    Multimap<String, View> clientByUsername = ArrayListMultimap.create();
    Set<View> lockedViews = new HashSet<>();

    @Subscribe
    public void on(WebSocketConnected webSocketConnected) {
        View view = views.register(webSocketConnected);
        clientByUsername.put(webSocketConnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(WebSocketDisconnected webSocketDisconnected) {
        View view = views.unregister(webSocketDisconnected);
        clientByUsername.remove(webSocketDisconnected.getClient().getHost(), view);
        lockedViews.remove(view);
    }

    @Subscribe
    public void on(WebSocketInboundData data) {
        views.invoke(data);
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
        if (clientByUsername.containsKey(user)) {
            ArrayDeque<View> viewsList = new ArrayDeque<>(clientByUsername.get(user));
            View view;
            while ((view = viewsList.pollLast()) != null) {
                if (!lockedViews.contains(view)) {
                    view.addSymbol(symbol);
                    return true;
                }
            }
        }
        return false;
    }

    public static interface View {
        public void addSymbol(String symbol);
    }

}
