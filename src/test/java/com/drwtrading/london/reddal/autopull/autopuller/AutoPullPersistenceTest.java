package com.drwtrading.london.reddal.autopull.autopuller;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.autopull.autopuller.rules.MktConditionConditional;
import com.drwtrading.london.reddal.autopull.autopuller.rules.MktConditionQtyAtPriceCondition;
import com.drwtrading.london.reddal.autopull.autopuller.rules.OrderSelectionPriceRangeSelection;
import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;
import com.drwtrading.london.reddal.autopull.autopuller.ui.AutoPullPersistence;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AutoPullPersistenceTest {

    @Test
    public void worksTest() throws IOException {

        final String orderSymbol = "SYMBOL1";
        final String mdSymbol = "SYMBOL2";

        final Path tempFile = Files.createTempFile("pull", ".json");

        final OrderSelectionPriceRangeSelection orderSelection =
                new OrderSelectionPriceRangeSelection(orderSymbol, BookSide.BID, -999, 999);

        final MktConditionQtyAtPriceCondition mdCondition =
                new MktConditionQtyAtPriceCondition(mdSymbol, BookSide.BID, 10069, MktConditionConditional.GT, 50);

        final PullRule pullRule = new PullRule(PullRule.nextID(), orderSymbol, orderSelection, mdSymbol, mdCondition);

        final AutoPullPersistence persistenceOne = new AutoPullPersistence(tempFile);
        persistenceOne.updateRule(pullRule);

        final AutoPullPersistence persistenceTwo = new AutoPullPersistence(tempFile);
        final Map<Long, PullRule> pullRules = persistenceTwo.getPullRules();
        Assert.assertEquals(pullRule, pullRules.get(pullRule.ruleID), "Rule equality.");

        persistenceTwo.deleteRule(pullRule.ruleID);
        Assert.assertNull(persistenceTwo.getPullRules().get(pullRule.ruleID), "Rule result.");

        final AutoPullPersistence persistenceThree = new AutoPullPersistence(tempFile);
        Assert.assertNull(persistenceThree.getPullRules().get(pullRule.ruleID), "Rule result.");
    }
}
