package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class PullRuleTest {

    @Test
    public void jsonTest() throws IOException, JSONException {

        final PullRule pullRule =
                new PullRule(PullRule.nextID(), "SYMBOL1", new OrderSelection.PriceRangeSelection("SYMBOL1", BookSide.BID, -999, 999),
                        new MktCondition.QtyAtPriceCondition("SYMBOL1", BookSide.BID, 10069, MktCondition.Condition.GT, 50));

        final StringBuilder builder = new StringBuilder();
        pullRule.toJson(builder);

        final PullRule after = PullRule.fromJSON(new JSONObject(builder.toString()));
        Assert.assertEquals(pullRule, after, "Pull rule equality.");
    }
}