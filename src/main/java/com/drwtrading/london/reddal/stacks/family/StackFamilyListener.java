package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.cache.stack.IStackGroupCacheListener;
import com.drwtrading.london.eeif.stack.transport.cache.strategy.IStackStrategyCacheListener;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;

import java.util.HashMap;
import java.util.Map;

public class StackFamilyListener implements IStackStrategyCacheListener, IStackGroupCacheListener {

    private final StackFamilyPresenter presenter;

    private final Map<String, StackUIData> uiDataBySymbol;

    private final LongMap<StackUIData> uiDataByStrategyID;

    private final LongMap<StackUIData> uiDataByStackID;
    private final LongMap<BookSide> stackSide;

    public StackFamilyListener(final StackFamilyPresenter presenter) {

        this.presenter = presenter;

        this.uiDataBySymbol = new HashMap<>();

        this.uiDataByStrategyID = new LongMap<>();

        this.uiDataByStackID = new LongMap<>();
        this.stackSide = new LongMap<>();
    }

    @Override
    public boolean strategyCreated(final String source, final long strategyID, final String familyName, final InstrumentID instID,
            final InstType leanInstType, final String leanSymbol, final InstrumentID leanInstID, final String additiveSymbol) {

        presenter.addFamily(familyName);

        final StackUIData uiData = new StackUIData(familyName);

        uiDataBySymbol.put(familyName, uiData);
        uiDataByStrategyID.put(strategyID, uiData);

        presenter.addUIData(uiData);
        return true;
    }

    @Override
    public boolean pendingRequirementsUpdated(final String source, final long strategyID, final boolean isQuoteInstDefEventAvailable,
            final boolean isQuoteBookAvailable, final boolean isLeanBookAvailable, final boolean isFXAvailable,
            final boolean isAdditiveAvailable) {
        return true;
    }

    @Override
    public boolean setSelectedConfig(final String source, final long strategyID, final StackConfigType selectedConfigType) {

        final StackUIData uiData = uiDataByStrategyID.get(strategyID);
        uiData.setSelectedConfig(selectedConfigType);
        presenter.updateUIData(uiData);
        return true;
    }

    @Override
    public boolean stackCreated(final String source, final long stackGroupID, final String symbol, final InstrumentID instID,
            final BookSide side) {

        final StackUIData uiData = uiDataBySymbol.get(symbol);
        uiDataByStackID.put(stackGroupID, uiData);
        stackSide.put(stackGroupID, side);
        return true;
    }

    @Override
    public boolean stackCleared(final String source, final long stackGroupID) {
        return true;
    }

    @Override
    public boolean updateStackGroup(final String source, final long stackGroupID, final double priceOffsetBPS,
            final double priceOffsetTickSize, final int tickMultiplier, final double stackAlignmentTickToBPS) {

        final StackUIData uiData = uiDataByStackID.get(stackGroupID);
        final BookSide side = stackSide.get(stackGroupID);
        if (BookSide.BID == side) {
            uiData.setBidStacks(priceOffsetBPS);
        } else {
            uiData.setAskStacks(priceOffsetBPS);
        }
        presenter.updateUIData(uiData);
        return true;
    }

    @Override
    public boolean setStackEnabled(final String source, final long stackGroupID, final StackType stackType, final boolean isEnabled) {

        final StackUIData uiData = uiDataByStackID.get(stackGroupID);
        final BookSide side = stackSide.get(stackGroupID);
        uiData.setStackEnabled(side, stackType, isEnabled);
        presenter.updateUIData(uiData);
        return true;
    }

    @Override
    public boolean adjustStackLevels(final String source, final long stackGroupID, final StackType stackType, final int tickAdjustment) {
        return true;
    }

    @Override
    public boolean setStackQty(final String source, final long stackGroupID, final StackType stackType, final StackOrderType orderType,
            final int pullbackTicks, final long qty) {
        return true;
    }

    @Override
    public boolean addStackQty(final String source, final long stackGroupID, final StackType stackType, final StackOrderType orderType,
            final int pullbackTicks, final long qty) {
        return true;
    }

    @Override
    public boolean remoteFillNotification(final String source, final long stackGroupID, final StackType stackType,
            final int maxPullbackTicks, final long qty) {
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

}
