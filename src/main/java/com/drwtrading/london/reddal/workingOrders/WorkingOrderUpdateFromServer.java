package com.drwtrading.london.reddal.workingOrders;

import eeif.execution.RemoteAutoCancelOrder;
import eeif.execution.RemoteCancelOrder;
import eeif.execution.RemoteOrder;
import eeif.execution.RemoteOrderType;
import eeif.execution.WorkingOrderType;
import eeif.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.util.Struct;

public class WorkingOrderUpdateFromServer extends Struct {

    public final String fromServer;
    public final WorkingOrderUpdate value;

    public WorkingOrderUpdateFromServer(final String fromServer, final WorkingOrderUpdate value) {
        this.fromServer = fromServer;
        this.value = value;
    }

    public Main.RemoteOrderCommandToServer buildCancelCommand(String username) {
        final WorkingOrderUpdate workingOrderUpdate = this.value;
        final String orderType = getOrderType(workingOrderUpdate.getWorkingOrderType());
        final RemoteOrder remoteOrder =
                new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), workingOrderUpdate.getPrice(),
                        workingOrderUpdate.getTotalQuantity(), getRemoteOrderType(orderType), false, workingOrderUpdate.getTag());
        return new Main.RemoteOrderCommandToServer(this.fromServer,
                new RemoteCancelOrder(workingOrderUpdate.getServerName(), username, workingOrderUpdate.getChainId(), remoteOrder));
    }

    public Main.RemoteOrderCommandToServer buildAutoCancel(String username) {
        final WorkingOrderUpdate workingOrderUpdate = this.value;
        final String orderType = getOrderType(workingOrderUpdate.getWorkingOrderType());
        final RemoteOrder remoteOrder =
                new RemoteOrder(workingOrderUpdate.getSymbol(), workingOrderUpdate.getSide(), workingOrderUpdate.getPrice(),
                        workingOrderUpdate.getTotalQuantity(), getRemoteOrderType(orderType), false, workingOrderUpdate.getTag());
        return new Main.RemoteOrderCommandToServer(this.fromServer,
                new RemoteAutoCancelOrder(workingOrderUpdate.getServerName(), username, workingOrderUpdate.getChainId(), remoteOrder));
    }

    static String getOrderType(final WorkingOrderType workingOrderType) {
        if (workingOrderType == WorkingOrderType.MARKET) {
            return "MKT_CLOSE";
        } else {
            return workingOrderType.name();
        }
    }

    public RemoteOrder toRemoteOrder(final boolean autoHedge, final long price,
                                     final int totalQuantity) {

        final RemoteOrderType remoteOrderType = getRemoteOrderType(this.value.getWorkingOrderType().toString());
        return new RemoteOrder(this.value.getSymbol(), this.value.getSide(), price, totalQuantity, remoteOrderType,
                autoHedge, this.value.getTag());
    }

    public static RemoteOrderType getRemoteOrderType(final String orderType) {

        for (final RemoteOrderType remoteOrderType : RemoteOrderType.values()) {
            if (remoteOrderType.toString().toUpperCase().equals(orderType.toUpperCase())) {
                return remoteOrderType;
            }
        }
        return RemoteOrderType.MANUAL;
    }
    
    public boolean isLikelyGTC() {
        return this.fromServer.toUpperCase().contains("GTC") ||
                this.value.getTag().toUpperCase().contains("GTC") ||
                this.value.getWorkingOrderType().name().toUpperCase().contains("GTC");
    }

    public String key() {
        return fromServer + '_' + value.getChainId();
    }
}
