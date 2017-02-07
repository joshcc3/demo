package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MktConditionTest {

    @Test
    public void test_json() throws IOException, JSONException {
        MktCondition.QtyAtPriceCondition before = new MktCondition.QtyAtPriceCondition("SYM", BookSide.ASK, 10000, MktCondition.Condition.GT, 6969);

        StringBuilder builder = new StringBuilder();
        before.toJson(builder);

        MktCondition after = MktCondition.fromJSON(new JSONObject(builder.toString()));
        assertEquals(before, after);


        StringBuffer builder2 = new StringBuffer();
        after.toJson(builder2);

        assertEquals(builder.toString(), builder2.toString());
    }

}