package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class MktConditionTest {

    @Test
    public void jsonTest() throws IOException, JSONException {

        final MktCondition.QtyAtPriceCondition before =
                new MktCondition.QtyAtPriceCondition("SYM", BookSide.ASK, 10000, MktCondition.Condition.GT, 6969);

        final StringBuilder builder = new StringBuilder();
        before.toJson(builder);

        final MktCondition after = MktCondition.fromJSON(new JSONObject(builder.toString()));
        Assert.assertEquals(before, after, "Rule equality.");

        final StringBuffer builder2 = new StringBuffer();
        after.toJson(builder2);

        Assert.assertEquals(builder.toString(), builder2.toString(), "Rule text equality.");
    }

}