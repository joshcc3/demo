package com.drwtrading.london.reddal.autopull.autopuller.rules;

import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import drw.london.json.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

public interface IMktCondition extends Jsonable {

    public boolean conditionMet(final IBook<?> book);

    public static IMktCondition fromJSON(final JSONObject object) throws JSONException {
        final String type = object.getString("_type");
        switch (type) {
            case "MktConditionQtyAtPriceCondition":
                return MktConditionQtyAtPriceCondition.fromJSON(object);
            default:
                throw new IllegalArgumentException("Could not parse [" + object + "] into MktCondition");
        }
    }

}
