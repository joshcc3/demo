package com.drwtrading.london.reddal.autopull.autopuller.rules;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.util.Struct;
import org.jetlang.channels.Publisher;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class PullRule extends Struct {

    private static final AtomicLong SUB_ID = new AtomicLong(0);

    public final long ruleID;

    public final String orderSymbol;
    public final OrderSelectionPriceRangeSelection orderSelection;

    public final String mdSymbol;
    public final MktConditionQtyAtPriceCondition mktCondition;

    public PullRule(final long ruleID, final String orderSymbol, final OrderSelectionPriceRangeSelection orderSelection,
            final String mdSymbol, final MktConditionQtyAtPriceCondition mktCondition) {

        this.ruleID = ruleID;

        this.orderSymbol = orderSymbol;
        this.orderSelection = orderSelection;

        this.mdSymbol = mdSymbol;
        this.mktCondition = mktCondition;
    }

    public List<IOrderCmd> getPullCmds(final Publisher<LadderClickTradingIssue> rejectChannel, final String username,
            final Set<SourcedWorkingOrder> workingOrders, final IBook<?> book) {

        if (!workingOrders.isEmpty() && isBookValid(book) && mktCondition.conditionMet(book)) {

            final List<IOrderCmd> result = new ArrayList<>();

            for (final SourcedWorkingOrder sourcedOrder : workingOrders) {

                if (isOrderSelectable(sourcedOrder.order, book) && orderSelection.isSelectionMet(sourcedOrder.order)) {

                    final IOrderCmd cmd = sourcedOrder.buildCancel(rejectChannel, username, true);
                    result.add(cmd);
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private static boolean isBookValid(final IBook<?> book) {

        return book.isValid() && BookMarketState.CONTINUOUS == book.getStatus();
    }

    private static boolean isOrderSelectable(final WorkingOrder order, final IBook<?> book) {

        if (order.getSymbol().equals(book.getSymbol())) {

            final IBookLevel level;
            switch (order.getSide()) {
                case BID:
                    level = book.getBidLevel(order.getPrice());
                    break;
                case ASK:
                    level = book.getAskLevel(order.getPrice());
                    break;
                default:
                    level = null;
                    break;
            }

            return null != level && (order.getOrderQty() - order.getFilledQty() <= level.getQty());
        } else {

            return true;
        }
    }

    public static PullRule fromJSON(final JSONObject object) throws JSONException {

        final String orderSymbol = object.has("orderSymbol") ? object.getString("orderSymbol") : object.getString("symbol");
        final OrderSelectionPriceRangeSelection orderSelection =
                OrderSelectionPriceRangeSelection.fromJSON(object.getJSONObject("orderSelection"));

        final String mdSymbol = object.has("mdSymbol") ? object.getString("mdSymbol") : object.getString("symbol");
        final MktConditionQtyAtPriceCondition mdCondition = MktConditionQtyAtPriceCondition.fromJSON(object.getJSONObject("mktCondition"));

        return new PullRule(object.getLong("ruleID"), orderSymbol, orderSelection, mdSymbol, mdCondition);
    }

    public static long nextID() {
        final long baseID = System.currentTimeMillis() * 1000000;
        return baseID + SUB_ID.incrementAndGet();
    }
}
