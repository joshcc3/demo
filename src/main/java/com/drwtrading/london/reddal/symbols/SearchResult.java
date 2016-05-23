package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.util.Struct;

import java.util.ArrayList;
import java.util.Collection;

public class SearchResult extends Struct {

    public final String symbol;
    public final String link;
    public final String description;
    public final MDSource mdSource;
    public final Collection<String> keywords;
    public final long expiry;

    public SearchResult(final String symbol, final String link, final String description, final MDSource mdSource,
            final Collection<String> keywords, final long expiry) {

        this.symbol = symbol;
        this.link = link;
        this.description = description;
        this.mdSource = mdSource;
        this.keywords = keywords;
        this.expiry = expiry;
    }

    public SearchResult(final IBook<?> book) {

        // TODO: Support Future expiry dates
        // TODO: Description of spread to contain buy/sell info

        final String isinCcyMic = book.getInstID().toString();

        this.symbol = book.getSymbol();
        this.link = "/ladder#" + symbol;
        this.description = isinCcyMic + ' ' + book.getMIC().exchange;
        this.mdSource = book.getSourceExch();

        this.keywords = new ArrayList<>();
        keywords.add(symbol);
        keywords.add(isinCcyMic);
        keywords.add(book.getMIC().exchange.name());

        this.expiry = 0;
    }
}
