package com.drwtrading.london.reddal.ladders.orders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderPresenterMsgRouter {

    private static final Pattern QUOTE_REMOVER = Pattern.compile("\"", Pattern.LITERAL);
    private final IResourceMonitor<ReddalComponents> monitor;

    private final UILogger webLog;

    private final Map<MDSource, TypedChannel<WebSocketControlMessage>> presenters;
    private final Map<String, Publisher<WebSocketControlMessage>> symbolPresenters;

    private final Map<String, HashSet<Publisher<WebSocketOutboundData>>> unknownSymbolSubscriptions;
    private final Map<Publisher<WebSocketOutboundData>, Publisher<WebSocketControlMessage>> redirects;
    private final Map<Publisher<WebSocketOutboundData>, LinkedList<WebSocketControlMessage>> queue;

    public OrderPresenterMsgRouter(final IResourceMonitor<ReddalComponents> monitor, final UILogger webLog,
            final Map<MDSource, TypedChannel<WebSocketControlMessage>> presenters) {

        this.monitor = monitor;

        this.webLog = webLog;

        this.presenters = presenters;
        this.symbolPresenters = new HashMap<>();

        this.unknownSymbolSubscriptions = new HashMap<>();
        this.redirects = new HashMap<>();
        this.queue = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        final String symbol = searchResult.symbol;
        final MDSource mdSource = searchResult.mdSource;

        final Publisher<WebSocketControlMessage> presenter = presenters.get(mdSource);

        if (null == presenter) {
            monitor.logError(ReddalComponents.ORDER_PRESENTER,
                    "No market data handling exchange [" + mdSource + "] for symbol [" + symbol + "].");
        } else {
            setPresenter(symbol, presenter);
        }
    }

    private void setPresenter(final String symbol, final Publisher<WebSocketControlMessage> presenter) {

        symbolPresenters.put(symbol, presenter);
        final Set<Publisher<WebSocketOutboundData>> waitingChannels = unknownSymbolSubscriptions.remove(symbol);
        if (null != waitingChannels) {
            for (final Publisher<WebSocketOutboundData> outChannel : waitingChannels) {
                replayQueuedMsgs(presenter, outChannel);
            }
        }
    }

    private void replayQueuedMsgs(final Publisher<WebSocketControlMessage> presenter, final Publisher<WebSocketOutboundData> outChannel) {

        final Collection<WebSocketControlMessage> queued = queue.remove(outChannel);
        for (final WebSocketControlMessage queuedMsg : queued) {
            presenter.publish(queuedMsg);
        }

        redirects.put(outChannel, presenter);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final List<WebSocketControlMessage> queuedMsgs = MapUtils.getMappedLinkedList(queue, connected.getOutboundChannel());
        queuedMsgs.add(connected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {

        final String data = msg.getData();
        if (!data.startsWith("heartbeat")) {
            webLog.write("orderPresenterRouter", msg);
        }

        final Publisher<WebSocketControlMessage> publisher = redirects.get(msg.getOutboundChannel());
        if (null != publisher) {
            publisher.publish(msg);
        } else {
            final String[] args = data.split(",");
            final String cmd = args[0];
            if ("subscribe".equals(cmd)) {
                subscribeToSymbol(msg, args);
            } else {
                monitor.logError(ReddalComponents.SHREDDER_ROUTER, "MESSAGE RECEIVED BEFORE SUBSCRIBE [" + msg + "].");
                final List<WebSocketControlMessage> queuedMsgs = queue.get(msg.getOutboundChannel());
                queuedMsgs.add(msg);
            }
        }
    }

    private void subscribeToSymbol(final WebSocketInboundData msg, final String[] args) {

        final String symbol = QUOTE_REMOVER.matcher(args[1]).replaceAll(Matcher.quoteReplacement(""));
        final Publisher<WebSocketOutboundData> outChannel = msg.getOutboundChannel();
        final Publisher<WebSocketControlMessage> presenter = symbolPresenters.get(symbol);

        if (null == presenter) {

            final Set<Publisher<WebSocketOutboundData>> waitingChannels = MapUtils.getMappedSet(unknownSymbolSubscriptions, symbol);
            waitingChannels.add(outChannel);

            final List<WebSocketControlMessage> queuedMsgs = queue.get(msg.getOutboundChannel());
            queuedMsgs.add(msg);
        } else {
            replayQueuedMsgs(presenter, outChannel);
            presenter.publish(msg);
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {

        final Publisher<WebSocketControlMessage> publisher = redirects.remove(disconnected.getOutboundChannel());
        if (null == publisher) {

            final Publisher<WebSocketOutboundData> outChannel = disconnected.getOutboundChannel();
            queue.remove(outChannel);

            final Iterator<HashSet<Publisher<WebSocketOutboundData>>> unknownSubscriptionIterator =
                    unknownSymbolSubscriptions.values().iterator();

            while (unknownSubscriptionIterator.hasNext()) {
                final Set<Publisher<WebSocketOutboundData>> waitingChannel = unknownSubscriptionIterator.next();
                waitingChannel.remove(outChannel);
            }
        } else {
            publisher.publish(disconnected);
        }
    }
}
