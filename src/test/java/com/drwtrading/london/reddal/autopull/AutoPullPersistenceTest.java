package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AutoPullPersistenceTest {

    @Test
    public void worksTest() throws IOException {

        final Path tempFile = Files.createTempFile("pull", ".json");
        final PullRule pullRule =
                new PullRule(PullRule.nextID(), "SYMBOL1", new OrderSelectionPriceRangeSelection("SYMBOL1", BookSide.BID, -999, 999),
                        new MktConditionQtyAtPriceCondition("SYMBOL1", BookSide.BID, 10069, MktConditionConditional.GT, 50));

        final AutoPullPersistence persistenceOne = new AutoPullPersistence(tempFile);
        persistenceOne.updateRule(pullRule);

        final AutoPullPersistence persistenceTwo = new AutoPullPersistence(tempFile);
        final Map<Long, PullRule> pullRules = persistenceTwo.getPullRules();
        Assert.assertEquals(pullRule, pullRules.get(pullRule.ruleID), "Rule equality.");

        persistenceTwo.deleteRule(pullRule);
        Assert.assertNull(persistenceTwo.getPullRules().get(pullRule.ruleID), "Rule result.");

        final AutoPullPersistence persistenceThree = new AutoPullPersistence(tempFile);
        Assert.assertNull(persistenceThree.getPullRules().get(pullRule.ruleID), "Rule result.");
    }
}
