package com.drwtrading.london.reddal.autopull.autopuller;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.autopull.autopuller.rules.MktConditionConditional;
import com.drwtrading.london.reddal.autopull.autopuller.rules.MktConditionQtyAtPriceCondition;
import com.drwtrading.london.reddal.autopull.autopuller.rules.OrderSelectionPriceRangeSelection;
import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class PullRuleTest {

    @Test
    public void jsonTest() throws IOException, JSONException {

        final PullRule pullRule =
                new PullRule(PullRule.nextID(), "SYMBOL1", new OrderSelectionPriceRangeSelection("SYMBOL1", BookSide.BID, -999, 999),
                        new MktConditionQtyAtPriceCondition("SYMBOL1", BookSide.BID, 10069, MktConditionConditional.GT, 50));

        final StringBuilder builder = new StringBuilder();
        pullRule.toJson(builder);

        final PullRule after = PullRule.fromJSON(new JSONObject(builder.toString()));
        Assert.assertEquals(pullRule, after, "Pull rule equality.");
    }
}