package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.util.Struct;
import drw.london.json.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.BiPredicate;

public interface MktCondition extends Jsonable {

    boolean conditionMet(WorkingOrdersForSymbol orders, IBook<?> book);

    class QtyAtPriceCondition extends Struct implements MktCondition {

        public final String symbol;
        public final BookSide side;
        public final long price;
        public final Condition qtyCondition;
        public final int qtyThreshold;

        QtyAtPriceCondition(final String symbol, final BookSide side, final long price, final Condition qtyCondition,
                final int qtyThreshold) {
            this.symbol = symbol;
            this.side = side;
            this.price = price;
            this.qtyCondition = qtyCondition;
            this.qtyThreshold = qtyThreshold;
        }

        @Override
        public boolean conditionMet(final WorkingOrdersForSymbol orders, final IBook<?> book) {
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
            }
            return false;
        }

        public static QtyAtPriceCondition fromJSON(final JSONObject object) throws JSONException {
            return new QtyAtPriceCondition(object.getString("symbol"), BookSide.valueOf(object.getString("side")), object.getLong("price"),
                    Condition.valueOf(object.getString("qtyCondition")), object.getInt("qtyThreshold"));
        }
    }

    enum Condition implements BiPredicate<Integer, Integer> {
        GT {
            @Override
            public boolean test(final Integer integer, final Integer integer2) {
                return integer > integer2;
            }
        },
        LT {
            @Override
            public boolean test(final Integer integer, final Integer integer2) {
                return integer < integer2;
            }
        };
    }

    public static MktCondition fromJSON(final JSONObject object) throws JSONException {
        final String type = object.getString("_type");
        switch (type) {
            case "QtyAtPriceCondition":
                return QtyAtPriceCondition.fromJSON(object);
            default:
                throw new IllegalArgumentException("Could not parse [" + object + "] into MktCondition");
        }
    }

}
