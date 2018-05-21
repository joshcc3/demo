package com.drwtrading.london.reddal.nibblers;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.csv.fileTables.FileTableRow;
import com.drwtrading.london.eeif.utils.csv.fileTables.FileTableWriter;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class NibblerMetaDataLogger implements INibblerTradingDataListener {

    private static final String LOG_FILE_PREFIX = "metaData-";

    private final IResourceMonitor<ReddalComponents> monitor;

    private final long millisAtMidnight;
    private final SimpleDateFormat timeFormat;

    private final DecimalFormat twoDF;
    private final DecimalFormat priceDF;

    private final FileTableWriter<NibblerMetaTables> fileTableWriter;

    private final FileTableRow<NibblerMetaTables, NibblerTheoValueColumns> theoRow;
    private final FileTableRow<NibblerMetaTables, NibblerSpreadnoughtTheoColumns> spreadnoughtRow;
    private final FileTableRow<NibblerMetaTables, NibblerLastTradeColumns> lastTradeRow;

    public NibblerMetaDataLogger(final IClock clock, final IResourceMonitor<ReddalComponents> monitor, final Path logDir,
            final String nibblerName) throws IOException {

        this.monitor = monitor;

        this.millisAtMidnight = clock.getMillisAtMidnightUTC();
        this.timeFormat = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);

        this.twoDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2);
        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2, 6);

        final Path logFile = logDir.resolve(LOG_FILE_PREFIX + nibblerName + ".csv");
        this.fileTableWriter = new FileTableWriter<>(logFile, NibblerMetaTables.class);

        this.theoRow = fileTableWriter.addTable(NibblerMetaTables.THEO_VALUE, NibblerTheoValueColumns.values());
        this.spreadnoughtRow = fileTableWriter.addTable(NibblerMetaTables.SPREADNOUGHT_THEO, NibblerSpreadnoughtTheoColumns.values());
        this.lastTradeRow = fileTableWriter.addTable(NibblerMetaTables.LAST_TRADE, NibblerLastTradeColumns.values());
    }

    @Override
    public boolean addTheoValue(final TheoValue theoValue) {
        writeTheoRow(theoValue);
        return true;

    }

    @Override
    public boolean updateTheoValue(final TheoValue theoValue) {
        writeTheoRow(theoValue);
        return true;
    }

    private void writeTheoRow(final TheoValue theoValue) {

        final long millis = millisAtMidnight + theoValue.getNanoSinceMidnightUTC() / DateTimeUtil.NANOS_IN_MILLIS;
        final String time = timeFormat.format(millis);

        theoRow.set(NibblerTheoValueColumns.SYMBOL, theoValue.getSymbol());
        theoRow.set(NibblerTheoValueColumns.TIME, time);

        theoRow.set(NibblerTheoValueColumns.IS_VALID, theoValue.isValid());
        theoRow.set(NibblerTheoValueColumns.THEO_TYPE, theoValue.getTheoType());

        theoRow.set(NibblerTheoValueColumns.ORIGINAL_VALUE,
                priceDF.format(theoValue.getOriginalValue() / (double) Constants.NORMALISING_FACTOR));
        theoRow.set(NibblerTheoValueColumns.THEO_VALUE,
                priceDF.format(theoValue.getTheoreticalValue() / (double) Constants.NORMALISING_FACTOR));

        theoRow.set(NibblerTheoValueColumns.AFTER_HOURS_PERCENT, twoDF.format(theoValue.getAfterHoursPct()));
        theoRow.set(NibblerTheoValueColumns.RAW_AFTER_HOURS_PERCENT, twoDF.format(theoValue.getAfterHoursPct()));
        theoRow.set(NibblerTheoValueColumns.MOMENTUM_COMPONENT, twoDF.format(theoValue.getMomentumComponent()));

        try {
            this.fileTableWriter.writeRow(theoRow, false);
        } catch (final IOException e) {
            monitor.logError(ReddalComponents.META_DATA_LOG, "Failed to write theo row.", e);
        }
    }

    @Override
    public boolean addSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        writeSpreadnoughtTheoRow(theo);
        return true;
    }

    @Override
    public boolean updateSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        writeSpreadnoughtTheoRow(theo);
        return true;
    }

    private void writeSpreadnoughtTheoRow(final SpreadnoughtTheo theo) {

        final long millis = millisAtMidnight + theo.getNanoSinceMidnightUTC() / DateTimeUtil.NANOS_IN_MILLIS;
        final String time = timeFormat.format(millis);

        spreadnoughtRow.set(NibblerSpreadnoughtTheoColumns.SYMBOL, theo.getSymbol());
        spreadnoughtRow.set(NibblerSpreadnoughtTheoColumns.TIME, time);

        spreadnoughtRow.set(NibblerSpreadnoughtTheoColumns.IS_BID_VALID, theo.isBidValid());
        spreadnoughtRow.set(NibblerSpreadnoughtTheoColumns.BID, priceDF.format(theo.getBidValue() / (double) Constants.NORMALISING_FACTOR));

        spreadnoughtRow.set(NibblerSpreadnoughtTheoColumns.IS_ASK_VALID, theo.isAskValid());
        spreadnoughtRow.set(NibblerSpreadnoughtTheoColumns.ASK, priceDF.format(theo.getAskValue() / (double) Constants.NORMALISING_FACTOR));

        try {
            this.fileTableWriter.writeRow(spreadnoughtRow, false);
        } catch (final IOException e) {
            monitor.logError(ReddalComponents.META_DATA_LOG, "Failed to write spreadnought theo row.", e);
        }
    }

    @Override
    public boolean addLastTrade(final LastTrade lastTrade) {
        writeLastTradeRow(lastTrade);
        return true;
    }

    @Override
    public boolean updateLastTrade(final LastTrade lastTrade) {
        writeLastTradeRow(lastTrade);
        return true;
    }

    private void writeLastTradeRow(final LastTrade lastTrade) {

        final long millis = lastTrade.getMilliSinceUTC();
        final String time = timeFormat.format(millis);

        lastTradeRow.set(NibblerLastTradeColumns.SYMBOL, lastTrade.getSymbol());
        lastTradeRow.set(NibblerLastTradeColumns.TIME, time);

        lastTradeRow.set(NibblerLastTradeColumns.SIDE, lastTrade.getSide());
        lastTradeRow.set(NibblerLastTradeColumns.PRICE, priceDF.format(lastTrade.getPrice() / (double) Constants.NORMALISING_FACTOR));
        lastTradeRow.set(NibblerLastTradeColumns.QTY, lastTrade.getQty());

        try {
            this.fileTableWriter.writeRow(lastTradeRow, false);
        } catch (final IOException e) {
            monitor.logError(ReddalComponents.META_DATA_LOG, "Failed to write laser line row.", e);
        }
    }

    @Override
    public boolean batchComplete() {

        try {
            this.fileTableWriter.flush();
            monitor.setOK(ReddalComponents.META_DATA_LOG);
        } catch (final IOException e) {
            monitor.logError(ReddalComponents.META_DATA_LOG, "Failed to flush writer.", e);
        }
        return true;
    }
}
