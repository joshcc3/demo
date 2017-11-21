package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.utils.csv.fileTables.FileTableRow;
import com.drwtrading.london.eeif.utils.csv.fileTables.FileTableWriter;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

public class NibblerTransportOrderEntry {

    private final IClock clock;
    private final IResourceMonitor<ReddalComponents> monitor;

    private final NibblerClientHandler nibblerClient;

    private final SimpleDateFormat sdf;
    private final FileTableWriter<NibblerRemoteTables> log;

    private final FileTableRow<NibblerRemoteTables, NibblerRemoteSubmitColumns> submitRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteModifyColumns> modifyRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteCancelColumns> cancelRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteStopAllStrategiesColumns> stopStrategiesRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteShutdownOMSColumns> shutdownRow;

    public NibblerTransportOrderEntry(final IClock clock, final IResourceMonitor<ReddalComponents> monitor,
            final NibblerClientHandler nibblerClient, final Path logDir) throws IOException {

        this.clock = clock;
        this.monitor = monitor;

        this.nibblerClient = nibblerClient;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);

        final Path logFile = logDir.resolve(nibblerClient.getRemoteUser() + ".csv");
        this.log = new FileTableWriter<>(logFile, NibblerRemoteTables.class);

        this.submitRow = log.addTable(NibblerRemoteTables.SUBMIT, NibblerRemoteSubmitColumns.values());
        this.modifyRow = log.addTable(NibblerRemoteTables.MODIFY, NibblerRemoteModifyColumns.values());
        this.cancelRow = log.addTable(NibblerRemoteTables.CANCEL, NibblerRemoteCancelColumns.values());
        this.stopStrategiesRow = log.addTable(NibblerRemoteTables.STOP_ALL_STRATEGIES, NibblerRemoteStopAllStrategiesColumns.values());
        this.shutdownRow = log.addTable(NibblerRemoteTables.SHUTDOWN_OMS, NibblerRemoteShutdownOMSColumns.values());
    }

    public void submit(final IOrderCmd cmd) {
        cmd.execute(this);
    }

    void submitOrder(final String username, final String symbol, final BookSide side, final OrderType orderType, final AlgoType algoType,
            final String tag, final long price, final int qty) {

        nibblerClient.submitOrder(username, symbol, side, orderType, algoType, tag, price, qty);
        nibblerClient.batchComplete();

        submitRow.set(NibblerRemoteSubmitColumns.USERNAME, username);
        submitRow.set(NibblerRemoteSubmitColumns.SYMBOL, symbol);
        submitRow.set(NibblerRemoteSubmitColumns.SIDE, side);
        submitRow.set(NibblerRemoteSubmitColumns.ORDER_TYPE, orderType);
        submitRow.set(NibblerRemoteSubmitColumns.ALGO_TYPE, algoType);
        submitRow.set(NibblerRemoteSubmitColumns.TAG, tag);
        submitRow.set(NibblerRemoteSubmitColumns.PRICE, price);
        submitRow.set(NibblerRemoteSubmitColumns.QTY, qty);
        writeRow(submitRow, NibblerRemoteSubmitColumns.timestamp);
    }

    void modifyOrder(final String username, final int chainID, final String symbol, final BookSide side, final OrderType orderType,
            final AlgoType algoType, final String tag, final long fromPrice, final int fromQty, final long toPrice, final int toQty) {

        nibblerClient.modifyOrder(username, chainID, symbol, side, orderType, algoType, tag, fromPrice, fromQty, toPrice, toQty);
        nibblerClient.batchComplete();

        modifyRow.set(NibblerRemoteModifyColumns.USERNAME, username);
        modifyRow.set(NibblerRemoteModifyColumns.CHAIN_ID, chainID);
        modifyRow.set(NibblerRemoteModifyColumns.SYMBOL, symbol);
        modifyRow.set(NibblerRemoteModifyColumns.SIDE, side);
        modifyRow.set(NibblerRemoteModifyColumns.ORDER_TYPE, orderType);
        modifyRow.set(NibblerRemoteModifyColumns.ALGO_TYPE, algoType);
        modifyRow.set(NibblerRemoteModifyColumns.TAG, tag);

        modifyRow.set(NibblerRemoteModifyColumns.FROM_PRICE, fromPrice);
        modifyRow.set(NibblerRemoteModifyColumns.FROM_QTY, fromQty);

        modifyRow.set(NibblerRemoteModifyColumns.TO_PRICE, toPrice);
        modifyRow.set(NibblerRemoteModifyColumns.TO_QTY, toQty);
        writeRow(modifyRow, NibblerRemoteModifyColumns.timestamp);
    }

    void cancelOrder(final String username, final boolean isAuto, final int chainID, final String symbol) {

        nibblerClient.cancelOrder(username, isAuto, chainID, symbol);
        nibblerClient.batchComplete();

        cancelRow.set(NibblerRemoteCancelColumns.USERNAME, username);
        cancelRow.set(NibblerRemoteCancelColumns.CHAIN_ID, chainID);
        cancelRow.set(NibblerRemoteCancelColumns.SYMBOL, symbol);
        writeRow(cancelRow, NibblerRemoteCancelColumns.timestamp);
    }

    public void stopAllStrategies(final String reason) {

        nibblerClient.stopAllStrategies(reason);
        nibblerClient.batchComplete();

        writeRow(stopStrategiesRow, NibblerRemoteStopAllStrategiesColumns.timestamp);
    }

    public void shutdownAllOMS(final String reason) {

        nibblerClient.shutdownAllOMS(reason);
        nibblerClient.batchComplete();

        writeRow(shutdownRow, NibblerRemoteShutdownOMSColumns.timestamp);
    }

    private <C extends Enum<C>> void writeRow(final FileTableRow<NibblerRemoteTables, C> row, final C timestampCol) {

        try {

            final String timestamp = sdf.format(clock.nowMilliUTC());
            row.set(timestampCol, timestamp);

            log.writeRow(row, true);

        } catch (final Exception e) {
            monitor.logError(ReddalComponents.BLOTTER_CONNECTION_LOG, "Failed to log [" + row.getTableName() + "] row.", e);
        }
    }
}
