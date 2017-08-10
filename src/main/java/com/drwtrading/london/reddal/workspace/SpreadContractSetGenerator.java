package com.drwtrading.london.reddal.workspace;

import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.google.common.collect.ImmutableSet;
import org.jetlang.channels.Publisher;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpreadContractSetGenerator {

    private static final Set<String> EXCLUDED_MARKETS = ImmutableSet.of("FEXD");

    private final Publisher<SpreadContractSet> publisher;

    private final Set<String> futureSymbols;
    private final Map<String, String> frontLegToBackLeg;

    private final Map<String, String> stackLeanSymbols;
    private final Map<String, String> parentStackSymbols;

    private final Map<String, SpreadContractSet> existingSets;

    private final Calendar cal;
    private final FutureExpiryCalc expiryCalc;

    public SpreadContractSetGenerator(final Publisher<SpreadContractSet> publisher) {

        this.publisher = publisher;

        this.futureSymbols = new HashSet<>();
        this.frontLegToBackLeg = new HashMap<>();

        this.stackLeanSymbols = new HashMap<>();
        this.parentStackSymbols = new HashMap<>();

        this.existingSets = new HashMap<>();

        this.cal = DateTimeUtil.getCalendar();
        this.expiryCalc = new FutureExpiryCalc(0);
    }

    public void setStackRelationship(final String quoteSymbol, final String leanSymbol) {
        this.stackLeanSymbols.put(quoteSymbol, leanSymbol);
        publishContractSet(quoteSymbol);
    }

    public void setParentStack(final String quoteSymbol, final String parentStackSymbol) {
        if (!"orphanage".equals(parentStackSymbol)) {
            this.parentStackSymbols.put(quoteSymbol, parentStackSymbol + ";S");
            publishContractSet(quoteSymbol);
        }
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

                    final String prevBackLeg = frontLegToBackLeg.get(legs[0]);
                    if (isSpreadChosen(legs[0], legs[1], prevBackLeg)) {
                        this.frontLegToBackLeg.put(legs[0], legs[1]);
                        publishContractSet(legs[0]);
                    }
                }
                break;
            }
        }
    }

    private boolean isSpreadChosen(final String frontLeg, final String newBackLeg, final String prevBackLeg) {

        final FutureConstant future = FutureConstant.getFutureFromSymbol(frontLeg);

        if (null == future) {
            return false;
        } else {
            final String expectedFrontLeg = expiryCalc.getFutureCode(future);
            if (!expectedFrontLeg.equals(frontLeg)) {
                return false;
            } else if (null == prevBackLeg) {
                return true;
            } else {
                expiryCalc.setToRollDate(cal, newBackLeg);
                final long newLegExpiry = cal.getTimeInMillis();

                expiryCalc.setToRollDate(cal, prevBackLeg);
                final long prevLegExpiry = cal.getTimeInMillis();

                return newLegExpiry < prevLegExpiry;
            }
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

        if (isFuture && null != backLeg) {

            return getSpreadSet(true, symbol, backLeg, symbol + '-' + backLeg);
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
