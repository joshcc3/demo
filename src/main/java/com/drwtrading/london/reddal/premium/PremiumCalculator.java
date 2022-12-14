package com.drwtrading.london.reddal.premium;

import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class PremiumCalculator implements IPremiumCalc {

    private static final long UPDATE_PERIOD_MILLIS = 3000L;

    private final IMDSubscriber bookSubscriber;
    private final Publisher<Premium> premiumPublisher;

    private final Map<String, SpreadnoughtMidTheo> theos;
    private final Map<String, MDForSymbol> books;

    public PremiumCalculator(final IMDSubscriber bookSubscriber, final Publisher<Premium> premiumPublisher) {

        this.bookSubscriber = bookSubscriber;
        this.premiumPublisher = premiumPublisher;

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
            recalcPremium(theo, mdForSymbol.getBook(), mdForSymbol.getTradeTracker());
        }

        return UPDATE_PERIOD_MILLIS;
    }

    private void recalcPremium(final SpreadnoughtMidTheo theo, final IBook<?> book, final TradeTracker tradeTracker) {

        if (theo.isValid() && 0 != theo.getMid() && null != book && book.isValid()) {

            final IBookLevel bestBid = book.getBestBid();
            final IBookLevel bestAsk = book.getBestAsk();

            final long midTheo = theo.getMid();

            final double midMarketPremium;
            if (null != bestBid && null != bestAsk) {

                final long midMarket = (bestBid.getPrice() + bestAsk.getPrice()) / 2;
                midMarketPremium = (midMarket - midTheo) / (double) midTheo;
            } else {
                midMarketPremium = Double.NaN;
            }

            final double lastTradePremium;
            if (tradeTracker.hasTrade()) {
                lastTradePremium = (tradeTracker.getLastPrice() - midTheo) / (double) midTheo;
            } else {
                lastTradePremium = Double.NaN;
            }

            premiumPublisher.publish(new Premium(theo.symbol, midMarketPremium, lastTradePremium));
        } else {
            premiumPublisher.publish(new Premium(theo.symbol, Double.NaN, Double.NaN));
        }
    }
}
