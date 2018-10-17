package com.drwtrading.london.reddal.autopull.rules;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.util.Struct;
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
    public final String symbol;
    public final OrderSelectionPriceRangeSelection orderSelection;
    public final MktConditionQtyAtPriceCondition mktCondition;

    public PullRule(final long ruleID, final String symbol, final OrderSelectionPriceRangeSelection orderSelection,
            final MktConditionQtyAtPriceCondition mktCondition) {

        this.ruleID = ruleID;
        this.symbol = symbol;
        this.orderSelection = orderSelection;
        this.mktCondition = mktCondition;
    }

    public List<RemoteOrderCommandToServer> getPullCmds(final String username, final Set<SourcedWorkingOrder> workingOrders,
            final IBook<?> book) {

        if (mktCondition.conditionMet(book) && !workingOrders.isEmpty()) {

            final List<RemoteOrderCommandToServer> result = new ArrayList<>();

            for (final SourcedWorkingOrder sourcedOrder : workingOrders) {

                if (isOrderInMarketData(sourcedOrder.order, book) && orderSelection.isSelectionMet(sourcedOrder.order)) {

                    final RemoteOrderCommandToServer cmd = sourcedOrder.buildCancel(username, true);
                    result.add(cmd);
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private static boolean isOrderInMarketData(final WorkingOrder order, final IBook<?> book) {

        if (!order.getSymbol().equals(book.getSymbol()) || !book.isValid() || book.getStatus() != BookMarketState.CONTINUOUS ||
                book.getBestBid() == null && book.getBestAsk() == null) {
            return false;
        } else {

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

            return null != level && order.getOrderQty() - order.getFilledQty() < level.getQty();
        }
    }

    public static PullRule fromJSON(final JSONObject object) throws JSONException {
        return new PullRule(object.getLong("ruleID"), object.getString("symbol"),
                OrderSelectionPriceRangeSelection.fromJSON(object.getJSONObject("orderSelection")),
                MktConditionQtyAtPriceCondition.fromJSON(object.getJSONObject("mktCondition")));
    }

    public static long nextID() {
        final long baseID = System.currentTimeMillis() * 1000000;
        return baseID + SUB_ID.incrementAndGet();
    }
}
