package com.drwtrading.london.reddal.util;

import com.drwtrading.london.reddal.ladders.PricingMode;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EnumSwitcherTest {

    @Test
    public void worksTest() {
        final EnumSwitcher<PricingMode> switcher = new EnumSwitcher<>(PricingMode.class, PricingMode.values());
        Assert.assertEquals(switcher.get(), PricingMode.BPS, "Pricing mode.");
        Assert.assertEquals(switcher.next(), PricingMode.EFP, "Pricing mode.");
        Assert.assertEquals(switcher.next(), PricingMode.RAW, "Pricing mode.");
        Assert.assertEquals(switcher.next(), PricingMode.BPS, "Pricing mode.");
    }

    @Test
    public void skipsSomeTest() {
        final EnumSwitcher<PricingMode> switcher = new EnumSwitcher<>(PricingMode.class, PricingMode.BPS, PricingMode.RAW);
        Assert.assertEquals(switcher.get(), PricingMode.BPS, "Pricing mode.");
        Assert.assertEquals(switcher.next(), PricingMode.RAW, "Pricing mode.");
        Assert.assertEquals(switcher.next(), PricingMode.BPS, "Pricing mode.");
        Assert.assertEquals(switcher.get(), PricingMode.BPS, "Pricing mode.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void refusesEmptyTest() {
        new EnumSwitcher<>(PricingMode.class);
    }
}