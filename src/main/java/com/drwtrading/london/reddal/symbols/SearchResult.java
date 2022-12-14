package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.util.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

public class SearchResult extends Struct {

    private static final Pattern FX_SPLIT = Pattern.compile("/", Pattern.LITERAL);

    public final String symbol;
    public final InstrumentID instID;
    public final InstType instType;
    public final String description;
    public final MDSource mdSource;
    public final long expiry;
    public final ITickTable tickTable;
    public final Collection<String> keywords;
    public final int decimalPlaces;

    public SearchResult(final String symbol, final InstrumentID instID, final InstType instType, final String description,
            final MDSource mdSource, final Collection<String> keywords, final long expiry, final ITickTable tickTable) {

        this.symbol = symbol;
        this.instID = instID;
        this.instType = instType;
        this.description = description;
        this.mdSource = mdSource;
        this.keywords = keywords;
        this.expiry = expiry;
        this.tickTable = tickTable;

        final long smallestTick = tickTable.getRawTickLevels().firstEntry().getValue();
        this.decimalPlaces = Math.max(0, 10 - Long.toString(smallestTick).length());
    }

    public SearchResult(final IBook<?> book) {

        // TODO: Description of contractAfterNext to contain buy/sell info

        final String isinCcyMic = book.getInstID().toString();

        this.symbol = book.getSymbol();
        this.instID = book.getInstID();
        this.instType = book.getInstType();
        this.description = isinCcyMic + ' ' + book.getMIC().exchange;
        this.mdSource = book.getSourceExch();

        this.expiry = book.getExpiryMilliSinceUTC();

        this.tickTable = book.getTickTable();

        this.keywords = new ArrayList<>();
        keywords.add(symbol);
        keywords.add(isinCcyMic);
        keywords.add(book.getMIC().exchange.name());

        if (InstType.FX == book.getInstType()) {
            final String fxSymbol = FX_SPLIT.matcher(symbol.split(" ")[0]).replaceAll("");
            keywords.add(fxSymbol);
        }

        final long smallestTick = tickTable.getRawTickLevels().firstEntry().getValue();
        this.decimalPlaces = Math.max(0, 10 - Long.toString(smallestTick).length());
    }
}
