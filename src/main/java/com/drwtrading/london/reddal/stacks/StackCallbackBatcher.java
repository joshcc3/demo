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
import com.drwtrading.london.reddal.SpreadContractSet;
import com.drwtrading.london.reddal.stacks.configui.StackConfigUIRouter;
import com.drwtrading.london.reddal.stacks.opxl.StackGroupOPXLView;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesNibblerView;
import org.jetlang.channels.Publisher;

import java.util.HashSet;
import java.util.Set;

public class StackCallbackBatcher
        implements IStackConnectionListener, IStackGroupUpdateCallback, IStackConfigUpdateCallback, IStackStrategyUpdateCallback {

    private final String nibblerName;

    private final StackStrategiesNibblerView strategiesPresenter;
    private final StackConfigUIRouter configPresenter;
    private final StackGroupOPXLView stackOPXLView;

    private final Set<StackStrategy> strategyBatch;
    private final Set<StackConfigGroup> configBatch;
    private final Set<StackGroup> groupBatch;

    private final Publisher<SpreadContractSet> stackContractSetPublisher;

    public StackCallbackBatcher(final String nibblerName, final StackStrategiesNibblerView strategiesPresenter,
            final StackConfigUIRouter configPresenter, final StackGroupOPXLView stackOPXLView,
            final Publisher<SpreadContractSet> stackContractSetPublisher) {

        this.nibblerName = nibblerName;

        this.strategiesPresenter = strategiesPresenter;
        this.configPresenter = configPresenter;
        this.stackOPXLView = stackOPXLView;

        this.stackContractSetPublisher = stackContractSetPublisher;

        this.strategyBatch = new HashSet<>();
        this.configBatch = new HashSet<>();
        this.groupBatch = new HashSet<>();
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
        strategiesPresenter.serverConnectionLost(remoteAppName);
        configPresenter.serverConnectionLost(nibblerName);
    }

    @Override
    public IStackStrategyUpdateCallback getStrategyListener(final String symbol, final InstrumentID instID, final String leanSymbol,
            final InstrumentID leanInstID) {

        if (null == strategiesPresenter) {
            throw new IllegalStateException("Strategy presenter has not been set.");
        } else {
            return this;
        }
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
    public void strategyCreated(final StackStrategy strategy) {
        strategiesPresenter.strategyCreated(strategy);

        final String symbol = strategy.getSymbol();
        final SpreadContractSet contractSet = new SpreadContractSet(symbol, strategy.getLeanSymbol(), symbol + ";S");
        stackContractSetPublisher.publish(contractSet);
    }

    @Override
    public void strategyUpdated(final StackStrategy strategy) {
        strategyBatch.add(strategy);
    }

    @Override
    public void configGroupCreated(final StackConfigGroup configGroup) {
        configPresenter.configUpdated(nibblerName, configGroup);
    }

    @Override
    public void configUpdated(final StackConfigGroup configGroup) {
        configBatch.add(configGroup);
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {
        groupBatch.add(stackGroup);
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup) {
        groupBatch.add(stackGroup);
    }

    @Override
    public void batchComplete() {

        for (final StackStrategy strategy : strategyBatch) {
            try {
                strategiesPresenter.strategyUpdated(strategy);

            } catch (final Exception e) {
                System.out.println("Failed Strategy stack batch update.");
                e.printStackTrace();
            }
        }
        strategyBatch.clear();

        for (final StackConfigGroup config : configBatch) {
            try {
                configPresenter.configUpdated(nibblerName, config);
            } catch (final Exception e) {
                System.out.println("Failed Config stack batch update.");
                e.printStackTrace();
            }
        }
        configBatch.clear();

        for (final StackGroup group : groupBatch) {
            try {
                stackOPXLView.setStackGroup(group);
            } catch (final Exception e) {
                System.out.println("Failed Group stack batch update.");
                e.printStackTrace();
            }
        }
        groupBatch.clear();
    }
}
