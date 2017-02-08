package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
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

    public PullRule(long ruleID, String symbol, OrderSelection.PriceRangeSelection orderSelection, MktCondition.QtyAtPriceCondition mktCondition) {
        this.ruleID = ruleID;
        this.symbol = symbol;
        this.orderSelection = orderSelection;
        this.mktCondition = mktCondition;
    }

    public List<Main.RemoteOrderCommandToServer> ordersToPull(String username, WorkingOrdersForSymbol workingOrdersForSymbol, IBook<?> book) {
        if (mktCondition.conditionMet(workingOrdersForSymbol, book)) {
            return workingOrdersForSymbol.ordersByKey.values().stream()
                    .filter(orderSelection::selectionMet)
                    .map(o -> o.buildCancelCommand(username))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static PullRule fromJSON(JSONObject object) throws JSONException {
        return new PullRule(
                object.getLong("ruleID"),
                object.getString("symbol"),
                OrderSelection.PriceRangeSelection.fromJSON(object.getJSONObject("orderSelection")),
                MktCondition.QtyAtPriceCondition.fromJSON(object.getJSONObject("mktCondition"))
        );
    }

    private static final AtomicLong subID = new AtomicLong(0);

    public static long nextID() {
        long baseID = System.currentTimeMillis() * 1000000;
        return baseID + subID.incrementAndGet();
    }

}