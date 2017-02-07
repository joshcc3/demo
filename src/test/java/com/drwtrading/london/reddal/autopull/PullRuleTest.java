package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PullRuleTest {

    @Test
    public void test_json() throws IOException, JSONException {

        PullRule pullRule = new PullRule(
                PullRule.nextID(),
                "SYMBOL1",
                new OrderSelection.PriceRangeSelection("SYMBOL1", BookSide.BID, -999, 999),
                new MktCondition.QtyAtPriceCondition("SYMBOL1", BookSide.BID, 10069, MktCondition.Condition.GT, 50)
        );

        StringBuilder builder = new StringBuilder();
        pullRule.toJson(builder);

        PullRule after = PullRule.fromJSON(new JSONObject(builder.toString()));
        assertEquals(pullRule, after);
    }

}