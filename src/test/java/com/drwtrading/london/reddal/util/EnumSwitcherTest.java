package com.drwtrading.london.reddal.util;

import com.drwtrading.london.reddal.LadderView;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EnumSwitcherTest {

    @Test
    public void test_works() {
        EnumSwitcher<LadderView.PricingMode> switcher = new EnumSwitcher<>(LadderView.PricingMode.class, EnumSet.allOf(LadderView.PricingMode.class));
        assertEquals(LadderView.PricingMode.BPS, switcher.get());
        assertEquals(LadderView.PricingMode.EFP, switcher.next());
        assertEquals(LadderView.PricingMode.RAW, switcher.next());
        assertEquals(LadderView.PricingMode.BPS, switcher.next());
    }

    @Test
    public void test_skips_some() {
        EnumSwitcher<LadderView.PricingMode> switcher = new EnumSwitcher<>(LadderView.PricingMode.class, EnumSet.of(LadderView.PricingMode.BPS, LadderView.PricingMode.RAW));
        assertEquals(LadderView.PricingMode.BPS, switcher.get());
        assertEquals(LadderView.PricingMode.RAW, switcher.next());
        assertEquals(LadderView.PricingMode.BPS, switcher.next());
        assertEquals(LadderView.PricingMode.BPS, switcher.get());
    }

    @Test
    public void test_refuses_empty() {
        try {
            EnumSwitcher<LadderView.PricingMode> switcher = new EnumSwitcher<>(LadderView.PricingMode.class, EnumSet.noneOf(LadderView.PricingMode.class));
        } catch (IllegalArgumentException e) {
            return;
        }
        assertFalse("Should have thrown an exception", true);
    }

}