package com.drwtrading.london.reddal.opxl;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class ISINsGoingEx {

    public final ImmutableSet<String> isins;

    ISINsGoingEx(final Set<String> isins) {
        this.isins = ImmutableSet.copyOf(isins);
    }
}
