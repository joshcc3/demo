package com.drwtrading.london.reddal.nibblers.tradingData;

import com.drwtrading.london.eeif.nibbler.transport.INibblerTransportConnectionListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.blotter.INibblerBlotterListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterLine;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterSymbolLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TradableInstrument;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.reddal.autopull.autopuller.onMD.AutoPuller;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.impliedGenerator.ImpliedTheoInfoGenerator;
import com.drwtrading.london.reddal.ladders.orders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;

public class LadderInfoListener implements INibblerTradingDataListener, INibblerTransportConnectionListener, INibblerBlotterListener {

    private final String sourceNibbler;
    private final LadderPresenter ladderPresenter;
    private final OrdersPresenter orderPresenter;
    private final ShredderPresenter shredderPresenter;
    private final AutoPuller autoPuller;
    private final ImpliedTheoInfoGenerator impliedGenerator;

    private final LongMap<SourcedWorkingOrder> sourcedWorkingOrder;

    public LadderInfoListener(final String sourceNibbler, final LadderPresenter ladderPresenter, final OrdersPresenter orderPresenter,
            final ShredderPresenter shredderPresenter, final AutoPuller autoPuller, final ImpliedTheoInfoGenerator impliedGenerator) {

        this.sourceNibbler = sourceNibbler;
        this.ladderPresenter = ladderPresenter;
        this.orderPresenter = orderPresenter;
        this.shredderPresenter = shredderPresenter;
        this.autoPuller = autoPuller;
        this.impliedGenerator = impliedGenerator;

        this.sourcedWorkingOrder = new LongMap<>();
    }

    @Override
    public boolean connectionEstablished(final String remoteAppName) {
        return true;
    }

    @Override
    public boolean addTradableInst(final TradableInstrument tradableInstrument) {
        ladderPresenter.addTradableInstrument(tradableInstrument);
        return true;
    }

    @Override
    public boolean addTheoValue(final TheoValue theoValue) {
        return updateTheoValue(theoValue);
    }

    @Override
    public boolean updateTheoValue(final TheoValue theoValue) {
        ladderPresenter.setTheo(theoValue);
        shredderPresenter.setTheo(theoValue);
        impliedGenerator.setTheoValue(theoValue);
        return true;
    }

    @Override
    public boolean addSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        return updateSpreadnoughtTheo(theo);
    }

    @Override
    public boolean updateSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        ladderPresenter.setSpreadnoughtTheo(theo);
        shredderPresenter.setSpreadnoughtTheo(theo);
        return true;
    }

    @Override
    public boolean addQuotingState(final QuotingState quotingState) {
        return true;
    }

    @Override
    public boolean updateQuotingState(final QuotingState quotingState) {
        return true;
    }

    @Override
    public boolean addWorkingOrder(final WorkingOrder order) {

        final SourcedWorkingOrder sourcedOrder = new SourcedWorkingOrder(sourceNibbler, order);
        sourcedWorkingOrder.put(order.getWorkingOrderID(), sourcedOrder);
        ladderPresenter.setWorkingOrder(sourcedOrder);
        orderPresenter.setWorkingOrder(sourcedOrder);
        autoPuller.setWorkingOrder(sourcedOrder);
        shredderPresenter.setWorkingOrder(sourcedOrder);
        return true;
    }

    @Override
    public boolean updateWorkingOrder(final WorkingOrder order) {

        final SourcedWorkingOrder sourcedOrder = sourcedWorkingOrder.get(order.getWorkingOrderID());
        ladderPresenter.setWorkingOrder(sourcedOrder);
        orderPresenter.setWorkingOrder(sourcedOrder);
        autoPuller.setWorkingOrder(sourcedOrder);
        shredderPresenter.setWorkingOrder(sourcedOrder);
        return true;
    }

    @Override
    public boolean deleteWorkingOrder(final WorkingOrder order) {

        final SourcedWorkingOrder sourcedOrder = sourcedWorkingOrder.remove(order.getWorkingOrderID());
        ladderPresenter.deleteWorkingOrder(sourcedOrder);
        orderPresenter.deleteWorkingOrder(sourcedOrder);
        autoPuller.deleteWorkingOrder(sourcedOrder);
        shredderPresenter.deleteWorkingOrder(sourcedOrder);
        return true;
    }

    @Override
    public boolean workingOrderSnapshotComplete() {
        return true;
    }

    @Override
    public boolean addLastTrade(final LastTrade lastTrade) {
        ladderPresenter.setLastTrade(lastTrade);
        return true;
    }

    @Override
    public boolean updateLastTrade(final LastTrade lastTrade) {
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

    @Override
    public void connectionLost(final String remoteAppName) {

        for (final LongMapNode<SourcedWorkingOrder> sourcedOrderNode : sourcedWorkingOrder) {

            final SourcedWorkingOrder sourcedWorkingOrder = sourcedOrderNode.getValue();
            ladderPresenter.deleteWorkingOrder(sourcedWorkingOrder);
            orderPresenter.deleteWorkingOrder(sourcedWorkingOrder);
            shredderPresenter.deleteWorkingOrder(sourcedWorkingOrder);
        }

        sourcedWorkingOrder.clear();
    }

    @Override
    public boolean addBlotterLine(final BlotterLine blotterLine) {
        return true;
    }

    @Override
    public boolean addBlotterSymbolLine(final BlotterSymbolLine blotterLine) {
        ladderPresenter.displayTradeIssue(blotterLine.symbol, blotterLine.text);
        return true;
    }
}
