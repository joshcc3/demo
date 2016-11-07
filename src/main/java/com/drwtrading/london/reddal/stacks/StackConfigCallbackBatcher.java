package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.transport.cache.IStackConnectionListener;
import com.drwtrading.london.eeif.stack.transport.cache.config.IStackConfigUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.cache.stack.IStackGroupUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.cache.strategy.IStackStrategyUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.stacks.configui.StackConfigNibblerView;

import java.util.HashSet;
import java.util.Set;

public class StackConfigCallbackBatcher
        implements IStackConnectionListener, IStackGroupUpdateCallback, IStackConfigUpdateCallback, IStackStrategyUpdateCallback {

    private final StackConfigNibblerView configPresenter;

    private final Set<StackConfigGroup> configBatch;

    public StackConfigCallbackBatcher(final StackConfigNibblerView configPresenter) {

        this.configPresenter = configPresenter;

        this.configBatch = new HashSet<>();
    }

    @Override
    public IStackStrategyUpdateCallback getStrategyListener(final String symbol, final InstrumentID instID, final String leanSymbol,
            final InstrumentID leanInstID) {
        return this;
    }

    @Override
    public IStackGroupUpdateCallback getStackListener(final String symbol, final InstrumentID instID, final BookSide side) {
        return this;
    }

    @Override
    public boolean connectionEstablished(final String remoteAppName) {
        return true;
    }

    @Override
    public void connectionLost(final String remoteAppName) {
        configBatch.clear();
        configPresenter.serverConnectionLost();
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup) {
    }

    @Override
    public IStackConfigUpdateCallback getConfigListener(final String symbol, final InstrumentID instID) {

        if (null == configPresenter) {
            throw new IllegalStateException("Config presenter has not been set.");
        } else {
            return this;
        }
    }

    @Override
    public void configGroupCreated(final StackConfigGroup configGroup) {
        configPresenter.configGroupCreated(configGroup);
    }

    @Override
    public void configUpdated(final StackConfigGroup configGroup) {
        configBatch.add(configGroup);
    }

    @Override
    public void batchComplete() {

        for (final StackConfigGroup config : configBatch) {
            try {
                configPresenter.configUpdated(config);
            } catch (final Exception e) {
                System.out.println("Failed stack batch update.");
                e.printStackTrace();
            }
        }
        configBatch.clear();
    }

    @Override
    public void strategyCreated(final StackStrategy strategy) {
        // no-op
    }

    @Override
    public void strategyUpdated(final StackStrategy strategy) {
        // no-op
    }
}
