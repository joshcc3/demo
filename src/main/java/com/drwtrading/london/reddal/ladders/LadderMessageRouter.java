package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
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

public class LadderMessageRouter {

    private final IResourceMonitor<ReddalComponents> monitor;

    private final FiberBuilder logFiber;
    private final UILogger webLog;

    private final TypedChannel<SymbolSelection> symbolSelections;

    private final TypedChannel<WebSocketControlMessage> stackManagerLadderPresenter;
    private final Map<MDSource, TypedChannel<WebSocketControlMessage>> ladderPresenters;
    private final Map<String, Publisher<WebSocketControlMessage>> symbolPresenters;

    private final Map<String, HashSet<Publisher<WebSocketOutboundData>>> unknownSymbolSubscriptions;
    private final Map<Publisher<WebSocketOutboundData>, Publisher<WebSocketControlMessage>> redirects;
    private final Map<Publisher<WebSocketOutboundData>, LinkedList<WebSocketControlMessage>> queue;

    public LadderMessageRouter(final IResourceMonitor<ReddalComponents> monitor, final UILogger webLog,
            final TypedChannel<SymbolSelection> symbolSelections, final TypedChannel<WebSocketControlMessage> stackManagerLadderPresenter,
            final Map<MDSource, TypedChannel<WebSocketControlMessage>> ladderPresenters, final FiberBuilder logFiber) {

        this.monitor = monitor;

        this.logFiber = logFiber;
        this.webLog = webLog;

        this.symbolSelections = symbolSelections;

        this.stackManagerLadderPresenter = stackManagerLadderPresenter;
        this.ladderPresenters = ladderPresenters;
        this.symbolPresenters = new HashMap<>();

        this.unknownSymbolSubscriptions = new HashMap<>();
        this.redirects = new HashMap<>();
        this.queue = new HashMap<>();
    }

    public void setParentStackSymbol(final String symbol) {
        setLadderPresenter(symbol, stackManagerLadderPresenter);
    }

    public void setSearchResult(final SearchResult searchResult) {

        final String symbol = searchResult.symbol;
        final MDSource mdSource = searchResult.mdSource;

        final Publisher<WebSocketControlMessage> ladderPresenter = ladderPresenters.get(mdSource);

        if (null == ladderPresenter) {
            monitor.logError(ReddalComponents.LADDER_ROUTER,
                    "No market data handling exchange [" + mdSource + "] for symbol [" + symbol + "].");
        } else {
            setLadderPresenter(symbol, ladderPresenter);
        }
    }

    private void setLadderPresenter(final String symbol, final Publisher<WebSocketControlMessage> ladderPresenter) {

        symbolPresenters.put(symbol, ladderPresenter);
        final Set<Publisher<WebSocketOutboundData>> waitingChannels = unknownSymbolSubscriptions.remove(symbol);
        if (null != waitingChannels) {
            for (final Publisher<WebSocketOutboundData> outChannel : waitingChannels) {
                replayQueuedMsgs(ladderPresenter, outChannel);
            }
        }
    }

    private void replayQueuedMsgs(final Publisher<WebSocketControlMessage> ladderPresenter,
            final Publisher<WebSocketOutboundData> outChannel) {

        final Collection<WebSocketControlMessage> queued = queue.remove(outChannel);
        for (final WebSocketControlMessage queuedMsg : queued) {
            ladderPresenter.publish(queuedMsg);
        }

        redirects.put(outChannel, ladderPresenter);
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
            logFiber.execute(() -> webLog.write("ladderMsgRouter", msg));
        }

        final Publisher<WebSocketControlMessage> publisher = redirects.get(msg.getOutboundChannel());
        if (null != publisher) {
            publisher.publish(msg);
        } else {
            final String[] args = data.split("\0");
            final String cmd = args[0];
            if ("ladder-subscribe".equals(cmd)) {
                subscribeToSymbol(msg, args);
            } else {
                monitor.logError(ReddalComponents.LADDER_ROUTER, "MESSAGE RECEIVED BEFORE SUBSCRIBE [" + msg + "].");
                final List<WebSocketControlMessage> queuedMsgs = queue.get(msg.getOutboundChannel());
                queuedMsgs.add(msg);
            }
        }
    }

    private void subscribeToSymbol(final WebSocketInboundData msg, final String[] args) {

        final String symbol = args[1];
        final Publisher<WebSocketOutboundData> outChannel = msg.getOutboundChannel();
        final Publisher<WebSocketControlMessage> ladderPresenter = symbolPresenters.get(symbol);

        if (null == ladderPresenter) {

            final Set<Publisher<WebSocketOutboundData>> waitingChannels = MapUtils.getMappedSet(unknownSymbolSubscriptions, symbol);
            waitingChannels.add(outChannel);

            final List<WebSocketControlMessage> queuedMsgs = queue.get(msg.getOutboundChannel());
            queuedMsgs.add(msg);
        } else {

            replayQueuedMsgs(ladderPresenter, outChannel);
            ladderPresenter.publish(msg);

            final SymbolSelection symbolSelection = new SymbolSelection(msg.getClient().getUserName(), args);
            symbolSelections.publish(symbolSelection);
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
