package com.drwtrading.london.reddal.autopull.autopuller.rules;

import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.util.Struct;
import org.json.JSONException;
import org.json.JSONObject;

public class MktConditionQtyAtPriceCondition extends Struct implements IMktCondition {

    public final String symbol;
    public final BookSide side;
    public final long price;
    public final MktConditionConditional qtyCondition;
    public final int qtyThreshold;

    public MktConditionQtyAtPriceCondition(final String symbol, final BookSide side, final long price,
            final MktConditionConditional qtyCondition, final int qtyThreshold) {

        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.qtyCondition = qtyCondition;
        this.qtyThreshold = qtyThreshold;
    }

    @Override
    public boolean conditionMet(final IBook<?> book) {

        if (book.getSymbol().equals(symbol) && book.getStatus() == BookMarketState.CONTINUOUS && book.isValid()) {

            final IBookLevel lvl;
            switch (side) {
                case BID:
                    lvl = book.getBidLevel(price);
                    break;
                case ASK:
                    lvl = book.getAskLevel(price);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown side [" + side + ']');
            }

            final long qty = null == lvl ? 0 : lvl.getQty();
            return qtyCondition.test((int) qty, qtyThreshold);
        } else {
            return false;
        }
    }

    public static MktConditionQtyAtPriceCondition fromJSON(final JSONObject object) throws JSONException {
        return new MktConditionQtyAtPriceCondition(object.getString("symbol"), BookSide.valueOf(object.getString("side")),
                object.getLong("price"), MktConditionConditional.valueOf(object.getString("qtyCondition")), object.getInt("qtyThreshold"));
    }
}
