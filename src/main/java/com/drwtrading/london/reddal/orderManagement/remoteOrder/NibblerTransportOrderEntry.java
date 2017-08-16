package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class NibblerTransportOrderEntry {

    private final NibblerClientHandler nibblerClient;

    public NibblerTransportOrderEntry(final NibblerClientHandler nibblerClient) {
        this.nibblerClient = nibblerClient;
    }

    public void submit(final IOrderCmd cmd) {
        cmd.execute(this);
    }

    void submitOrder(final String username, final String symbol, final BookSide side, final OrderType orderType, final AlgoType algoType,
            final String tag, final long price, final int qty) {

        nibblerClient.submitOrder(username, symbol, side, orderType, algoType, tag, price, qty);
        nibblerClient.batchComplete();
    }

    void modifyOrder(final String username, final int chainID, final String symbol, final BookSide side, final OrderType orderType,
            final AlgoType algoType, final String tag, final long fromPrice, final int fromQty, final long toPrice, final int toQty) {

        nibblerClient.modifyOrder(username, chainID, symbol, side, orderType, algoType, tag, fromPrice, fromQty, toPrice, toQty);
        nibblerClient.batchComplete();
    }

    void cancelOrder(final String username, final boolean isUserLogin, final int chainID, final String symbol) {

        nibblerClient.cancelOrder(username, isUserLogin, chainID, symbol);
        nibblerClient.batchComplete();
    }

    public void stopAllStrategies(final String reason) {
        nibblerClient.stopAllStrategies(reason);
        nibblerClient.batchComplete();
    }

    public void shutdownAllOMS(final String reason) {
        nibblerClient.shutdownAllOMS(reason);
        nibblerClient.batchComplete();
    }
}
