package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
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

    private final UILogger webLog;

    private final Multimap<Publisher<WebSocketOutboundData>, WebSocketControlMessage> queue = HashMultimap.create();
    private final Map<Publisher<WebSocketOutboundData>, Publisher<WebSocketControlMessage>> redirects = new HashMap<>();
    private final Map<String, Publisher<WebSocketControlMessage>> shards;

    private final List<TypedChannel<WebSocketControlMessage>> pool;

    public LadderMessageRouter(final UILogger webLog, final List<TypedChannel<WebSocketControlMessage>> pool) {

        this.webLog = webLog;

        this.pool = pool;
        this.shards = new MapMaker().makeComputingMap(from -> null);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        queue.put(connected.getOutboundChannel(), connected);
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        final Publisher<WebSocketControlMessage> publisher = redirects.remove(disconnected.getOutboundChannel());
        if (publisher != null) {
            publisher.publish(disconnected);
        }
        queue.removeAll(disconnected.getOutboundChannel());
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {

        final String data = msg.getData();
        if (!data.startsWith("heartbeat")) {
            webLog.write("ladderMsgRouter", msg);
        }

        Publisher<WebSocketControlMessage> publisher = redirects.get(msg.getOutboundChannel());
        if (publisher != null) {
            publisher.publish(msg);
        } else {
            final String[] args = data.split("\0");
            final String cmd = args[0];
            if (cmd.equals("ladder-subscribe")) {
                final String symbol = args[1];
                publisher = pool.get(getShard(symbol, pool.size()));
                redirects.put(msg.getOutboundChannel(), publisher);
                final Collection<WebSocketControlMessage> queued = queue.removeAll(msg.getOutboundChannel());
                for (final WebSocketControlMessage queuedMsg : queued) {
                    publisher.publish(queuedMsg);
                }
                publisher.publish(msg);
            } else {
                queue.put(msg.getOutboundChannel(), msg);
            }
        }
    }

    public int getShard(final String s, final int n) {
        return Math.abs(HashCommon.murmurHash3(s.hashCode()) % n);
    }

}
