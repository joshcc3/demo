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

        final String orderSymbol = "SYMBOL1";
        final String mdSymbol = "SYMBOL2";

        final OrderSelectionPriceRangeSelection orderSelection =
                new OrderSelectionPriceRangeSelection(orderSymbol, BookSide.BID, -999, 999);

        final MktConditionQtyAtPriceCondition mdCondition =
                new MktConditionQtyAtPriceCondition(mdSymbol, BookSide.BID, 10069, MktConditionConditional.GT, 50);

        final PullRule pullRule = new PullRule(PullRule.nextID(), orderSymbol, orderSelection, mdSymbol, mdCondition);

        final StringBuilder builder = new StringBuilder();
        pullRule.toJson(builder);

        final PullRule after = PullRule.fromJSON(new JSONObject(builder.toString()));
        Assert.assertEquals(pullRule, after, "Pull rule equality.");
    }
}