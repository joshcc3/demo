package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.CancelOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.ModifyOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;
import com.drwtrading.london.util.Struct;
import eeif.execution.Side;
import eeif.execution.WorkingOrderType;
import eeif.execution.WorkingOrderUpdate;

public class WorkingOrderUpdateFromServer extends Struct {

    public final String fromServer;
    public final WorkingOrderUpdate workingOrderUpdate;

    public WorkingOrderUpdateFromServer(final String fromServer, final WorkingOrderUpdate workingOrderUpdate) {
        this.fromServer = fromServer;
        this.workingOrderUpdate = workingOrderUpdate;
    }

    public RemoteOrderCommandToServer buildModify(final String username, final long toPrice, final int toQty) {

        final BookSide side = Side.BID == workingOrderUpdate.getSide() ? BookSide.BID : BookSide.ASK;
        final RemoteOrderType orderType = getOrderType(workingOrderUpdate.getWorkingOrderType());

        final IOrderCmd cmd = new ModifyOrderCmd(username, workingOrderUpdate.getChainId(), workingOrderUpdate.getSymbol(), side, orderType,
                workingOrderUpdate.getTag(), workingOrderUpdate.getPrice(), workingOrderUpdate.getTotalQuantity(), toPrice, toQty);

        return new RemoteOrderCommandToServer(this.fromServer, cmd);
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

    private static RemoteOrderType getOrderType(final WorkingOrderType workingOrderType) {

        if (WorkingOrderType.MARKET == workingOrderType) {
            return RemoteOrderType.MKT_CLOSE;
        } else {
            return getRemoteOrderType(workingOrderType.name());
        }
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
                this.workingOrderUpdate.getTag().toUpperCase().contains("GTC") ||
                this.workingOrderUpdate.getWorkingOrderType().name().toUpperCase().contains("GTC");
    }

    public String key() {
        return fromServer + '_' + workingOrderUpdate.getChainId();
    }
}
