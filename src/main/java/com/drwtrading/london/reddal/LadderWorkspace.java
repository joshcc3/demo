package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LadderWorkspace {

    WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    Multimap<String, View> clientByUsername = HashMultimap.create();

    @Subscribe
    public void on(WebSocketConnected webSocketConnected) {
        View view = views.register(webSocketConnected);
        clientByUsername.put(webSocketConnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(WebSocketDisconnected webSocketDisconnected) {
        View view = views.unregister(webSocketDisconnected);
        clientByUsername.remove(webSocketDisconnected.getClient().getHost(), view);
    }

    @Subscribe
    public void on(WebSocketInboundData data) {
        views.invoke(data);
    }


    public boolean openLadderForUser(String user, String symbol) {

        if (clientByUsername.containsKey(user)) {
            clientByUsername.get(user).iterator().next().addSymbol(symbol);
            return true;
        }
        return false;
    }

    public static interface View {
        public void addSymbol(String symbol);
    }

}
