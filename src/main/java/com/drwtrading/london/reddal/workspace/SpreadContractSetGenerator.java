package com.drwtrading.london.reddal.workspace;

import com.drwtrading.london.eeif.stack.manager.relations.StackOrphanage;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.google.common.collect.ImmutableSet;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpreadContractSetGenerator {

    private static final Set<String> EXCLUDED_MARKETS = ImmutableSet.of("FEXD");

    private final Publisher<SpreadContractSet> publisher;
    private final Publisher<LeanDef> leanDefPublisher;

    private final Set<String> futureSymbols;
    private final Map<String, String> frontLegToBackLeg;
    private final Map<String, String> backLegToFrontLeg;

    private final Map<String, String> stackLeanSymbols;
    private final Map<String, String> parentStackSymbols;

    private final Map<String, SpreadContractSet> existingSets;

    private final FutureExpiryCalc expiryCalc;


    public SpreadContractSetGenerator(final Publisher<SpreadContractSet> publisher, final Publisher<LeanDef> leanDefPublisher) {
        this.publisher = publisher;
        this.leanDefPublisher = leanDefPublisher;

        this.futureSymbols = new HashSet<>();
        this.frontLegToBackLeg = new HashMap<>();
        this.backLegToFrontLeg = new HashMap<>();

        this.stackLeanSymbols = new HashMap<>();
        this.parentStackSymbols = new HashMap<>();

        this.existingSets = new HashMap<>();

        this.expiryCalc = new FutureExpiryCalc(0);
    }

    public void setStackRelationship(final String quoteSymbol, final String leanSymbol, InstrumentID leanInstID, InstType leanInstType) {
        this.stackLeanSymbols.put(quoteSymbol, leanSymbol);
        publishContractSet(quoteSymbol);
        leanDefPublisher.publish(new LeanDef(leanSymbol, leanInstID, leanInstType));
    }

    public void setParentStack(final String quoteSymbol, final String parentStackSymbol) {

        if (StackOrphanage.ORPHANAGE.equals(parentStackSymbol)) {
            this.parentStackSymbols.remove(quoteSymbol);
        } else {
            this.parentStackSymbols.put(quoteSymbol, parentStackSymbol + ";S");
        }
        publishContractSet(quoteSymbol);
    }

    public void setSearchResult(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case FUTURE: {

                final int contractLength = searchResult.symbol.length() - 2;
                final String contract = searchResult.symbol.substring(0, contractLength);

                if (!EXCLUDED_MARKETS.contains(contract)) {

                    this.futureSymbols.add(searchResult.symbol);
                    publishContractSet(searchResult.symbol);
                }
                break;
            }
            case FUTURE_SPREAD: {

                final String[] legs = searchResult.symbol.split("-");

                final int frontContractLength = legs[0].length() - 2;
                final String contract = legs[0].substring(0, frontContractLength);

                if (!EXCLUDED_MARKETS.contains(contract)) {

                    if (isSpreadChosen(legs[0], legs[1])) {
                        this.frontLegToBackLeg.put(legs[0], legs[1]);
                        this.backLegToFrontLeg.put(legs[1], legs[0]);
                        publishContractSet(legs[0]);
                        publishContractSet(legs[1]);
                    }
                }
                break;
            }
        }
    }

    private boolean isSpreadChosen(final String frontLeg, final String backLeg) {

        final FutureConstant future = FutureConstant.getFutureFromSymbol(frontLeg);

        if (null == future) {
            return false;
        } else {
            final String expectedFrontLeg = expiryCalc.getFutureCode(future);
            final String expectedBackLeg = expiryCalc.getFutureCode(future, 1);
            return expectedFrontLeg.equals(frontLeg) && expectedBackLeg.equals(backLeg);
        }
    }

    private void publishContractSet(final String symbol) {

        final SpreadContractSet spreadContractSet = getSpreadSet(symbol);
        if (null != spreadContractSet && !spreadContractSet.equals(existingSets.put(spreadContractSet.symbol, spreadContractSet))) {
            publisher.publish(spreadContractSet);
        }
    }

    private SpreadContractSet getSpreadSet(final String symbol) {

        final boolean isFuture = futureSymbols.contains(symbol);
        final String backLeg = frontLegToBackLeg.get(symbol);
        final String frontLeg = backLegToFrontLeg.get(symbol);

        if (isFuture) {

            if (null != backLeg) {
                return getSpreadSet(true, symbol, backLeg, symbol + '-' + backLeg);
            } else if (null != frontLeg) {
                return getSpreadSet(true, symbol, frontLeg + '-' + symbol, frontLeg);
            } else {
                return getSpreadSet(false, symbol, symbol, symbol);
            }

        } else {
            return getSpreadSet(false, symbol, symbol, symbol);
        }
    }

    private SpreadContractSet getSpreadSet(final boolean isFutureSpread, final String symbol, final String backMonthSymbol,
            final String spreadSymbol) {

        final String leanSymbol = stackLeanSymbols.get(symbol);
        final String parentStackSymbol = parentStackSymbols.get(symbol);

        if (!isFutureSpread && null == leanSymbol && null == parentStackSymbol) {
            return null;
        } else if (null == leanSymbol) {
            return new SpreadContractSet(symbol, backMonthSymbol, spreadSymbol, null, null, parentStackSymbol);
        } else {
            final String stackSymbol = symbol + ";S";
            return new SpreadContractSet(symbol, backMonthSymbol, spreadSymbol, leanSymbol, stackSymbol, parentStackSymbol);
        }
    }
}
