package com.drwtrading.london.reddal.util;

import com.drwtrading.london.reddal.ladders.PricingMode;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EnumSwitcherTest {

    @Test
    public void test_works() {
        final EnumSwitcher<PricingMode> switcher = new EnumSwitcher<>(PricingMode.class, PricingMode.values());
        assertEquals(PricingMode.BPS, switcher.get());
        assertEquals(PricingMode.EFP, switcher.next());
        assertEquals(PricingMode.RAW, switcher.next());
        assertEquals(PricingMode.BPS, switcher.next());
    }

    @Test
    public void test_skips_some() {
        final EnumSwitcher<PricingMode> switcher = new EnumSwitcher<>(PricingMode.class, PricingMode.BPS, PricingMode.RAW);
        assertEquals(PricingMode.BPS, switcher.get());
        assertEquals(PricingMode.RAW, switcher.next());
        assertEquals(PricingMode.BPS, switcher.next());
        assertEquals(PricingMode.BPS, switcher.get());
    }

    @Test
    public void test_refuses_empty() {
        try {
            final EnumSwitcher<PricingMode> switcher = new EnumSwitcher<>(PricingMode.class);
        } catch (final IllegalArgumentException ignored) {
            return;
        }
        assertFalse("Should have thrown an exception", true);
    }

}