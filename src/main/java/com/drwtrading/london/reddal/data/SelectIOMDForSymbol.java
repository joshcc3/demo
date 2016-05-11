package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.md.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.reddal.util.PriceOperations;
import com.drwtrading.london.reddal.util.PriceUtils;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class SelectIOMDForSymbol implements IMarketData {

    private static final Pattern NUMBER_FORMAT_REPLACEMENT = Pattern.compile("[0-9]");

    private final MDTransportClient mdTransportClient;

    private final boolean isPriceInverted;
    public final TradeTracker tradeTracker;
    private final Runnable fastFlush;

    private IBook<?> book;
    private PriceOperations priceOperations;
    private DecimalFormat df;

    private boolean updatesWanted;

    public SelectIOMDForSymbol(final MDTransportClient mdTransportClient, final String symbol, final Runnable fastFlush) {

        this.mdTransportClient = mdTransportClient;

        this.isPriceInverted = symbol.startsWith("6R");
        this.tradeTracker = new TradeTracker();
        this.fastFlush = fastFlush;
    }

    public void setBook(final IBook<?> book) {

        this.book = book;
        this.priceOperations = new PriceUtils(book.getTickTable());

        final long smallestTick = book.getTickTable().getRawTickLevels().firstEntry().getValue();
        final String numberFormat = NUMBER_FORMAT_REPLACEMENT.matcher(Long.toString(smallestTick)).replaceAll("0");
        this.df = NumberFormatUtil.getDF(numberFormat);

        if (updatesWanted) {
            mdTransportClient.subscribeToInst(book.getLocalID());
        }
    }

    @Override
    public void subscribeForMD() {

        updatesWanted = true;
        if (null != book) {
            mdTransportClient.subscribeToInst(book.getLocalID());
        }
    }

    @Override
    public void unsubscribeForMD() {

        updatesWanted = false;
        if (null != book) {
            mdTransportClient.unsubscribeToInst(book.getLocalID());
        }
    }

    public void bookUpdated() {
        fastFlush.run();
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
