package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.data.symbology.StackTradableSymbol;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.reddal.stacks.opxl.OpxlStrategyOffsetsUI;

import java.util.HashMap;
import java.util.Map;

public class StackChildListener {

    private final String nibblerName;
    private final boolean isOffsetWanted;

    private final StackFamilyPresenter presenter;
    private final OpxlStrategyOffsetsUI offsetsOPXL;

    private final Map<String, StackUIData> symbolUIData;

    private final LongMap<StackUIData> stackGroupUIData;

    public StackChildListener(final String nibblerName, final boolean isManager, final StackFamilyPresenter presenter,
            final OpxlStrategyOffsetsUI offsetsOPXL) {

        this.nibblerName = nibblerName;
        this.isOffsetWanted = !isManager;

        this.presenter = presenter;
        this.offsetsOPXL = offsetsOPXL;

        this.symbolUIData = new HashMap<>();

        this.stackGroupUIData = new LongMap<>();
    }

    public void addTradableSymbol(final String nibblerName, final StackTradableSymbol tradableSymbol) {
        presenter.addTradableSymbol(nibblerName, tradableSymbol);
    }

    public void strategyCreated(final StackStrategy strategy) {

        final String symbol = strategy.getSymbol();
        final StackUIData uiData =
                new StackUIData(nibblerName, symbol, strategy.getInstID(), strategy.getLeanSymbol(), strategy.getLeanInstType(),
                        strategy.getAdditiveSymbol());

        symbolUIData.put(symbol, uiData);

        presenter.addChildUIData(uiData);
    }

    public void stackGroupCreated(final StackGroup stackGroup) {

        final String symbol = stackGroup.getSymbol();
        final long stackGroupID = stackGroup.getStackID();

        final StackUIData uiData = symbolUIData.get(symbol);
        stackGroupUIData.put(stackGroupID, uiData);

        uiData.stackGroupCreated(stackGroup);

        presenter.updateChildUIData(uiData);
        if (isOffsetWanted) {
            offsetsOPXL.setStrategyOffsets(uiData);
        }
    }

    public void stackGroupUpdated(final StackGroup stackGroup) {

        final StackUIData uiData = stackGroupUIData.get(stackGroup.getStackID());
        uiData.stackGroupUpdated(stackGroup, false);

        presenter.updateChildUIData(uiData);
        if (isOffsetWanted) {
            offsetsOPXL.setStrategyOffsets(uiData);
        }
    }

    public void setConfig(final StackConfigGroup config) {
        presenter.setConfig(config);
    }

    public void serverConnectionLost() {

        for (final StackUIData uiData : symbolUIData.values()) {

            uiData.clear();
            presenter.updateChildUIData(uiData);
            offsetsOPXL.setStrategyOffsets(uiData);
        }
    }
}
