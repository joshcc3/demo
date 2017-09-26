package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;

import java.text.DecimalFormat;

public class MDForSymbol {

    private final IMDSubscriber bookSubscriber;
    private final String symbol;

    private final boolean isPriceInverted;
    public final TradeTracker tradeTracker;

    private IBook<?> book;
    private IBook<IBookLevelWithOrders> level3Book;
    private DecimalFormat df;
    private DecimalFormat nonTrailingDF;

    public MDForSymbol(final IMDSubscriber bookSubscriber, final String symbol) {

        this.bookSubscriber = bookSubscriber;
        this.symbol = symbol;

        this.isPriceInverted = symbol.startsWith("6R");
        this.tradeTracker = new TradeTracker();
        bookSubscriber.subscribeForMD(symbol, this);
    }

    public void setL3Book(final IBook<IBookLevelWithOrders> book) {
        setBook(book);
        this.level3Book = book;
    }

    public void setBook(final IBook<?> book) {

        this.book = book;

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
        this.nonTrailingDF = NumberFormatUtil.getDF(NumberFormatUtil.ZERO_TO_FOUR_DP, 1, decimalPlaces);
    }

    public void trade(final long price, final long qty) {
        tradeTracker.addTrade(price, qty);
    }

    public void unsubscribeForMD() {
        bookSubscriber.unsubscribeForMD(symbol);
        tradeTracker.clear();
    }

    public boolean isPriceInverted() {
        return isPriceInverted;
    }

    public IBook<?> getBook() {
        return book;
    }

    public TradeTracker getTradeTracker() {
        return tradeTracker;
    }

    public String formatPrice(final long price) {
        return df.format(price / (double) Constants.NORMALISING_FACTOR);
    }

    public String formatPriceWithoutTrailingZeroes(final long price) {
        return nonTrailingDF.format(price / (double) Constants.NORMALISING_FACTOR);
    }

    public IBook<IBookLevelWithOrders> getLevel3Book() {
        return level3Book;
    }

    public void subscribeForMD() {
        bookSubscriber.subscribeForMD(symbol, this);
    }
}
