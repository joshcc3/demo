package com.drwtrading.london.reddal.symbols;

import java.util.Collection;

public interface IndexUIView {

    void display(final Collection<SearchResult> results, final boolean tooMany);
}
