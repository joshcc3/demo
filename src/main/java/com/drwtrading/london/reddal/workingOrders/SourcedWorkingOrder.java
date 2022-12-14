package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.CancelOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.ModifyOrderCmd;
import org.jetlang.channels.Publisher;

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

    public IOrderCmd buildModify(final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final long toPrice,
            final int toQty) {

        return new ModifyOrderCmd(source, rejectChannel, user, order.getChainID(), order.getSymbol(), order.getSide(), order.getOrderType(),
                order.getAlgoType(), order.getTag(), order.getPrice(), (int) order.getOrderQty(), toPrice, toQty);
    }

    public IOrderCmd buildCancel(final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final boolean isAuto) {

        return new CancelOrderCmd(source, rejectChannel, user, isAuto, order.getChainID(), order.getSymbol());
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

        final AlgoType algoType = order.getAlgoType();
        final OrderType orderType = order.getOrderType();

        if (AlgoType.QUOTE == algoType) {
            return CSSClass.WORKING_ORDER_TYPE_QUOTE;
        } else if (OrderType.HIDDEN_LIMIT == orderType) {
            return CSSClass.WORKING_ORDER_TYPE_HIDDEN;
        } else if (OrderType.GTC == orderType) {
            return CSSClass.WORKING_ORDER_TYPE_GTC;
        } else if (OrderType.DARK_PEGGED == orderType) {
            return CSSClass.WORKING_ORDER_TYPE_DARK;
        } else if (AlgoType.HAWK == algoType) {
            return CSSClass.WORKING_ORDER_TYPE_HAWK;
        } else if (AlgoType.TAKER == algoType) {
            return CSSClass.WORKING_ORDER_TYPE_TAKER;
        } else if (OrderType.MKT_CLOSE == orderType) {
            return CSSClass.WORKING_ORDER_TYPE_MKT_CLOSE;
        } else if (OrderType.MKT_OPEN == orderType) {
            return CSSClass.WORKING_ORDER_TYPE_MKT_OPEN;
        } else if (OrderType.MARKET == orderType) {
            return CSSClass.WORKING_ORDER_TYPE_MARKET;
        } else if (AlgoType.MANUAL == algoType || AlgoType.THOR == algoType) {
            return CSSClass.WORKING_ORDER_TYPE_MANUAL;
        } else if (AlgoType.HIDDEN_TICK_TAKER == algoType) {
            return CSSClass.WORKING_ORDER_TYPE_HIDDEN_TICKTAKER;
        } else if (AlgoType.PICARD == algoType) {
            return CSSClass.WORKING_ORDER_TYPE_PICARD;
        } else {
            throw new IllegalArgumentException(
                    "Unknown CSSClass for [" + order.getSymbol() + "] for [" + algoType + "], [" + orderType + "].");
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
