package com.drwtrading.london.reddal.orderManagement.oe;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.google.common.base.Preconditions;
import drw.eeif.eeifoe.Ack;
import drw.eeif.eeifoe.AvailableSymbol;
import drw.eeif.eeifoe.Cancel;
import drw.eeif.eeifoe.ClientHeartbeat;
import drw.eeif.eeifoe.Dead;
import drw.eeif.eeifoe.Fill;
import drw.eeif.eeifoe.OrderEntryCommand;
import drw.eeif.eeifoe.OrderEntryCommandMsg;
import drw.eeif.eeifoe.OrderEntryReply;
import drw.eeif.eeifoe.OrderEntryReplyMsg;
import drw.eeif.eeifoe.Reject;
import drw.eeif.eeifoe.ServerHeartbeat;
import drw.eeif.eeifoe.Submit;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OrderEntryClient implements PhotocolsHandler<OrderEntryReplyMsg, OrderEntryCommandMsg>, OrderEntryReply.Visitor<Void> {

    private final String thisInstance;
    private final IClock clock;
    private final String serverInstance;
    private final Fiber fiber;
    private final Publisher<OrderEntrySymbolChannel> publisher;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssues;

    private PhotocolsConnection<OrderEntryCommandMsg> connection;
    private int seqNo = 0;
    private int incomingSeqNo = -1;

    private final Map<Integer, String> seqNoToSymbol = new HashMap<>();

    public OrderEntryClient(final String thisInstance, final IClock clock, final String serverInstance, final Fiber fiber,
            final Publisher<OrderEntrySymbolChannel> publisher, final Publisher<LadderClickTradingIssue> ladderClickTradingIssues) {
        this.thisInstance = thisInstance;
        this.clock = clock;
        this.serverInstance = serverInstance;
        this.fiber = fiber;
        this.publisher = publisher;
        this.ladderClickTradingIssues = ladderClickTradingIssues;
        fiber.scheduleWithFixedDelay(this::sendHeartbeat, 1, 1, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        send(new ClientHeartbeat());
    }

    private boolean send(final OrderEntryCommand command) {
        if (null != connection) {
            final int cmdSeqNo = seqNo++;
            connection.send(new OrderEntryCommandMsg(thisInstance, serverInstance, clock.nowMilliUTC(), cmdSeqNo, command));
            if (command instanceof Submit) {
                seqNoToSymbol.put(cmdSeqNo, ((Submit) command).getOrder().getSymbol());
            } else if (command instanceof Cancel) {
                seqNoToSymbol.put(cmdSeqNo, ((Cancel) command).getOrder().getSymbol());
            }
            return true;
        }
        return false;
    }

    @Subscribe
    public void on(final OrderEntryCommandToServer cmd) {
        if (serverInstance.equals(cmd.server)) {
            send(cmd.command);
        }
    }

    @Override
    public PhotocolsConnection<OrderEntryCommandMsg> onOpen(final PhotocolsConnection<OrderEntryCommandMsg> connection) {
        Preconditions.checkArgument(this.connection == null, "Double connection");
        this.connection = connection;
        return connection;
    }

    @Override
    public void onClose(final PhotocolsConnection<OrderEntryCommandMsg> connection) {
        this.connection = null;
        incomingSeqNo = -1;
        seqNo = 0;
    }

    @Override
    public void onMessage(final PhotocolsConnection<OrderEntryCommandMsg> connection, final OrderEntryReplyMsg message) {

        final int expectedSeqNo = incomingSeqNo + 1;

        incomingSeqNo = message.getSessionSeqNo();

        Preconditions.checkArgument(serverInstance.equals(message.getFromInstance()), "Wrong instance: expecting %s, received %s, ",
                serverInstance, message.getFromInstance());
        Preconditions.checkArgument(expectedSeqNo == message.getSessionSeqNo(), "Seq no doesn't match: expected %s, received %s",
                expectedSeqNo, message.getSessionSeqNo());

        message.getMsg().accept(this);
    }

    @Override
    public void onConnectFailure() {
    }

    @Override
    public Void visitAvailableSymbol(final AvailableSymbol availableSymbol) {

        final MemoryChannel<OrderEntryCommand> channel = new MemoryChannel<>();
        final Set<ManagedOrderType> orderTypes = new HashSet<>();

        for (final ManagedOrderType managedOrderType : ManagedOrderType.values()) {

            if (availableSymbol.isLeanAllowed() || !managedOrderType.requiresLean()) {
                orderTypes.add(managedOrderType);
            }
        }

        publisher.publish(new OrderEntrySymbolChannel(availableSymbol.getSymbol(), channel, orderTypes));
        channel.subscribe(fiber, this::send);
        return null;
    }

    @Override
    public Void visitAck(final Ack msg) {
        seqNoToSymbol.remove(msg.getAckSeqNo());
        return null;
    }

    @Override
    public Void visitFill(final Fill msg) {
        return null;
    }

    @Override
    public Void visitDead(final Dead msg) {
        return null;
    }

    @Override
    public Void visitReject(final Reject msg) {
        final String symbol = seqNoToSymbol.remove(msg.getRejSeqNo());
        if (null != symbol) {
            ladderClickTradingIssues.publish(new LadderClickTradingIssue(symbol, msg.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitServerHeartbeat(final ServerHeartbeat msg) {
        return null;
    }

}
