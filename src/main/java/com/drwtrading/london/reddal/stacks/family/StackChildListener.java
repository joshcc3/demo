package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.cache.strategy.IStackStrategyCacheListener;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.staticData.InstType;

public class StackChildListener implements IStackStrategyCacheListener {

    private final StackFamilyPresenter presenter;

    public StackChildListener(final StackFamilyPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public boolean strategyCreated(final String source, final long strategyID, final String familyName, final InstrumentID instID,
            final InstType leanInstType, final String leanSymbol, final InstrumentID leanInstID, final String additiveSymbol) {

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
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

}
