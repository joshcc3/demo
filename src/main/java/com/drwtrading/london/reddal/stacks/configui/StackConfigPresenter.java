package com.drwtrading.london.reddal.stacks.configui;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.stack.transport.data.config.StackAdditiveConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.config.StackFXConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackLeanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackPlanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackQuoteConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackStrategyConfig;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StackConfigPresenter {

    private static final String SOURCE = "CONFIG_UI";

    private static final Collection<String> STACK_CONFIG_TYPES;

    static {
        final StackConfigType[] configTypes = StackConfigType.values();
        STACK_CONFIG_TYPES = new ArrayList<>(configTypes.length);
        for (final StackConfigType configType : configTypes) {
            STACK_CONFIG_TYPES.add(configType.name());
        }
    }

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    private final WebSocketViews<IStackConfigUI> views;

    private final Map<String, LongMap<StackConfigGroup>> nibblerConfigs;
    private final Map<String, StackClientHandler> configClients;

    public StackConfigPresenter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.views = WebSocketViews.create(IStackConfigUI.class, this);

        this.nibblerConfigs = new HashMap<>();
        this.configClients = new HashMap<>();
    }

    public void setConfigClient(final String nibblerName, final StackClientHandler cache) {
        this.configClients.put(nibblerName, cache);
        this.nibblerConfigs.put(nibblerName, new LongMap<>());
    }

    public void configUpdated(final String nibblerName, final StackConfigGroup configGroup) {

        final LongMap<StackConfigGroup> stackConfigs = nibblerConfigs.get(nibblerName);
        stackConfigs.put(configGroup.configGroupID, configGroup);
        sendLine(views.all(), nibblerName, configGroup);
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("StackConfig", msg));

        final String data = msg.getData();
        if ("subscribe".equals(data)) {
            addUI(msg.getOutboundChannel());
        } else {
            views.invoke(msg);
        }
    }

    public void addUI(final Publisher<WebSocketOutboundData> channel) {

        final IStackConfigUI newView = views.get(channel);
        newView.setConfigTypes(STACK_CONFIG_TYPES);

        for (final Map.Entry<String, LongMap<StackConfigGroup>> stackConfigs : nibblerConfigs.entrySet()) {

            final String nibblerName = stackConfigs.getKey();
            for (final LongMapNode<StackConfigGroup> configNode : stackConfigs.getValue()) {

                final StackConfigGroup config = configNode.getValue();
                sendLine(newView, nibblerName, config);
            }
        }
    }

    private static void sendLine(final IStackConfigUI viewer, final String nibblerName, final StackConfigGroup configGroup) {

        final StackQuoteConfig quoteConfig = configGroup.quoteConfig;
        final StackFXConfig fxConfig = configGroup.fxConfig;
        final StackLeanConfig leanConfig = configGroup.leanConfig;
        final StackAdditiveConfig additiveConfig = configGroup.additiveConfig;

        final StackPlanConfig bidPlanConfig = configGroup.bidPlanConfig;
        final StackStrategyConfig bidStrategyConfig = configGroup.bidStrategyConfig;

        final StackPlanConfig askPlanConfig = configGroup.askPlanConfig;
        final StackStrategyConfig askStrategyConfig = configGroup.askStrategyConfig;

        final String leanToQuoteRatio;
        if (Double.isNaN(leanConfig.getLeanToQuoteRatio())) {
            leanToQuoteRatio = "0";
        } else {
            leanToQuoteRatio = Double.toString(leanConfig.getLeanToQuoteRatio());
        }

        viewer.setRow(nibblerName, configGroup.configGroupID, configGroup.symbol, configGroup.configType, quoteConfig.getMaxBookAgeMillis(),
                quoteConfig.isAuctionQuotingEnabled(), quoteConfig.isOnlyAuctionQuoting(), quoteConfig.getAuctionTheoMaxTicksThrough(),
                quoteConfig.getMaxJumpBPS(), quoteConfig.getBettermentQty(), quoteConfig.getBettermentTicks(),
                fxConfig.getMaxBookAgeMillis(), fxConfig.getMaxJumpBPS(), leanConfig.getMaxBookAgeMillis(), leanConfig.getMaxJumpBPS(),
                leanConfig.getRequiredQty(), leanConfig.getMaxPapaWeight(), leanToQuoteRatio, additiveConfig.isEnabled(),
                additiveConfig.getMaxSignalAgeMillis(), additiveConfig.getMinRequiredBPS(), additiveConfig.getMaxBPS(),
                bidPlanConfig.getMinLevelQty(), bidPlanConfig.getMaxLevelQty(), bidPlanConfig.getLotSize(), bidPlanConfig.getMaxLevels(),
                bidStrategyConfig.getMaxOrdersPerLevel(), bidStrategyConfig.isQuoteBettermentOn(),
                bidStrategyConfig.getQuoteFlickerBufferPercent(), bidStrategyConfig.getQuotePicardMaxTicksThrough(),
                bidStrategyConfig.getPicardMaxPerSec(), bidStrategyConfig.getPicardMaxPerMin(), bidStrategyConfig.getPicardMaxPerHour(),
                bidStrategyConfig.getPicardMaxPerDay(), askPlanConfig.getMinLevelQty(), askPlanConfig.getMaxLevelQty(),
                askPlanConfig.getLotSize(), askPlanConfig.getMaxLevels(), askStrategyConfig.getMaxOrdersPerLevel(),
                askStrategyConfig.isQuoteBettermentOn(), askStrategyConfig.getQuoteFlickerBufferPercent(),
                askStrategyConfig.getQuotePicardMaxTicksThrough(), askStrategyConfig.getPicardMaxPerSec(),
                askStrategyConfig.getPicardMaxPerMin(), askStrategyConfig.getPicardMaxPerHour(), askStrategyConfig.getPicardMaxPerDay());
    }

    public void serverConnectionLost(final String nibblerName) {
        nibblerConfigs.get(nibblerName).clear();
        views.all().removeAll(nibblerName);
    }

    @FromWebSocketView
    public void submitChange(final String nibblerName, final String configGroupIDStr, final int quoteMaxBookAgeMillis,
            final boolean quoteIsAuctionQuotingEnabled, final boolean quoteIsOnlyAuction, final int quoteAuctionTheoMaxTicksThrough,
            final int quoteMaxJumpBPS, final int quoteBettermentQty, final int quoteBettermentTicks, final int fxMaxBookAgeMillis,
            final int fxMaxJumpBPS, final int leanMaxBookAgeMillis, final int leanMaxJumpBPS, final int leanRequiredQty,
            final int leanMaxPapaWeight, final String leanToQuoteRatioStr, final boolean additiveIsEnabled,
            final int additiveMaxSignalAgeMillis, final int additiveMinRequiredBPS, final int additiveMaxBPS, final int bidPlanMinLevelQty,
            final int bidPlanMaxLevelQty, final int bidPlanLotSize, final int bidPlanMaxLevels, final int bidMaxOrdersPerLevel,
            final boolean bidIsQuoteBettermentOn, final int bidQuoteFlickerBufferPercent, final int bidPicardMaxTicksThrough,
            final int bidPicardMaxPerSec, final int bidPicardMaxPerMin, final int bidPicardMaxPerHour, final int bidPicardMaxPerDay,
            final int askPlanMinLevelQty, final int askPlanMaxLevelQty, final int askPlanLotSize, final int askPlanMaxLevels,
            final int askMaxOrdersPerLevel, final boolean askIsQuoteBettermentOn, final int askQuoteFlickerBufferPercent,
            final int askPicardMaxTicksThrough, final int askPicardMaxPerSec, final int askPicardMaxPerMin, final int askPicardMaxPerHour,
            final int askPicardMaxPerDay) {

        final double leanToQuoteRatio = Double.parseDouble(leanToQuoteRatioStr);

        final StackClientHandler configClient = configClients.get(nibblerName);
        if (null != configClient) {
            final long configGroupID = Long.parseLong(configGroupIDStr);

            configClient.quoteConfigUpdated(SOURCE, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
                    quoteAuctionTheoMaxTicksThrough, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks);
            configClient.fxConfigUpdated(SOURCE, configGroupID, fxMaxBookAgeMillis, fxMaxJumpBPS);
            configClient.leanConfigUpdated(SOURCE, configGroupID, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty,
                    (byte) leanMaxPapaWeight, leanToQuoteRatio);

            configClient.additiveConfigUpdated(SOURCE, configGroupID, additiveMaxSignalAgeMillis, additiveIsEnabled, additiveMinRequiredBPS,
                    additiveMaxBPS);

            configClient.planConfigUpdated(SOURCE, configGroupID, BookSide.BID, bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize,
                    bidPlanMaxLevels);
            configClient.strategyConfigUpdated(SOURCE, configGroupID, BookSide.BID, bidMaxOrdersPerLevel, bidIsQuoteBettermentOn,
                    (byte) bidQuoteFlickerBufferPercent, bidPicardMaxTicksThrough, bidPicardMaxPerSec, bidPicardMaxPerMin,
                    bidPicardMaxPerHour, bidPicardMaxPerDay);

            configClient.planConfigUpdated(SOURCE, configGroupID, BookSide.ASK, askPlanMinLevelQty, askPlanMaxLevelQty, askPlanLotSize,
                    askPlanMaxLevels);
            configClient.strategyConfigUpdated(SOURCE, configGroupID, BookSide.ASK, askMaxOrdersPerLevel, askIsQuoteBettermentOn,
                    (byte) askQuoteFlickerBufferPercent, askPicardMaxTicksThrough, askPicardMaxPerSec, askPicardMaxPerMin,
                    askPicardMaxPerHour, askPicardMaxPerDay);

            configClient.batchComplete();
        }
    }
}