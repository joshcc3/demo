package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.reddal.data.ibook.IBookSubscriber;
import com.drwtrading.london.reddal.util.PriceOperations;
import com.drwtrading.london.reddal.util.PriceUtils;

import java.text.DecimalFormat;

public class SelectIOMDForSymbol implements IMarketData {

    private final IBookSubscriber bookHandler;
    private final String symbol;

    private final boolean isPriceInverted;
    public final TradeTracker tradeTracker;

    private IBook<?> book;
    private PriceOperations priceOperations;
    private DecimalFormat df;

    public SelectIOMDForSymbol(final IBookSubscriber bookHandler, final String symbol) {

        this.bookHandler = bookHandler;
        this.symbol = symbol;

        this.isPriceInverted = symbol.startsWith("6R");
        this.tradeTracker = new TradeTracker();
    }

    @Override
    public void subscribeForMD() {
        bookHandler.subscribeForMD(symbol, this);
    }

    public void setBook(final IBook<?> book) {

        this.book = book;
        this.priceOperations = new PriceUtils(book.getTickTable());

        final long smallestTick = book.getTickTable().getRawTickLevels().firstEntry().getValue();
        final String tickSize = Long.toString(smallestTick);

        int leastSigDigit = 0;
        for (int i = 0; i < tickSize.length(); ++i) {

            if ('0' != tickSize.charAt(i)) {
                leastSigDigit = tickSize.length() - i;
            }
        }
        final int decimalPlaces = Math.max(0, 10 - leastSigDigit);
        this.df = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, decimalPlaces);
    }

    @Override
    public void unsubscribeForMD() {
        bookHandler.unsubscribeForMD(symbol);
        tradeTracker.clear();
    }

    public void trade(final long price, final long qty) {
        tradeTracker.addTrade(price, qty);
    }

    @Override
    public PriceOperations getPriceOperations() {
        return priceOperations;
    }

    @Override
    public boolean isPriceInverted() {
        return isPriceInverted;
    }

    @Override
    public IBook<?> getBook() {
        return book;
    }

    @Override
    public TradeTracker getTradeTracker() {
        return tradeTracker;
    }

    @Override
    public String formatPrice(final long price) {
        return df.format(price / (double) Constants.NORMALISING_FACTOR);
    }
}
