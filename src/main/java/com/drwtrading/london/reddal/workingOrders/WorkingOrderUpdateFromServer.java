package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.CancelOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;
import com.drwtrading.london.util.Struct;
import eeif.execution.WorkingOrderUpdate;

public class WorkingOrderUpdateFromServer extends Struct {

    public final String fromServer;
    public final WorkingOrderUpdate workingOrderUpdate;

    public WorkingOrderUpdateFromServer(final String fromServer, final WorkingOrderUpdate workingOrderUpdate) {
        this.fromServer = fromServer;
        this.workingOrderUpdate = workingOrderUpdate;
    }

    public RemoteOrderCommandToServer buildCancelCommand(final String username) {
        return buildCancel(username, false);
    }

    public RemoteOrderCommandToServer buildAutoCancel(final String username) {
        return buildCancel(username, true);
    }

    private RemoteOrderCommandToServer buildCancel(final String username, final boolean isAuto) {

        final IOrderCmd cmd = new CancelOrderCmd(username, isAuto, workingOrderUpdate.getChainId(), workingOrderUpdate.getSymbol());
        return new RemoteOrderCommandToServer(this.fromServer, cmd);
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
        return this.fromServer.toUpperCase().contains("GTC") || this.workingOrderUpdate.getTag().toUpperCase().contains("GTC") ||
                this.workingOrderUpdate.getWorkingOrderType().name().toUpperCase().contains("GTC");
    }

    public String key() {
        return fromServer + '_' + workingOrderUpdate.getChainId();
    }
}
