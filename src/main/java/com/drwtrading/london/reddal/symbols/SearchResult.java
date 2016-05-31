package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.util.Struct;

import java.util.ArrayList;
import java.util.Collection;

public class SearchResult extends Struct {

    public final String symbol;
    public final InstrumentID instID;
    public final InstType instType;
    public final String description;
    public final MDSource mdSource;
    public final Collection<String> keywords;
    public final long expiry;
    public final String displaySymbol;

    public SearchResult(final String symbol, final InstrumentID instID, final InstType instType,
                        final String description, final MDSource mdSource, final Collection<String> keywords, final long expiry, String displaySymbol) {

        this.symbol = symbol;
        this.instID = instID;
        this.instType = instType;
        this.description = description;
        this.mdSource = mdSource;
        this.keywords = keywords;
        this.expiry = expiry;
        this.displaySymbol = displaySymbol;
    }

    public SearchResult(final IBook<?> book) {

        // TODO: Support Future expiry dates
        // TODO: Description of spread to contain buy/sell info

        final String isinCcyMic = book.getInstID().toString();

        this.symbol = book.getSymbol();
        this.instID = book.getInstID();
        this.instType = book.getInstType();
        this.description = isinCcyMic + ' ' + book.getMIC().exchange;
        this.mdSource = book.getSourceExch();
        this.displaySymbol = book.getSymbol();

        this.keywords = new ArrayList<>();
        keywords.add(symbol);
        keywords.add(isinCcyMic);
        keywords.add(book.getMIC().exchange.name());

        this.expiry = 0;
    }
}
