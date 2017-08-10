package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.manager.utils.IStackClientListener;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.reddal.stacks.configui.StackConfigPresenter;
import com.drwtrading.london.reddal.stacks.family.StackChildListener;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesPresenter;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;

import java.util.HashSet;
import java.util.Set;

public class StackCallbackBatcher implements IStackClientListener {

    private final String nibblerName;

    private final StackStrategiesPresenter strategiesPresenter;
    private final StackConfigPresenter configPresenter;
    private final StackChildListener childListener;

    private final Set<StackStrategy> strategyBatch;
    private final Set<StackGroup> stackGroupBatch;
    private final Set<StackConfigGroup> configBatch;

    private final SpreadContractSetGenerator contractSetGenerator;

    public StackCallbackBatcher(final String nibblerName, final StackStrategiesPresenter strategiesPresenter,
            final StackConfigPresenter configPresenter, final StackChildListener childListener, final boolean isStackManager,
            final SpreadContractSetGenerator contractSetGenerator) {

        this.nibblerName = nibblerName;

        this.strategiesPresenter = strategiesPresenter;
        this.configPresenter = configPresenter;
        this.childListener = childListener;

        if (isStackManager) {
            this.contractSetGenerator = null;
        } else {
            this.contractSetGenerator = contractSetGenerator;
        }

        this.strategyBatch = new HashSet<>();
        this.stackGroupBatch = new HashSet<>();
        this.configBatch = new HashSet<>();
    }

    @Override
    public void connectionEstablished() {
        // no-op
    }

    @Override
    public void connectionLost() {
        configBatch.clear();
        strategiesPresenter.serverConnectionLost(nibblerName);
        configPresenter.serverConnectionLost(nibblerName);
        childListener.serverConnectionLost();
    }

    @Override
    public void strategyCreated(final StackStrategy strategy) {

        strategyBatch.add(strategy);

        final String symbol = strategy.getSymbol();
        if (null != contractSetGenerator) {
            contractSetGenerator.setStackRelationship(symbol, strategy.getLeanSymbol());
        }
        childListener.strategyCreated(strategy);
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
    public void remoteFillNotification(final String source, final StackGroup stackGroup, final StackType stackType, final long qty) {
        // no-op
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {
        childListener.stackGroupCreated(stackGroup);
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup) {
        stackGroupBatch.add(stackGroup);
    }

    @Override
    public void stackGroupInfoUpdated(final StackGroup stackGroup) {
        stackGroupBatch.add(stackGroup);
    }

    @Override
    public void batchComplete() {

        for (final StackStrategy strategy : strategyBatch) {
            try {
                strategiesPresenter.strategyUpdated(nibblerName, strategy);
                childListener.strategyUpdated(strategy);
            } catch (final Exception e) {
                System.out.println("Failed Strategy stack batch update.");
                e.printStackTrace();
            }
        }
        strategyBatch.clear();

        for (final StackGroup stackGroup : stackGroupBatch) {
            try {
                childListener.stackGroupUpdated(stackGroup);
            } catch (final Exception e) {
                System.out.println("Failed Stack Group batch update.");
                e.printStackTrace();
            }
        }
        stackGroupBatch.clear();

        for (final StackConfigGroup config : configBatch) {
            try {
                configPresenter.configUpdated(nibblerName, config);
            } catch (final Exception e) {
                System.out.println("Failed Config stack batch update.");
                e.printStackTrace();
            }
        }
        configBatch.clear();
    }
}
