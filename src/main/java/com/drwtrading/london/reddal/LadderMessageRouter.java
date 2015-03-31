package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LadderMessageRouter {

    final Multimap<Publisher<WebSocketOutboundData>, WebSocketControlMessage> queue = HashMultimap.create();
    final Map<Publisher<WebSocketOutboundData>, Publisher<WebSocketControlMessage>> redirects = new HashMap<>();
    final Map<String, Publisher<WebSocketControlMessage>> shards;
    private final List<TypedChannel<WebSocketControlMessage>> pool;

    public LadderMessageRouter(final List<TypedChannel<WebSocketControlMessage>> pool) {
        this.pool = pool;
        shards = new MapMaker().makeComputingMap(new Function<String, Publisher<WebSocketControlMessage>>() {
            @Override
            public Publisher<WebSocketControlMessage> apply(final String from) {
                return null;
            }
        });
    }

    @Subscribe
    public void onConnected(WebSocketConnected connected) {
        queue.put(connected.getOutboundChannel(), connected);
    }

    @Subscribe
    public void onDisconnected(WebSocketDisconnected disconnected) {
        Publisher<WebSocketControlMessage> publisher = redirects.remove(disconnected.getOutboundChannel());
        if (publisher != null) {
            publisher.publish(disconnected);
        }
        queue.removeAll(disconnected.getOutboundChannel());
    }

    @Subscribe
    public void onMessage(WebSocketInboundData msg) {
        Publisher<WebSocketControlMessage> publisher = redirects.get(msg.getOutboundChannel());
        if (publisher != null) {
            publisher.publish(msg);
        } else {
            String data = msg.getData();
            String[] args = data.split("\0");
            String cmd = args[0];
            if (cmd.equals("ladder-subscribe")) {
                final String symbol = args[1];
                publisher = pool.get(getShard(symbol, pool.size()));
                redirects.put(msg.getOutboundChannel(), publisher);
                Collection<WebSocketControlMessage> queued = queue.removeAll(msg.getOutboundChannel());
                for (WebSocketControlMessage queuedMsg : queued) {
                    publisher.publish(queuedMsg);
                }
                publisher.publish(msg);
            } else {
                queue.put(msg.getOutboundChannel(), msg);
            }
        }
    }

    public int getShard(String s, int n) {
        return Math.abs(HashCommon.murmurHash3(s.hashCode()) % n);
    }

}
