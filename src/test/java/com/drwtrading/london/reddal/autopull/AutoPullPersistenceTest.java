package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.*;

public class AutoPullPersistenceTest {

    @Test
    public void test_works() throws IOException {

        Path tempFile = Files.createTempFile("pull", ".json");
        PullRule pullRule = new PullRule(
                PullRule.nextID(),
                "SYMBOL1",
                new OrderSelection.PriceRangeSelection("SYMBOL1", BookSide.BID, -999, 999),
                new MktCondition.QtyAtPriceCondition("SYMBOL1", BookSide.BID, 10069, MktCondition.Condition.GT, 50)
        );

        {
            AutoPullPersistence persistence = new AutoPullPersistence(tempFile);
            persistence.updateRule(pullRule);
        }

        {
            AutoPullPersistence persistence = new AutoPullPersistence(tempFile);
            Map<Long, PullRule> pullRules = persistence.getPullRules();
            assertEquals(pullRule, pullRules.get(pullRule.ruleID));

            persistence.deleteRule(pullRule);
            assertEquals(null, persistence.getPullRules().get(pullRule.ruleID));
        }

        {
            AutoPullPersistence persistence = new AutoPullPersistence(tempFile);
            assertEquals(null, persistence.getPullRules().get(pullRule.ruleID));
        }

    }

}