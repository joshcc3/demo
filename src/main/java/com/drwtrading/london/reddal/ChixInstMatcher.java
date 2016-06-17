package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Channel;

import java.util.HashMap;
import java.util.Map;

public class ChixInstMatcher {

    private final Channel<ChixSymbolPair> publisher;

    private final Map<InstrumentID, String> chixSymbols;
    private final Map<InstrumentID, String> primaryExchSymbols;

    public ChixInstMatcher(final Channel<ChixSymbolPair> publisher) {

        this.publisher = publisher;

        this.chixSymbols = new HashMap<>();
        this.primaryExchSymbols = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case ETF:
            case DR:
            case EQUITY: {
                addEquityDef(searchResult.symbol, searchResult.instID, searchResult.mdSource);
            }
        }
    }

    private void addEquityDef(final String symbol, final InstrumentID instID, final MDSource mdSource) {

        if (MDSource.CHIX == mdSource) {

            chixSymbols.put(instID, symbol);
            final String primarySymbol = primaryExchSymbols.get(instID);
            if (null != primarySymbol) {
                updateChixPair(primarySymbol, symbol);
            }
        } else {

            primaryExchSymbols.put(instID, symbol);
            final String chixSymbol = chixSymbols.get(instID);
            if (null != chixSymbol) {
                updateChixPair(symbol, chixSymbol);
            }
        }
    }

    private void updateChixPair(final String primarySymbol, final String chixSymbol) {

        final ChixSymbolPair chixSymbolPair = new ChixSymbolPair(primarySymbol, chixSymbol);
        publisher.publish(chixSymbolPair);
    }
}
