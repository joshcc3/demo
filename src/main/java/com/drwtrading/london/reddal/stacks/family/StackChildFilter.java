package com.drwtrading.london.reddal.stacks.family;

import java.util.HashSet;
import java.util.Set;

public class StackChildFilter {

    public final String groupName;
    public final String filterName;

    final Set<String> symbols;

    public StackChildFilter(final String groupName, final String filterName) {

        this.groupName = groupName;
        this.filterName = filterName;

        this.symbols = new HashSet<>();
    }

    public void addSymbol(final String symbol) {
        this.symbols.add(symbol);
    }

    public void removeSymbol(final String symbol) {
        this.symbols.remove(symbol);
    }
}
