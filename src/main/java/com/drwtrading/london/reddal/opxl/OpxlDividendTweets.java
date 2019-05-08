package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class OpxlDividendTweets extends AOpxlLoggingReader<OPXLComponents, Collection<StockAlert>> {

    private static final String TOPIC_PREFIX = "eeif(markit_dividend_updates_";
    private static final String TOPIC_SUFFIX = ")";

    private static final String TIME_COL = "Time";
    private static final String SYMBOL_COL = "Symbol";
    private static final String TYPE_COL = "Type";
    private static final String MSG_COL = "Msg";

    private static final String STOCK_ALERT_TYPE = "DIV";
    private static final long MAX_MILLIS_DELAYS = 60_000;

    private final IClock clock;
    private final Publisher<StockAlert> stockAlertPublisher;

    private final SimpleDateFormat timeDF;
    private final long timeOffset;

    private final Set<StockAlert> sentStockAlerts;

    public OpxlDividendTweets(final SelectIO selectIO, final IResourceMonitor<OPXLComponents> monitor, final Path logPath,
            final Publisher<StockAlert> stockAlertPublisher) {

        super(selectIO, selectIO, monitor, OPXLComponents.OPXL_DIVIDEND_TWEET_READER, getTopic(), logPath);

        this.clock = selectIO;
        this.stockAlertPublisher = stockAlertPublisher;

        this.timeDF = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.timeOffset = DateTimeUtil.LONDON_TIME_ZONE.getOffset(selectIO.nowMilliUTC());

        this.sentStockAlerts = new HashSet<>();
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Collection<StockAlert> parseTable(final Object[][] opxlTable) {

        if (0 < opxlTable.length) {

            final Object[] headerRow = opxlTable[0];

            final int timeCol = findColumn(headerRow, TIME_COL);
            final int symbolCol = findColumn(headerRow, SYMBOL_COL);
            final int typeCol = findColumn(headerRow, TYPE_COL);
            final int msgCol = findColumn(headerRow, MSG_COL);

            final Collection<StockAlert> recent = new LinkedList<>();

            for (int i = opxlTable.length - 1; 0 < i; --i) {

                final Object[] row = opxlTable[i];

                if (testColsPresent(row, timeCol, symbolCol, typeCol, msgCol)) {

                    final String time = row[timeCol].toString().substring(0, DateTimeUtil.TIME_FORMAT.length());

                    try {
                        final long milliSinceMidnight = clock.getMillisSinceMidnightUTC();
                        final long timestamp = timeDF.parse(time).getTime() - timeOffset;

                        if ((milliSinceMidnight - timestamp) < MAX_MILLIS_DELAYS) {

                            final String symbol = row[symbolCol].toString();
                            final String type = row[typeCol].toString();
                            final String msg = row[msgCol].toString();

                            final String alertMsg = type + ": " + msg;

                            final StockAlert alert = new StockAlert(milliSinceMidnight, time, STOCK_ALERT_TYPE, symbol, alertMsg);
                            recent.add(alert);
                        }

                    } catch (final Exception e) {
                        logErrorOnSelectIO("Could not parse ultimate parent line " + Arrays.toString(row) + " in ultimate parent file.", e);
                    }
                }
            }
            return recent;
        } else {
            return null;
        }
    }

    @Override
    protected void handleUpdate(final Collection<StockAlert> prevValue, final Collection<StockAlert> values) {

        for (final StockAlert stockAlert : values) {

            if (sentStockAlerts.add(stockAlert)) {
                stockAlertPublisher.publish(stockAlert);
            }
        }
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