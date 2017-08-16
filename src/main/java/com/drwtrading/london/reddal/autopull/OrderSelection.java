package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import eeif.execution.Side;
import eeif.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.util.Struct;
import drw.london.json.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

public interface OrderSelection extends Jsonable {

    None NONE = new None();

    boolean selectionMet(WorkingOrderUpdateFromServer order);

    class None extends Struct implements OrderSelection {
        @Override
        public boolean selectionMet(WorkingOrderUpdateFromServer order) {
            return false;
        }

    }

    class PriceRangeSelection extends Struct implements OrderSelection {
        public final String symbol;
        public final BookSide side;
        public final long fromPrice;
        public final long toPrice;

        PriceRangeSelection(String symbol, BookSide side, long fromPrice, long toPrice) {
            this.symbol = symbol;
            this.side = side;
            this.fromPrice = fromPrice;
            this.toPrice = toPrice;
        }

        @Override
        public boolean selectionMet(WorkingOrderUpdateFromServer order) {
            WorkingOrderUpdate update = order.workingOrderUpdate;
            return update.getSymbol().equals(symbol) &&
                    (update.getSide() == Side.BID && side == BookSide.BID ||
                            update.getSide() == Side.OFFER && side == BookSide.ASK)
                    && fromPrice <= order.workingOrderUpdate.getPrice()
                    && toPrice >= order.workingOrderUpdate.getPrice();
        }

        public static PriceRangeSelection fromJSON(JSONObject object) throws JSONException {
            return new PriceRangeSelection(
                    object.getString("symbol"),
                    BookSide.valueOf(object.getString("side")),
                    object.getLong("fromPrice"),
                    object.getLong("toPrice")
            );
        }
    }

    public static OrderSelection fromJSON(JSONObject object) throws JSONException {
        String objType = object.getString("_type");
        switch (objType) {

            case "PriceRangeSelection":
                return PriceRangeSelection.fromJSON(object);
            case "None":
                return NONE;
            default:
                throw new IllegalArgumentException("Cannot parse " + object.toString() + " into OrderSelection");
        }
    }
}
