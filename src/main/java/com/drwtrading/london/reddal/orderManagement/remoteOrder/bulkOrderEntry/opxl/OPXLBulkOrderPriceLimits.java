package com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.opxl;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.BulkOrderEntryPresenter;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class OPXLBulkOrderPriceLimits extends AOpxlLoggingReader<OPXLComponents, Map<BookSide, Map<String, Long>>> {

    private static final String TOPIC_PREFIX = "eeif(best_bid_offer_spreads_";
    private static final String TOPIC_SUFFIX = ")";

    private static final String SYMBOL_COL = "Roll";
    private static final String BID_COL = "Bid";
    private static final String ASK_COL = "Ask";

    private final BulkOrderEntryPresenter presenter;

    public OPXLBulkOrderPriceLimits(final SelectIO selectIO, final IResourceMonitor<OPXLComponents> monitor, final Path logDir,
            final OpxlClient<OPXLComponents> opxlClient, final BulkOrderEntryPresenter presenter) {

        super(opxlClient, selectIO, monitor, OPXLComponents.OPXL_BULK_ORDER_BETTERMENT_PRICE_LIMITS, getTopic(), logDir);

        this.presenter = presenter;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Map<BookSide, Map<String, Long>> parseTable(final Object[][] opxlTable) {

        if (0 < opxlTable.length) {

            final Object[] headerRow = opxlTable[0];

            final int symbolCol = findColumn(headerRow, SYMBOL_COL);
            final int bidCol = findColumn(headerRow, BID_COL);
            final int askCol = findColumn(headerRow, ASK_COL);

            final Map<String, Long> bidPriceLimits = new HashMap<>();
            final Map<String, Long> askPriceLimits = new HashMap<>();

            for (int i = 1; i < opxlTable.length; ++i) {

                final Object[] row = opxlTable[i];

                if (testColsPresent(row, symbolCol, bidCol, askCol)) {

                    final String symbol = row[symbolCol].toString();

                    try {

                        final double rawBid = Double.parseDouble(row[bidCol].toString());
                        final long bid = Math.round(rawBid * Constants.NORMALISING_FACTOR);
                        bidPriceLimits.put(symbol, bid);

                        final double rawAsk = Double.parseDouble(row[askCol].toString());
                        final long ask = Math.round(rawAsk * Constants.NORMALISING_FACTOR);
                        askPriceLimits.put(symbol, ask);
                    } catch (final Exception e) {
                        logErrorOnSelectIO("Could not parse Bulk Order limits for [" + symbol + "].", e);
                    }
                }
            }
            final Map<BookSide, Map<String, Long>> result = new EnumMap<>(BookSide.class);

            result.put(BookSide.BID, bidPriceLimits);
            result.put(BookSide.ASK, askPriceLimits);

            return result;

        } else {
            return null;
        }
    }

    @Override
    protected void handleUpdate(final Map<BookSide, Map<String, Long>> prevValue, final Map<BookSide, Map<String, Long>> values) {

        presenter.setPriceLimits(values);
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg) {
        monitor.logError(component, msg);
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg, final Throwable t) {
        monitor.logError(component, msg, t);
    }

    private static String getTopic() {

        final SimpleDateFormat sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT);
        final Calendar cal = DateTimeUtil.getCalendar();
        final String dateString = sdf.format(cal.getTime());
        return TOPIC_PREFIX + dateString + TOPIC_SUFFIX;
    }
}
