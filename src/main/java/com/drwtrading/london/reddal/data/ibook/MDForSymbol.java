package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IInstrument;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.reddal.data.TradeTracker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MDForSymbol {

    public final String symbol;

    private final Set<Object> listeners;

    private final boolean isPriceInverted;
    private final TradeTracker tradeTracker;

    private final ArrayList<IMDCallback> bookUpdateCallbacks;

    private IBook<?> book;
    private MIC tradeMIC;
    private IBook<IBookLevelWithOrders> level3Book;
    private DecimalFormat df;
    private DecimalFormat nonTrailingDF;

    private boolean isReverseSpread;

    public MDForSymbol(final String symbol) {

        this.symbol = symbol;

        this.listeners = new HashSet<>();

        this.isPriceInverted = symbol.startsWith("6R");
        this.tradeTracker = new TradeTracker();

        this.bookUpdateCallbacks = new ArrayList<>();

        this.isReverseSpread = false;
    }

    public void setL3Book(final IBook<IBookLevelWithOrders> book) {
        setBook(book);
        this.level3Book = book;
    }

    public void setBook(final IBook<?> book) {

        this.book = book;
        if (InstType.FUTURE_SPREAD == book.getInstType()) {

            final String frontMonth = book.getSymbol().split("-")[0];
            final FutureConstant future = FutureConstant.getFutureFromSymbol(frontMonth);
            isReverseSpread = null != future && future.isReverseSpread;
        }

        this.tradeMIC = getTradeMIC(book);

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
        this.nonTrailingDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 1, decimalPlaces);
    }

    private static MIC getTradeMIC(final IInstrument book) {

        switch (book.getSourceExch()) {
            case BATS_EUROPE: {
                return MIC.BATE;
            }
            case BATS_US: {
                return MIC.BATS;
            }
            case CHIX: {
                return MIC.CHIX;
            }
            case RFQ: {
                return MIC.XOFF;
            }
            default: {
                return book.getMIC();
            }
        }
    }

    boolean addListener(final Object listener) {

        final boolean result = listeners.isEmpty();
        listeners.add(listener);
        return result;
    }

    boolean addMDCallback(final IMDCallback callback) {

        bookUpdateCallbacks.add(callback);
        return 1 == bookUpdateCallbacks.size();
    }

    boolean isListeningForUpdates() {
        return !bookUpdateCallbacks.isEmpty();
    }

    boolean removeListener(final Object listener) {

        listeners.remove(listener);
        return listeners.isEmpty() && bookUpdateCallbacks.isEmpty();
    }

    public boolean isPriceInverted() {
        return isPriceInverted;
    }

    public MIC getTradeMIC() {
        return tradeMIC;
    }

    public IBook<?> getBook() {
        return book;
    }

    public boolean isReverseSpread() {
        return isReverseSpread;
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

    public void trade(final long price, final long qty) {
        tradeTracker.addTrade(price, qty);
    }

    public void bookUpdated() {

        for (int i = 0; i < bookUpdateCallbacks.size(); ++i) {
            final IMDCallback callback = bookUpdateCallbacks.get(i);
            callback.bookUpdated(this);
        }
    }

    public void unsubscribed() {
        tradeTracker.clear();
    }
}
