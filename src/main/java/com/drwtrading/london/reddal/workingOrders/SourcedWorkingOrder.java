package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.CancelOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.ModifyOrderCmd;

public class SourcedWorkingOrder {

    public final String source;
    public final WorkingOrder order;

    public final String uiKey;
    public final CSSClass cssClass;

    public SourcedWorkingOrder(final String source, final WorkingOrder order) {

        this.source = source;
        this.order = order;

        this.uiKey = source + '_' + order.getChainID();
        this.cssClass = getCSSClass(order);
    }

    public RemoteOrderCommandToServer buildModify(final String username, final long toPrice, final int toQty) {

        final IOrderCmd cmd = new ModifyOrderCmd(username, order.getChainID(), order.getSymbol(), order.getSide(), order.getOrderType(),
                order.getAlgoType(), order.getTag(), order.getPrice(), (int) order.getOrderQty(), toPrice, toQty);

        return new RemoteOrderCommandToServer(source, cmd);
    }

    public RemoteOrderCommandToServer buildCancel(final String username, final boolean isAuto) {

        final IOrderCmd cmd = new CancelOrderCmd(username, isAuto, order.getChainID(), order.getSymbol());
        return new RemoteOrderCommandToServer(source, cmd);
    }

    @Override
    public int hashCode() {
        return order.getWorkingOrderID();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final SourcedWorkingOrder that = (SourcedWorkingOrder) o;
            return order.getWorkingOrderID() == that.order.getWorkingOrderID() && source.equals(that.source);
        }
    }

    private static CSSClass getCSSClass(final WorkingOrder order) {

        if (order.getAlgoType() == AlgoType.QUOTE) {
            return CSSClass.WORKING_ORDER_TYPE_QUOTE;
        } else if (order.getOrderType() == OrderType.HIDDEN_LIMIT) {
            return CSSClass.WORKING_ORDER_TYPE_HIDDEN;
        } else if (order.getOrderType() == OrderType.GTC) {
            return CSSClass.WORKING_ORDER_TYPE_GTC;
        } else if (order.getOrderType() == OrderType.DARK_PEGGED) {
            return CSSClass.WORKING_ORDER_TYPE_DARK;
        } else if (order.getAlgoType() == AlgoType.HAWK) {
            return CSSClass.WORKING_ORDER_TYPE_HAWK;
        } else if (order.getAlgoType() == AlgoType.TAKER) {
            return CSSClass.WORKING_ORDER_TYPE_TAKER;
        } else if (order.getOrderType() == OrderType.MKT_CLOSE) {
            return CSSClass.WORKING_ORDER_TYPE_MKT_CLOSE;
        } else if (order.getOrderType() == OrderType.MARKET) {
            return CSSClass.WORKING_ORDER_TYPE_MARKET;
        } else if (order.getAlgoType() == AlgoType.MANUAL) {
            return CSSClass.WORKING_ORDER_TYPE_MANUAL;
        } else if (order.getAlgoType() == AlgoType.HIDDEN_TICK_TAKER) {
            return CSSClass.WORKING_ORDER_TYPE_HIDDEN_TICKTAKER;
        } else if (order.getAlgoType() == AlgoType.PICARD) {
            return CSSClass.WORKING_ORDER_TYPE_QUICKDRAW;
        } else {
            throw new IllegalArgumentException(
                    "Unknown CSSClass for [" + order.getSymbol() + "] for [" + order.getAlgoType() + "], [" + order.getOrderType() + "].");
        }
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();
        sb.append("Sourced Order: ");
        sb.append(source);
        sb.append(',');
        sb.append(' ');
        sb.append(order);
        return sb.toString();
    }
}
