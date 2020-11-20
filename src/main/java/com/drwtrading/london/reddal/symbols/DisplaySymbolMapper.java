package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.staticData.ExpiryMonthCodes;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DisplaySymbolMapper {

    private final Publisher<DisplaySymbol> displaySymbolPublisher;

    private final Map<String, HashSet<String>> mdSymbolsByIsin;
    private final Map<String, String> bbgByIsin;
    private final Set<DisplaySymbol> displaySymbols;

    private final Map<String, String> ultimateParents;

    private final Map<String, String> ultimateParentBBG;

    public DisplaySymbolMapper(final Publisher<DisplaySymbol> displaySymbolPublisher) {

        this.displaySymbolPublisher = displaySymbolPublisher;

        this.mdSymbolsByIsin = new HashMap<>();
        this.bbgByIsin = new HashMap<>();
        this.displaySymbols = new HashSet<>();

        this.ultimateParents = new HashMap<>();
        this.ultimateParentBBG = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        final String symbol = searchResult.symbol;

        switch (searchResult.instType) {
            case EQUITY:
            case DR:
            case ETF: {
                final Set<String> symbols = MapUtils.getMappedSet(mdSymbolsByIsin, searchResult.instID.isin);
                symbols.add(symbol);
                makeDisplaySymbol(searchResult.instID.isin);
                break;
            }
            case FUTURE: {

                final String market = symbol.substring(0, symbol.length() - 2);
                final ExpiryMonthCodes expiryMonth = ExpiryMonthCodes.getCode(symbol.charAt(symbol.length() - 2));

                final Calendar cal = DateTimeUtil.getCalendar();

                final int currentYear = cal.get(Calendar.YEAR);
                final int digitYear = currentYear % 10;
                final int contractYear = Integer.parseInt(symbol.substring(symbol.length() - 1));
                final int yearsAhead;
                if (contractYear == digitYear) {
                    final int currentMonth = cal.get(Calendar.MONTH);
                    if (expiryMonth.calMonth < currentMonth) {
                        yearsAhead = 10;
                    } else {
                        yearsAhead = 0;
                    }
                } else if (contractYear < digitYear) {
                    yearsAhead = 10 + contractYear - digitYear;
                } else {
                    yearsAhead = contractYear - digitYear;
                }

                cal.set(Calendar.YEAR, currentYear + yearsAhead);
                cal.set(Calendar.MONTH, expiryMonth.calMonth);
                cal.set(Calendar.DAY_OF_MONTH, 1);

                final SimpleDateFormat sdf = DateTimeUtil.getDateFormatter("MMM yy");
                final String expiry = sdf.format(cal.getTimeInMillis());

                final DisplaySymbol displaySymbol = new DisplaySymbol(symbol, market + ' ' + expiry);
                publishIfNew(displaySymbol);
            }
        }
    }

    public void setInstDef(final InstrumentDef instDef) {

        if (instDef.isPrimary) {
            bbgByIsin.put(instDef.instID.isin, instDef.bbgCode);

            for (final Map.Entry<String, String> ultimateParent : ultimateParents.entrySet()) {

                if (ultimateParent.getValue().equals(instDef.instID.isin)) {
                    ultimateParentBBG.put(ultimateParent.getKey(), instDef.bbgCode);
                }
            }

            if (!ultimateParents.containsKey(instDef.instID.isin)) {
                ultimateParentBBG.put(instDef.instID.isin, instDef.bbgCode);
            }
            makeDisplaySymbol(instDef.instID.isin);
        }
    }

    public void setUltimateParent(final UltimateParentMapping ultimateParent) {

        ultimateParents.put(ultimateParent.childISIN, ultimateParent.parentID.isin);

        final String parentBBGCode = bbgByIsin.get(ultimateParent.parentID.isin);
        if (null != parentBBGCode) {
            ultimateParentBBG.put(ultimateParent.childISIN, parentBBGCode);
            makeDisplaySymbol(ultimateParent.childISIN);
        }
    }

    private void makeDisplaySymbol(final String isin) {

        final Set<String> symbols = mdSymbolsByIsin.get(isin);

        if (null != symbols) {
            final String ultimateBBG = ultimateParentBBG.get(isin);

            for (final String symbol : mdSymbolsByIsin.get(isin)) {

                final String display;
                if (null == ultimateBBG || ultimateBBG.contains(symbol)) {
                    display = symbol;
                } else {
                    display = ultimateBBG + " (" + symbol + ')';
                }

                final DisplaySymbol displaySymbol = new DisplaySymbol(symbol, display);
                publishIfNew(displaySymbol);
            }
        }
    }

    private void publishIfNew(final DisplaySymbol displaySymbol) {

        if (displaySymbols.add(displaySymbol)) {
            displaySymbolPublisher.publish(displaySymbol);
        }
    }
}
