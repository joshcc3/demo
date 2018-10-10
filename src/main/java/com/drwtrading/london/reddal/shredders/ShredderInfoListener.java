package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;

public class ShredderInfoListener implements INibblerTradingDataListener {

    private final ShredderPresenter shredderPresenter;

    private final LongMap<WorkingOrder> workingOrders;

    public ShredderInfoListener(final ShredderPresenter shredderPresenter) {

        this.shredderPresenter = shredderPresenter;

        this.workingOrders = new LongMap<>();
    }

    @Override
    public boolean addTheoValue(final TheoValue theoValue) {
        shredderPresenter.setTheo(theoValue);
        return true;
    }

    @Override
    public boolean updateTheoValue(final TheoValue theoValue) {
        shredderPresenter.setTheo(theoValue);
        return true;
    }

    @Override
    public boolean addSpreadnoughtTheo(final SpreadnoughtTheo theoValue) {
        shredderPresenter.setSpreadnoughtTheo(theoValue);
        return true;
    }

    @Override
    public boolean updateSpreadnoughtTheo(final SpreadnoughtTheo theoValue) {
        shredderPresenter.setSpreadnoughtTheo(theoValue);
        return true;
    }

    @Override
    public boolean addWorkingOrder(final WorkingOrder workingOrder) {

        return updateWorkingOrder(workingOrder);
    }

    @Override
    public boolean updateWorkingOrder(final WorkingOrder workingOrder) {

        workingOrders.put(workingOrder.getWorkingOrderID(), workingOrder);
        shredderPresenter.setWorkingOrder(workingOrder);
        return true;
    }

    @Override
    public boolean deleteWorkingOrder(final WorkingOrder workingOrder) {

        workingOrders.remove(workingOrder.getWorkingOrderID());
        shredderPresenter.deleteWorkingOrder(workingOrder);
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

    public void connectionLost() {

        for (final LongMapNode<WorkingOrder> workingOrderNode : workingOrders) {

            final WorkingOrder workingOrder = workingOrderNode.getValue();
            shredderPresenter.deleteWorkingOrder(workingOrder);
        }
        workingOrders.clear();
    }
}
