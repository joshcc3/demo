package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.util.Struct;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PullRule extends Struct {

    public final long ruleID;
    public final String symbol;
    public final OrderSelection.PriceRangeSelection orderSelection;
    public final MktCondition.QtyAtPriceCondition mktCondition;

    PullRule(final long ruleID, final String symbol, final OrderSelection.PriceRangeSelection orderSelection,
            final MktCondition.QtyAtPriceCondition mktCondition) {
        this.ruleID = ruleID;
        this.symbol = symbol;
        this.orderSelection = orderSelection;
        this.mktCondition = mktCondition;
    }

    public List<RemoteOrderCommandToServer> ordersToPull(final String username, final WorkingOrdersForSymbol workingOrdersForSymbol,
            final IBook<?> book) {
        if (mktCondition.conditionMet(workingOrdersForSymbol, book)) {
            return workingOrdersForSymbol.ordersByKey.values().stream().filter(
                    workingOrderUpdateFromServer -> orderIsInMarketData(workingOrderUpdateFromServer, book)).filter(
                    orderSelection::selectionMet).map(o -> o.buildAutoCancel(username)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private boolean orderIsInMarketData(final WorkingOrderUpdateFromServer order, final IBook<?> book) {

        if (!order.workingOrderUpdate.getSymbol().equals(book.getSymbol())) {
            return false;
        }

        if (!book.isValid() || book.getStatus() != BookMarketState.CONTINUOUS) {
            return false;
        }

        if (book.getBestBid() == null && book.getBestAsk() == null) {
            return false;
        }

        final IBookLevel level;
        switch (order.workingOrderUpdate.getSide()) {
            case BID:
                level = book.getBidLevel(order.workingOrderUpdate.getPrice());
                break;
            case OFFER:
                level = book.getAskLevel(order.workingOrderUpdate.getPrice());
                break;
            default:
                level = null;
                break;
        }

        return null != level && order.workingOrderUpdate.getTotalQuantity() - order.workingOrderUpdate.getFilledQuantity() < level.getQty();
    }

    public static PullRule fromJSON(final JSONObject object) throws JSONException {
        return new PullRule(object.getLong("ruleID"), object.getString("symbol"),
                OrderSelection.PriceRangeSelection.fromJSON(object.getJSONObject("orderSelection")),
                MktCondition.QtyAtPriceCondition.fromJSON(object.getJSONObject("mktCondition")));
    }

    private static final AtomicLong subID = new AtomicLong(0);

    public static long nextID() {
        final long baseID = System.currentTimeMillis() * 1000000;
        return baseID + subID.incrementAndGet();
    }
}
