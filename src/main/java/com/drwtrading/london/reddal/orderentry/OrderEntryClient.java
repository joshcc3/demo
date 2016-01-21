package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.photons.eeifoe.Ack;
import com.drwtrading.london.photons.eeifoe.AvailableSymbol;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.ClientHeartbeat;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommand;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommandMsg;
import com.drwtrading.london.photons.eeifoe.OrderEntryReply;
import com.drwtrading.london.photons.eeifoe.OrderEntryReplyMsg;
import com.drwtrading.london.photons.eeifoe.Reject;
import com.drwtrading.london.photons.eeifoe.ServerHeartbeat;
import com.drwtrading.london.photons.eeifoe.Submit;
import com.drwtrading.london.reddal.LadderClickTradingIssue;
import com.drwtrading.london.util.Struct;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.google.common.base.Preconditions;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OrderEntryClient implements PhotocolsHandler<OrderEntryReplyMsg, OrderEntryCommandMsg>, OrderEntryReply.Visitor<Void> {

    private final String thisInstance;
    private final IClock clock;
    private final String serverInstance;
    private final Fiber fiber;
    private final Publisher<SymbolOrderChannel> publisher;
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssues;

    private PhotocolsConnection<OrderEntryCommandMsg> connection;
    private int seqNo = 0;
    private int incomingSeqNo = -1;

    private Map<Integer, String> seqNoToSymbol = new HashMap<>();

    public OrderEntryClient(String thisInstance, IClock clock, String serverInstance, Fiber fiber, Publisher<SymbolOrderChannel> publisher, Publisher<LadderClickTradingIssue> ladderClickTradingIssues) {
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

    private boolean send(OrderEntryCommand command) {
        if (null != connection) {
            int cmdSeqNo = seqNo++;
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
    public void on(OrderEntryCommandToServer cmd) {
        if (serverInstance.equals(cmd.server)) {
            send(cmd.command);
        }
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
        MemoryChannel<OrderEntryCommand> channel = new MemoryChannel<>();
        publisher.publish(new SymbolOrderChannel(availableSymbol.getSymbol(), channel));
        channel.subscribe(fiber, this::send);
        return null;
    }

    @Override
    public Void visitAck(Ack msg) {
        seqNoToSymbol.remove(msg.getAckSeqNo());
        return null;
    }

    @Override
    public Void visitReject(Reject msg) {
        String symbol = seqNoToSymbol.remove(msg.getRejSeqNo());
        if (null != symbol) {
            ladderClickTradingIssues.publish(new LadderClickTradingIssue(symbol, msg.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitServerHeartbeat(ServerHeartbeat msg) {
        return null;
    }

    public static class SymbolOrder extends Struct {
        public final String symbol;

        public SymbolOrder(String symbol) {
            this.symbol = symbol;
        }
    }

    public static class SymbolOrderChannel extends Struct {
        public final String symbol;
        public final Publisher<OrderEntryCommand> publisher;

        public SymbolOrderChannel(String symbol, Publisher<OrderEntryCommand> publisher) {
            this.symbol = symbol;
            this.publisher = publisher;
        }
    }

}
