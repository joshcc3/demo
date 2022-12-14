package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.Tag;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.stack.manager.persistence.CachingTimeFormatter;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.csv.fileTables.FileTableRow;
import com.drwtrading.london.eeif.utils.csv.fileTables.FileTableWriter;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteCancelColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteModifyColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteShutdownOMSColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteStartQuoterColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteStopAllStrategiesColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteStopQuoterColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteSubmitColumns;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteTables;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.logging.NibblerRemoteTraderLoginColumns;
import org.jetlang.channels.Publisher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class NibblerTransportOrderEntry {

    private final IClock clock;
    private final IFuseBox<ReddalComponents> monitor;

    private final NibblerClientHandler nibblerClient;

    private final CachingTimeFormatter ctf;
    private final FileTableWriter<NibblerRemoteTables> log;

    private final FileTableRow<NibblerRemoteTables, NibblerRemoteSubmitColumns> submitRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteModifyColumns> modifyRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteCancelColumns> cancelRow;

    private final FileTableRow<NibblerRemoteTables, NibblerRemoteStartQuoterColumns> startQuoterRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteStopQuoterColumns> stopQuoterRow;
    private final FileTableRow<NibblerRemoteTables, NibblerRemoteStopAllStrategiesColumns> stopStrategiesRow;

    private final FileTableRow<NibblerRemoteTables, NibblerRemoteShutdownOMSColumns> shutdownRow;

    private final FileTableRow<NibblerRemoteTables, NibblerRemoteTraderLoginColumns> traderLoginRow;

    private int prevClOrdID;

    public NibblerTransportOrderEntry(final IClock clock, final IFuseBox<ReddalComponents> monitor,
            final NibblerClientHandler nibblerClient, final Path logDir) throws IOException {

        this.clock = clock;
        this.monitor = monitor;

        this.nibblerClient = nibblerClient;

        this.ctf = new CachingTimeFormatter();

        final Path logFile = logDir.resolve(nibblerClient.getRemoteUser() + ".csv");
        this.log = new FileTableWriter<>(logFile, NibblerRemoteTables.class);

        this.submitRow = log.addTable(NibblerRemoteTables.SUBMIT, NibblerRemoteSubmitColumns.values());
        this.modifyRow = log.addTable(NibblerRemoteTables.MODIFY, NibblerRemoteModifyColumns.values());
        this.cancelRow = log.addTable(NibblerRemoteTables.CANCEL, NibblerRemoteCancelColumns.values());
        this.stopStrategiesRow = log.addTable(NibblerRemoteTables.STOP_ALL_STRATEGIES, NibblerRemoteStopAllStrategiesColumns.values());
        this.startQuoterRow = log.addTable(NibblerRemoteTables.START_QUOTER, NibblerRemoteStartQuoterColumns.values());
        this.stopQuoterRow = log.addTable(NibblerRemoteTables.STOP_QUOTER, NibblerRemoteStopQuoterColumns.values());
        this.shutdownRow = log.addTable(NibblerRemoteTables.SHUTDOWN_OMS, NibblerRemoteShutdownOMSColumns.values());
        this.traderLoginRow = log.addTable(NibblerRemoteTables.TRADER_LOGIN, NibblerRemoteTraderLoginColumns.values());

        this.prevClOrdID = -1;
    }

    public void submitOrder(final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final String symbol,
            final BookSide side, final OrderType orderType, final AlgoType algoType, final Tag tag, final long price, final int qty) {

        nibblerClient.submitOrder(++prevClOrdID, 0, user, symbol, side, orderType, algoType, tag, price, qty);
        if (!nibblerClient.batchComplete()) {
            rejectChannel.publish(new LadderClickTradingIssue(symbol, "Submit failed. Nibbler disconnected."));
        }

        submitRow.set(NibblerRemoteSubmitColumns.CLORDID, prevClOrdID);
        submitRow.set(NibblerRemoteSubmitColumns.USERNAME, user);
        submitRow.set(NibblerRemoteSubmitColumns.SYMBOL, symbol);
        submitRow.set(NibblerRemoteSubmitColumns.SIDE, side);
        submitRow.set(NibblerRemoteSubmitColumns.ORDER_TYPE, orderType);
        submitRow.set(NibblerRemoteSubmitColumns.ALGO_TYPE, algoType);
        submitRow.set(NibblerRemoteSubmitColumns.TAG, tag);
        submitRow.set(NibblerRemoteSubmitColumns.PRICE, price);
        submitRow.set(NibblerRemoteSubmitColumns.QTY, qty);
        writeRow(submitRow, NibblerRemoteSubmitColumns.timestamp);
    }

    public void modifyOrder(final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final int chainID, final String symbol,
            final BookSide side, final OrderType orderType, final AlgoType algoType, final Tag tag, final long fromPrice, final int fromQty,
            final long toPrice, final int toQty) {

        nibblerClient.modifyOrder(user, chainID, symbol, side, orderType, algoType, tag, fromPrice, fromQty, toPrice, toQty);
        if (!nibblerClient.batchComplete()) {
            rejectChannel.publish(new LadderClickTradingIssue(symbol, "Modify failed. Nibbler disconnected."));
        }

        modifyRow.set(NibblerRemoteModifyColumns.USERNAME, user);
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

    public void cancelOrder(final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final boolean isAuto,
            final int chainID, final String symbol) {

        nibblerClient.cancelOrder(user, isAuto, chainID, symbol);
        if (!nibblerClient.batchComplete()) {
            rejectChannel.publish(new LadderClickTradingIssue(symbol, "Cancel failed. Nibbler disconnected."));
        }

        cancelRow.set(NibblerRemoteCancelColumns.USERNAME, user);
        cancelRow.set(NibblerRemoteCancelColumns.CHAIN_ID, chainID);
        cancelRow.set(NibblerRemoteCancelColumns.SYMBOL, symbol);
        writeRow(cancelRow, NibblerRemoteCancelColumns.timestamp);
    }

    public void startQuoter(final int strategyID, final User user) {
        nibblerClient.startQuoter(strategyID, user);

        startQuoterRow.set(NibblerRemoteStartQuoterColumns.STRATEGY_ID, strategyID);
        startQuoterRow.set(NibblerRemoteStartQuoterColumns.USERNAME, user);
        writeRow(startQuoterRow, NibblerRemoteStartQuoterColumns.timestamp);

    }

    public void stopQuoter(final int strategyID) {
        nibblerClient.stopQuoter(strategyID);

        stopQuoterRow.set(NibblerRemoteStopQuoterColumns.STRATEGY_ID, strategyID);
        writeRow(stopQuoterRow, NibblerRemoteStopQuoterColumns.timestamp);

    }

    public void stopAllStrategies(final String reason) {

        nibblerClient.stopAllStrategies(reason);
        nibblerClient.batchComplete();

        stopStrategiesRow.set(NibblerRemoteStopAllStrategiesColumns.IS_MARKET_NUMBER, false);
        stopStrategiesRow.set(NibblerRemoteStopAllStrategiesColumns.IS_MARKET_NUMBER_ACKNOWLEDGED, false);
        stopStrategiesRow.set(NibblerRemoteStopAllStrategiesColumns.REASON, reason);
        writeRow(stopStrategiesRow, NibblerRemoteStopAllStrategiesColumns.timestamp);
    }

    public void stopAllForMarketNumber(final boolean isAcknowledged, final String reason) {

        nibblerClient.stopAllForMarketNumber(isAcknowledged, reason);
        nibblerClient.batchComplete();

        stopStrategiesRow.set(NibblerRemoteStopAllStrategiesColumns.IS_MARKET_NUMBER, true);
        stopStrategiesRow.set(NibblerRemoteStopAllStrategiesColumns.IS_MARKET_NUMBER_ACKNOWLEDGED, isAcknowledged);
        stopStrategiesRow.set(NibblerRemoteStopAllStrategiesColumns.REASON, reason);
        writeRow(stopStrategiesRow, NibblerRemoteStopAllStrategiesColumns.timestamp);
    }

    public void shutdownAllOMS(final String reason) {

        nibblerClient.shutdownAllOMS(reason);
        nibblerClient.batchComplete();

        writeRow(shutdownRow, NibblerRemoteShutdownOMSColumns.timestamp);
    }

    public void traderLogin(final Set<User> users) {

        final StringBuilder userNames = new StringBuilder();
        for (final User user : users) {
            nibblerClient.loginTrader(user);
            userNames.append(user.name());
            userNames.append(',');
            userNames.append(' ');
        }
        nibblerClient.batchComplete();

        if (0 < userNames.length()) {
            userNames.setLength(userNames.length() - 2);
        }
        traderLoginRow.set(NibblerRemoteTraderLoginColumns.users, userNames.toString());
        writeRow(traderLoginRow, NibblerRemoteTraderLoginColumns.timestamp);
    }

    public void batchComplete() {
        nibblerClient.batchComplete();
    }

    private <C extends Enum<C>> void writeRow(final FileTableRow<NibblerRemoteTables, C> row, final C timestampCol) {

        try {

            final String timestamp = ctf.format(clock.nowMilliUTC());
            row.set(timestampCol, timestamp);

            log.writeRow(row, true);

        } catch (final Exception e) {
            monitor.logError(ReddalComponents.BLOTTER_CONNECTION_LOG, "Failed to log [" + row.getTableName() + "] row.", e);
        }
    }

}
