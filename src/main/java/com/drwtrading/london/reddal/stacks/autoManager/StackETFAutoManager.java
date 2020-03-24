package com.drwtrading.london.reddal.stacks.autoManager;

import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.config.StackStrategyConfig;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.reddal.symbols.SymbolReferencePrice;

import java.util.HashMap;
import java.util.Map;

public class StackETFAutoManager implements IStackInstTypeAutoManager {

    private static final String SOURCE = "ETF_AUTO_MAN";

    private final Map<String, StackClientHandler> stackClients;
    private final Map<String, Map<String, LongMap<StackConfigGroup>>> stacksConfig;

    private final Map<String, SymbolReferencePrice> refPrices;

    StackETFAutoManager() {

        this.refPrices = new HashMap<>();

        this.stackClients = new HashMap<>();
        this.stacksConfig = new HashMap<>();
    }

    @Override
    public void setConfigClient(final String nibblerName, final StackClientHandler cache) {

        this.stackClients.put(nibblerName, cache);
        this.stacksConfig.put(nibblerName, new HashMap<>());
    }

    @Override
    public void configUpdated(final String nibbler, final StackConfigGroup configGroup) {

        final Map<String, LongMap<StackConfigGroup>> nibblerConfigs = stacksConfig.get(nibbler);
        final LongMap<StackConfigGroup> typedConfigs = MapUtils.getMappedItem(nibblerConfigs, configGroup.getSymbol(), LongMap::new);
        typedConfigs.put(configGroup.getConfigID(), configGroup);
    }

    @Override
    public void serverConnectionLost(final String nibblerName) {

        stacksConfig.get(nibblerName).clear();
    }

    @Override
    public void addRefPrice(final SymbolReferencePrice refPrice) {

        this.refPrices.put(refPrice.inst.getSymbol(), refPrice);
    }

    @Override
    public void setModLevels(final int bpsEquivalent) {

        for (final Map.Entry<String, Map<String, LongMap<StackConfigGroup>>> nibblerConfig : stacksConfig.entrySet()) {

            final String nibbler = nibblerConfig.getKey();
            final StackClientHandler clientHandler = stackClients.get(nibbler);

            final Map<String, LongMap<StackConfigGroup>> configGroups = nibblerConfig.getValue();
            for (final LongMap<StackConfigGroup> typedConfigs : configGroups.values()) {

                for (final LongMapNode<StackConfigGroup> configGroupNode : typedConfigs) {

                    final StackConfigGroup configGroup = configGroupNode.getValue();
                    final SymbolReferencePrice refPrice = refPrices.get(configGroup.getSymbol());
                    if (null != refPrice) {

                        final long nextPrice = refPrice.yestClosePrice + (bpsEquivalent * refPrice.yestClosePrice) / 1_00_00;

                        final ITickTable tickTable = refPrice.inst.getTickTable();
                        final int basicModLevels = (int) tickTable.getTicksBetween(refPrice.yestClosePrice, nextPrice);
                        final int modLevels = Math.max(1, basicModLevels);

                        final StackStrategyConfig bidConfig = configGroup.bidStrategyConfig;
                        setModLevels(clientHandler, BookSide.BID, configGroup.configGroupID, bidConfig, modLevels);

                        final StackStrategyConfig askConfig = configGroup.askStrategyConfig;
                        setModLevels(clientHandler, BookSide.ASK, configGroup.configGroupID, askConfig, modLevels);
                    }
                }
            }
            clientHandler.batchComplete();
        }
    }

    private static void setModLevels(final StackClientHandler clientHandler, final BookSide side, final long configGroupID,
            final StackStrategyConfig config, final int modLevels) {

        clientHandler.strategyConfigUpdated(SOURCE, configGroupID, side, config.getMaxOrdersPerLevel(), config.isOnlySubmitBestLevel(),
                config.isQuoteBettermentOn(), modLevels, config.getQuoteFlickerBufferPercent(), config.getQuotePicardMaxBPSThrough(),
                config.getPicardMaxPapaWeight(), config.getPicardMaxPerSec(), config.getPicardMaxPerMin(), config.getPicardMaxPerHour(),
                config.getPicardMaxPerDay());
    }
}
