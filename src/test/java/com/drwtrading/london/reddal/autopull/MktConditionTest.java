package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.autopull.rules.IMktCondition;
import com.drwtrading.london.reddal.autopull.rules.IOrderSelection;
import com.drwtrading.london.reddal.autopull.rules.MktConditionConditional;
import com.drwtrading.london.reddal.autopull.rules.MktConditionQtyAtPriceCondition;
import com.drwtrading.london.reddal.autopull.rules.OrderSelectionPriceRangeSelection;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class MktConditionTest {

    @Test
    public void mktConditionJSONTest() throws IOException, JSONException {

        final MktConditionQtyAtPriceCondition before =
                new MktConditionQtyAtPriceCondition("SYM", BookSide.ASK, 10000, MktConditionConditional.GT, 6969);

        final StringBuilder builder = new StringBuilder();
        before.toJson(builder);

        final IMktCondition after = IMktCondition.fromJSON(new JSONObject(builder.toString()));
        Assert.assertEquals(before, after, "Rule equality.");

        final StringBuffer builder2 = new StringBuffer();
        after.toJson(builder2);

        Assert.assertEquals(builder.toString(), builder2.toString(), "Rule text equality.");
    }

    @Test
    public void orderSelectionJSONTest() throws IOException, JSONException {

        final OrderSelectionPriceRangeSelection before = new OrderSelectionPriceRangeSelection("SYM", BookSide.BID, 69, 6969);

        final StringBuilder builder = new StringBuilder();
        before.toJson(builder);

        final IOrderSelection after = IOrderSelection.fromJSON(new JSONObject(builder.toString()));
        Assert.assertEquals(after, before, "Rule equality.");

        final StringBuffer builder2 = new StringBuffer();
        after.toJson(builder2);

        Assert.assertEquals(builder.toString(), builder2.toString(), "Rule text equality.");
    }
}