package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.photons.eeifoe.*;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.util.concurrent.TimeUnit;

public class OrderEntryClient implements PhotocolsHandler<OrderEntryReplyMsg, OrderEntryCommandMsg>, OrderEntryReply.Visitor<Void> {

    private final HashMultimap<SymbolOrder, AvailableSymbol> tradableMap = HashMultimap.create();
    private final String thisInstance;
    private final IClock clock;
    private final String serverInstance;
    private final Fiber fiber;
    private final Publisher<SymbolOrderChannel> publisher;

    private PhotocolsConnection<OrderEntryCommandMsg> connection;
    private int seqNo = 0;
    private int incomingSeqNo = -1;

    public OrderEntryClient(String thisInstance, IClock clock, String serverInstance, Fiber fiber, Publisher<SymbolOrderChannel> publisher) {
        this.thisInstance = thisInstance;
        this.clock = clock;
        this.serverInstance = serverInstance;
        this.fiber = fiber;
        this.publisher = publisher;
        fiber.scheduleWithFixedDelay(this::sendHeartbeat, 1, 1, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        send(new ClientHeartbeat());
    }

    private boolean send(OrderEntryCommand command) {
        if (null != connection) {
            connection.send(new OrderEntryCommandMsg(thisInstance, serverInstance, clock.nowMilliUTC(), seqNo++, command));
            return true;
        }
        return false;
    }

    @Override
    public PhotocolsConnection<OrderEntryCommandMsg> onOpen(PhotocolsConnection<OrderEntryCommandMsg> connection) {
        Preconditions.checkArgument(this.connection == null, "Double connection");
        this.connection = connection;
        return connection;
    }

    @Override
    public void onClose(PhotocolsConnection<OrderEntryCommandMsg> connection) {
        this.connection = null;
        incomingSeqNo = -1;
        seqNo = 0;
        tradableMap.clear();
    }

    @Override
    public void onMessage(PhotocolsConnection<OrderEntryCommandMsg> connection, OrderEntryReplyMsg message) {
        int expectedSeqNo = incomingSeqNo + 1;
        incomingSeqNo = message.getSessionSeqNo();
        long delayMs = clock.nowMilliUTC() - message.getMillisUtc();
        Preconditions.checkArgument(serverInstance.equals(message.getFromInstance()), "Wrong instance: expecting %s, received %s, ", serverInstance, message.getFromInstance());
        Preconditions.checkArgument(expectedSeqNo == message.getSessionSeqNo(), "Seq no doesn't match: expected %s, received %s", expectedSeqNo, message.getSessionSeqNo());
        Preconditions.checkArgument(delayMs < 1000L, "Message delayed %d ms", delayMs);
        message.getMsg().accept(this);
    }

    @Override
    public void onConnectFailure() {
    }

    @Override
    public Void visitAvailableSymbol(AvailableSymbol availableSymbol) {
        availableSymbol.getOrderTypes().forEach(remoteOrderType -> {
            SymbolOrder key = new SymbolOrder(availableSymbol.getSymbol(), remoteOrderType);
            tradableMap.put(key, availableSymbol);
            MemoryChannel<OrderEntryCommand> channel = new MemoryChannel<>();
            publisher.publish(new SymbolOrderChannel(key, channel));
            channel.subscribe(fiber, this::send);
        });
        System.out.println("availableSymbol = " + availableSymbol);
        return null;
    }

    @Override
    public Void visitAck(Ack msg) {
        System.out.println("ACK " + msg);
        return null;
    }

    @Override
    public Void visitReject(Reject msg) {
        System.out.println("REJ " + msg);
        return null;
    }

    @Override
    public Void visitServerHeartbeat(ServerHeartbeat msg) {
        return null;
    }

    public static class SymbolOrder {
        public final String symbol;
        public final RemoteOrderType orderType;

        public SymbolOrder(String symbol, RemoteOrderType orderType) {
            this.symbol = symbol;
            this.orderType = orderType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            SymbolOrder that = (SymbolOrder) o;

            if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
            return orderType == that.orderType;

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
            result = 31 * result + (orderType != null ? orderType.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("SymbolOrder{");
            sb.append("symbol='").append(symbol).append('\'');
            sb.append(", orderType=").append(orderType);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class SymbolOrderChannel {
        public final SymbolOrder symbolOrder;
        public final Publisher<OrderEntryCommand> publisher;

        public SymbolOrderChannel(SymbolOrder symbolOrder, Publisher<OrderEntryCommand> publisher) {
            this.symbolOrder = symbolOrder;
            this.publisher = publisher;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SymbolOrderChannel that = (SymbolOrderChannel) o;

            if (symbolOrder != null ? !symbolOrder.equals(that.symbolOrder) : that.symbolOrder != null) return false;
            return !(publisher != null ? !publisher.equals(that.publisher) : that.publisher != null);

        }

        @Override
        public int hashCode() {
            int result = symbolOrder != null ? symbolOrder.hashCode() : 0;
            result = 31 * result + (publisher != null ? publisher.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("SymbolOrderChannel{");
            sb.append("symbolOrder=").append(symbolOrder);
            sb.append(", publisher=").append(publisher);
            sb.append('}');
            return sb.toString();
        }
    }

}
