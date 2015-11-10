package com.drwtrading.london.reddal.eeifoe;

import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.photons.eeifoe.BookParameters;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.ClientHeartbeat;
import com.drwtrading.london.photons.eeifoe.CommandMsg;
import com.drwtrading.london.photons.eeifoe.EventMsg;
import com.drwtrading.london.photons.eeifoe.Order;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommand;
import com.drwtrading.london.photons.eeifoe.OrderParameters;
import com.drwtrading.london.photons.eeifoe.OrderSide;
import com.drwtrading.london.photons.eeifoe.PegPriceToTheoOnSubmit;
import com.drwtrading.london.photons.eeifoe.QuotingParameters;
import com.drwtrading.london.photons.eeifoe.Submit;
import com.drwtrading.london.photons.eeifoe.TakingParameters;
import com.drwtrading.london.photons.eeifoe.TheoPrice;
import com.drwtrading.london.protocols.photon.execution.Side;
import com.drwtrading.london.util.Struct;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.drwtrading.photocols.nio.PhotocolsNioClient;
import com.google.common.base.Preconditions;
import org.jetlang.fibers.Fiber;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OrderEntryClient implements PhotocolsHandler<EventMsg, CommandMsg>, EeifOrderCommandHandler {

    final String thisInstance;
    final String toInstance;
    final IClock clock;
    final PhotocolsNioClient<EventMsg, CommandMsg> client;
    PhotocolsConnection<CommandMsg> connection = null;
    long seqNo = 0;
    long lastReceivedMillisUTC = 0;

    Map<Integer, OrderState> orderMap = new HashMap<>();

    public OrderEntryClient(String thisInstance, String toInstance, Fiber photocolsFiber, SocketAddress address, File logDir, IClock clock) throws IOException {
        this.thisInstance = thisInstance;
        this.toInstance = toInstance;
        this.clock = clock;
        client = PhotocolsNioClient.client(
                address,
                "0.0.0.0",
                EventMsg.class, CommandMsg.class, photocolsFiber,
                (t, e) -> {
                })
                .reconnectMillis(5000)
                .logFile(new File(logDir, "order-entry." + toInstance + ".log"), photocolsFiber)
                .handler(this);
        photocolsFiber.execute(() -> {
            System.out.println("\tstarting eeif-oe to " + toInstance + " " + address.toString());
            client.start();
        });
        photocolsFiber.scheduleWithFixedDelay(this::sendHeartbeat, 1, 1, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        if (null != connection) {
            send(new ClientHeartbeat());
        }
    }

    private void send(OrderEntryCommand msg) {
        if (null != connection) {
            connection.send(new CommandMsg(seqNo++, clock.nowMilliUTC(), thisInstance, toInstance, msg));
        } else {
            System.out.println("Reject: not connected");
        }
    }

    @Override
    public PhotocolsConnection<CommandMsg> onOpen(PhotocolsConnection<CommandMsg> connection) {
        Preconditions.checkArgument(this.connection == null, "Can't connect to two order entry servers with Photocols client");
        this.connection = connection;
        seqNo = 0;
        return connection;
    }

    @Override
    public void onConnectFailure() {
    }

    @Override
    public void onClose(PhotocolsConnection<CommandMsg> connection) {
        Preconditions.checkArgument(null != connection, "Unknown connection closed");
        this.connection = null;
        seqNo = 0;
    }

    @Override
    public void onMessage(PhotocolsConnection<CommandMsg> connection, EventMsg message) {
        lastReceivedMillisUTC = clock.nowMilliUTC();
        System.out.println(message);
    }

    @Override
    public void on(CancelEeifOrder cancelEeifOrder) {
        OrderState orderState = orderMap.get(cancelEeifOrder.orderId);
        if (null != orderState && toInstance.equals(cancelEeifOrder.getServer())) {
            send(orderState.getCancel());
        } else {
            throw new IllegalArgumentException("Received cancel EEIF order for " + toInstance + ", " + cancelEeifOrder);
        }
    }

    @Override
    public void on(SubmitEeifOrder submitEeifOrder) {
        OrderState orderState = new OrderState(submitEeifOrder.order);
        if (toInstance.equals(submitEeifOrder.getServer()) && null == orderMap.put(submitEeifOrder.order.orderId, orderState)) {
            send(orderState.getSubmit());
            return;
        }
        throw new IllegalArgumentException("Received submit EEIF order for " + toInstance + ", " + submitEeifOrder);
    }

    public enum EeifOrderType {
        NEW_HAWK {
            @Override
            public Order getOrder(EeifOrder eeifOrder) {
                return new Order(eeifOrder.orderId,
                        eeifOrder.symbol,
                        eeifOrder.side == Side.BID ? OrderSide.BUY : OrderSide.SELL,
                        new TheoPrice(100, 5, 10, new PegPriceToTheoOnSubmit(eeifOrder.price)),
                        new BookParameters(true, true, false, true, true),
                        new OrderParameters(eeifOrder.qty),
                        new TakingParameters(false, 0, 0, 0),
                        new QuotingParameters(true, 0, 0, 1, 0, 0, eeifOrder.qty / 2, 2, 0, 1),
                        eeifOrder.user, eeifOrder.tag);
            }
        },
        NEW_TAKER {
            @Override
            public Order getOrder(EeifOrder eeifOrder) {
                return new Order(eeifOrder.orderId,
                        eeifOrder.symbol,
                        eeifOrder.side == Side.BID ? OrderSide.BUY : OrderSide.SELL,
                        new TheoPrice(100, 5, 10, new PegPriceToTheoOnSubmit(eeifOrder.price)),
                        new BookParameters(true, true, false, true, true),
                        new OrderParameters(eeifOrder.qty),
                        new TakingParameters(true, 0, 20, 10),
                        new QuotingParameters(false, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        eeifOrder.user, eeifOrder.tag);
            }
        };

        public abstract Order getOrder(EeifOrder eeifOrder);
    }


    public static class OrderState extends Struct {
        public final EeifOrder order;
        public int commandId;

        public OrderState(EeifOrder order) {
            this.order = order;
        }

        public Submit getSubmit() {
            return new Submit(commandId++, order.order);
        }

        public Cancel getCancel() {
            return new Cancel(commandId++, order.orderId, order.symbol, order.order.getSide());
        }
    }

}
