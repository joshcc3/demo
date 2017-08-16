package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.transport.cache.IStackConnectionListener;
import com.drwtrading.london.eeif.stack.transport.cache.config.IStackConfigUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.cache.stack.IStackGroupUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.cache.strategy.IStackStrategyUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.ladders.LadderPresenter;

import java.util.HashSet;
import java.util.Set;

public class StackGroupCallbackBatcher
        implements IStackConnectionListener, IStackGroupUpdateCallback, IStackConfigUpdateCallback, IStackStrategyUpdateCallback {

    private final LadderPresenter ladderPresenter;

    private final Set<StackGroup> stackBatch;

    private StackClientHandler stackClientHandler;

    public StackGroupCallbackBatcher(final LadderPresenter ladderPresenter) {

        this.ladderPresenter = ladderPresenter;

        this.stackBatch = new HashSet<>();
    }

    public void setStackClient(final StackClientHandler stackClientHandler) {

        this.stackClientHandler = stackClientHandler;
    }

    @Override
    public IStackStrategyUpdateCallback getStrategyListener(final String symbol, final InstrumentID instID, final String leanSymbol,
            final InstrumentID leanInstID, final String additiveSymbol) {

        return this;
    }

    @Override
    public IStackGroupUpdateCallback getStackListener(final String symbol, final InstrumentID instID, final BookSide side) {

        if (null == ladderPresenter) {
            throw new IllegalStateException("Ladder presenter has not been set.");
        } else {
            return this;
        }
    }

    @Override
    public IStackConfigUpdateCallback getConfigListener(final String symbol, final InstrumentID instID) {
        return this;
    }

    @Override
    public boolean connectionEstablished(final String remoteAppName) {
        return true;
    }

    @Override
    public void connectionLost(final String remoteAppName) {
        stackBatch.clear();
        ladderPresenter.stacksConnectionLost(remoteAppName);
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {
        ladderPresenter.stackGroupCreated(stackGroup, stackClientHandler);
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup) {
        stackBatch.add(stackGroup);
    }

    @Override
    public void stackGroupInfoUpdated(final StackGroup stackGroup) {
        stackBatch.add(stackGroup);
    }

    @Override
    public void remoteFillNotification(final String source, final StackGroup stackGroup, final StackType stackType,
            final int maxPullbackTicks, final long qty) {
        // no-op
    }

    @Override
    public void configGroupCreated(final StackConfigGroup configGroup) {
        // no-op
    }

    @Override
    public void configUpdated(final StackConfigGroup configGroup) {
        // no-op
    }

    @Override
    public void batchComplete() {

        for (final StackGroup group : stackBatch) {
            try {
                ladderPresenter.stackGroupUpdated(group);
            } catch (final Exception e) {
                System.out.println("Failed stack batch update.");
                e.printStackTrace();
            }
        }
        stackBatch.clear();
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
