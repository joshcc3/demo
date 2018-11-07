package com.drwtrading.london.reddal.autopull.autopuller.rules;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import drw.london.json.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

public interface IOrderSelection extends Jsonable {

    public boolean isSelectionMet(final WorkingOrder order);

    public static IOrderSelection fromJSON(final JSONObject object) throws JSONException {

        final String objType = object.getString("_type");
        switch (objType) {
            case "OrderSelectionPriceRangeSelection":
                return OrderSelectionPriceRangeSelection.fromJSON(object);
            case "OrderSelectionNone":
                return OrderSelectionNone.NONE;
            default:
                throw new IllegalArgumentException("Cannot parse " + object + " into OrderSelection");
        }
    }
}
