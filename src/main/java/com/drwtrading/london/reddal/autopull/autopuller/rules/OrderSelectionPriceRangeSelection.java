package com.drwtrading.london.reddal.autopull.autopuller.rules;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.util.Struct;
import org.json.JSONException;
import org.json.JSONObject;

public class OrderSelectionPriceRangeSelection extends Struct implements IOrderSelection {

    public final String symbol;
    public final BookSide side;
    public final long fromPrice;
    public final long toPrice;

    public OrderSelectionPriceRangeSelection(final String symbol, final BookSide side, final long fromPrice, final long toPrice) {

        this.symbol = symbol;
        this.side = side;
        this.fromPrice = fromPrice;
        this.toPrice = toPrice;
    }

    @Override
    public boolean isSelectionMet(final WorkingOrder order) {

        return order.getSymbol().equals(symbol) &&
                (BookSide.BID == order.getSide() && BookSide.BID == side || BookSide.ASK == order.getSide() && BookSide.ASK == side) &&
                fromPrice <= order.getPrice() && order.getPrice() <= toPrice;
    }

    public static OrderSelectionPriceRangeSelection fromJSON(final JSONObject object) throws JSONException {
        return new OrderSelectionPriceRangeSelection(object.getString("symbol"), BookSide.valueOf(object.getString("side")),
                object.getLong("fromPrice"), object.getLong("toPrice"));
    }
}
