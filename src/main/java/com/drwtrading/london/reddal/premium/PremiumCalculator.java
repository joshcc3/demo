package com.drwtrading.london.reddal.premium;

import com.drwtrading.london.eeif.opxl.OpxlClientComponents;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;

import java.util.HashMap;
import java.util.Map;

public class PremiumCalculator implements IPremiumCalc {

    private static final long UPDATE_PERIOD_MILLIS = 5000L;

    private final IMDSubscriber bookSubscriber;
    private final PremiumOPXLWriter opxlWriter;

    private final Map<String, SpreadnoughtMidTheo> theos;
    private final Map<String, MDForSymbol> books;

    public PremiumCalculator(final SelectIO selectIO, final ConfigGroup config, final IResourceMonitor<OpxlClientComponents> monitor,
            final IMDSubscriber bookSubscriber) throws ConfigException {

        this.bookSubscriber = bookSubscriber;
        this.opxlWriter = new PremiumOPXLWriter(selectIO, config, monitor);

        this.theos = new HashMap<>();
        this.books = new HashMap<>();
    }

    @Override
    public void setTheoMid(final String symbol, final boolean isValid, final long mid) {

        final SpreadnoughtMidTheo theo = theos.get(symbol);
        if (null == theo) {

            final SpreadnoughtMidTheo newTheo = new SpreadnoughtMidTheo(symbol, isValid, mid);
            theos.put(symbol, newTheo);

            final MDForSymbol mdForSymbol = bookSubscriber.subscribeForMD(symbol, this);
            books.put(symbol, mdForSymbol);

        } else {
            theo.set(isValid, mid);
        }
    }

    public long recalcAll() {

        for (final SpreadnoughtMidTheo theo : theos.values()) {

            final MDForSymbol mdForSymbol = books.get(theo.symbol);
            recalcPremium(theo, mdForSymbol.getBook());
        }

        opxlWriter.flush();

        return UPDATE_PERIOD_MILLIS;
    }

    private void recalcPremium(final SpreadnoughtMidTheo theo, final IBook<?> book) {

        if (theo.isValid() && 0 != theo.getMid() && null != book && book.isValid()) {

            final IBookLevel bestBid = book.getBestBid();
            final IBookLevel bestAsk = book.getBestBid();

            if (null != bestBid && null != bestAsk) {

                final long midTheo = theo.getMid();
                final long midMarket = (bestBid.getPrice() + bestAsk.getPrice()) / 2;

                final double premium = (midMarket - midTheo) / (double) midTheo;

                opxlWriter.setPremium(theo.symbol, premium);

            } else {
                opxlWriter.setPremium(theo.symbol, Double.NaN);
            }
        } else {
            opxlWriter.setPremium(theo.symbol, Double.NaN);
        }
    }
}
