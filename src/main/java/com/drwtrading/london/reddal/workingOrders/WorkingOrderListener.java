package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuotingObligationsPresenter;
import com.drwtrading.london.reddal.workingOrders.ui.WorkingOrdersPresenter;

public class WorkingOrderListener implements INibblerTradingDataListener {

    private final String sourceNibbler;

    private final WorkingOrdersPresenter workingOrdersPresenter;
    private final IWorkingOrdersCallback obligationPresenter;
    private final IWorkingOrdersCallback bestWorkingOrderMaintainer;
    private final QuotingObligationsPresenter quotingObligationsPresenter;

    private final LongMap<SourcedWorkingOrder> sourcedWorkingOrder;

    public WorkingOrderListener(final String sourceNibbler, final WorkingOrdersPresenter workingOrdersPresenter,
            final IWorkingOrdersCallback obligationPresenter, final IWorkingOrdersCallback bestWorkingOrderMaintainer,
            final QuotingObligationsPresenter quotingObligationsPresenter) {

        this.sourceNibbler = sourceNibbler;

        this.workingOrdersPresenter = workingOrdersPresenter;
        this.obligationPresenter = obligationPresenter;
        this.bestWorkingOrderMaintainer = bestWorkingOrderMaintainer;
        this.quotingObligationsPresenter = quotingObligationsPresenter;

        this.sourcedWorkingOrder = new LongMap<>();
    }

    @Override
    public boolean addTheoValue(final TheoValue theoValue) {
        return true;
    }

    @Override
    public boolean updateTheoValue(final TheoValue theoValue) {
        return true;
    }

    @Override
    public boolean addSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        return true;
    }

    @Override
    public boolean updateSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        return true;
    }

    @Override
    public boolean addQuotingState(final QuotingState quotingState) {
        quotingObligationsPresenter.setQuotingState(sourceNibbler, quotingState);
        return true;
    }

    @Override
    public boolean updateQuotingState(final QuotingState quotingState) {
        quotingObligationsPresenter.setQuotingState(sourceNibbler, quotingState);
        return true;
    }

    @Override
    public boolean addWorkingOrder(final WorkingOrder order) {

        final SourcedWorkingOrder sourcedOrder = new SourcedWorkingOrder(sourceNibbler, order);
        sourcedWorkingOrder.put(order.getWorkingOrderID(), sourcedOrder);

        workingOrdersPresenter.setWorkingOrder(sourcedOrder);
        obligationPresenter.setWorkingOrder(sourcedOrder);
        bestWorkingOrderMaintainer.setWorkingOrder(sourcedOrder);
        return true;
    }

    @Override
    public boolean updateWorkingOrder(final WorkingOrder order) {

        final SourcedWorkingOrder sourcedOrder = sourcedWorkingOrder.get(order.getWorkingOrderID());

        workingOrdersPresenter.setWorkingOrder(sourcedOrder);
        obligationPresenter.setWorkingOrder(sourcedOrder);
        bestWorkingOrderMaintainer.setWorkingOrder(sourcedOrder);
        return true;
    }

    @Override
    public boolean deleteWorkingOrder(final WorkingOrder order) {

        final SourcedWorkingOrder sourcedOrder = sourcedWorkingOrder.remove(order.getWorkingOrderID());

        workingOrdersPresenter.deleteWorkingOrder(sourcedOrder);
        obligationPresenter.deleteWorkingOrder(sourcedOrder);
        bestWorkingOrderMaintainer.deleteWorkingOrder(sourcedOrder);
        return true;
    }

    @Override
    public boolean addLastTrade(final LastTrade lastTrade) {
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
}
