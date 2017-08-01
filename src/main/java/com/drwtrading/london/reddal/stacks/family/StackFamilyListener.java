package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.manager.persistence.StackNoOpCacheListener;
import com.drwtrading.london.eeif.stack.transport.cache.stack.IStackGroupCacheListener;
import com.drwtrading.london.eeif.stack.transport.cache.strategy.IStackStrategyCacheListener;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroupFactory;
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
    private final LongMap<StackGroup> stackGroups;

    private final StackGroupFactory stackGroupFactory;

    public StackFamilyListener(final StackFamilyPresenter presenter) {

        this.presenter = presenter;

        this.uiDataBySymbol = new HashMap<>();

        this.uiDataByStrategyID = new LongMap<>();

        this.uiDataByStackID = new LongMap<>();
        this.stackGroups = new LongMap<>();

        this.stackGroupFactory = new StackGroupFactory();
    }

    @Override
    public boolean strategyCreated(final String source, final long strategyID, final String familyName, final InstrumentID instID,
            final InstType leanInstType, final String leanSymbol, final InstrumentID leanInstID, final String additiveSymbol) {

        presenter.addFamily(familyName);

        final StackUIData uiData = new StackUIData(source, familyName);

        uiDataBySymbol.put(familyName, uiData);
        uiDataByStrategyID.put(strategyID, uiData);

        presenter.addFamilyUIData(uiData);
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
        presenter.updateFamilyUIData(uiData);
        return true;
    }

    @Override
    public boolean stackCreated(final String source, final long stackGroupID, final String symbol, final InstrumentID instID,
            final BookSide side) {

        final StackUIData uiData = uiDataBySymbol.get(symbol);
        uiDataByStackID.put(stackGroupID, uiData);

        final StackGroup stackGroup =
                stackGroupFactory.createStackGroup(stackGroupID, symbol, instID, side, StackNoOpCacheListener.INSTANCE, uiData);
        stackGroups.put(stackGroupID, stackGroup);

        uiData.stackGroupCreated(stackGroup);
        return true;
    }

    @Override
    public boolean stackCleared(final String source, final long stackGroupID) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.clear(source);
        return true;
    }

    @Override
    public boolean updateStackGroup(final String source, final long stackGroupID, final double priceOffsetBPS,
            final double priceOffsetTickSize, final int tickMultiplier, final double stackAlignmentTickToBPS) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.setGroupData(source, priceOffsetBPS, priceOffsetTickSize, tickMultiplier, stackAlignmentTickToBPS);

        return updateUI(stackGroupID);
    }

    @Override
    public boolean setStackEnabled(final String source, final long stackGroupID, final StackType stackType, final boolean isEnabled) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.setStackEnabled(source, stackType, isEnabled);

        return updateUI(stackGroupID);
    }

    @Override
    public boolean adjustStackLevels(final String source, final long stackGroupID, final StackType stackType, final int tickAdjustment) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.adjustStackLevels(source, stackType, tickAdjustment);

        return updateUI(stackGroupID);
    }

    @Override
    public boolean setStackQty(final String source, final long stackGroupID, final StackType stackType, final StackOrderType orderType,
            final int pullbackTicks, final long qty) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.setStackQty(source, stackType, orderType, pullbackTicks, qty);

        return updateUI(stackGroupID);
    }

    @Override
    public boolean addStackQty(final String source, final long stackGroupID, final StackType stackType, final StackOrderType orderType,
            final int pullbackTicks, final long qty) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.addStackQty(source, stackType, orderType, pullbackTicks, qty);

        return updateUI(stackGroupID);
    }

    @Override
    public boolean stackGroupInfoUpdated(final String source, final long stackGroupID, final boolean isStrategyRunning,
            final String strategyInfo) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.setStackGroupInfo(source, isStrategyRunning, strategyInfo);

        return updateUI(stackGroupID);
    }

    @Override
    public boolean remoteFillNotification(final String source, final long stackGroupID, final StackType stackType,
            final int maxPullbackTicks, final long qty) {

        final StackGroup stackGroup = stackGroups.get(stackGroupID);
        stackGroup.remoteFillNotification(source, stackType, maxPullbackTicks, qty);

        return updateUI(stackGroupID);
    }

    private boolean updateUI(final long stackGroupID) {

        final StackUIData uiData = uiDataByStackID.get(stackGroupID);
        presenter.updateFamilyUIData(uiData);
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }
}
