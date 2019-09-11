package com.drwtrading.london.reddal.stacks.configui;

import com.drwtrading.london.eeif.stack.transport.data.config.StackAdditiveConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.config.StackFXConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackLeanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackPlanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackQuoteConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackStrategyConfig;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
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

    private final UILogger uiLogger;

    private final WebSocketViews<IStackConfigUI> views;

    private final Map<String, LongMap<StackConfigGroup>> nibblerConfigs;
    private final Map<String, StackClientHandler> configClients;

    public StackConfigPresenter(final UILogger uiLogger) {

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

        uiLogger.write("StackConfig", msg);

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
            leanToQuoteRatio = "1";
        } else {
            leanToQuoteRatio = Double.toString(leanConfig.getLeanToQuoteRatio());
        }

        final double leanPriceAdjustmentRaw = leanConfig.getPriceAdjustment() / (double) Constants.NORMALISING_FACTOR;

        viewer.setRow(nibblerName, configGroup.configGroupID, configGroup.symbol, configGroup.configType, quoteConfig.getMaxBookAgeMillis(),
                quoteConfig.isAuctionQuotingEnabled(), quoteConfig.isOnlyAuctionQuoting(), quoteConfig.getAuctionTheoMaxBPSThrough(),
                quoteConfig.isAllowEmptyBook(), quoteConfig.getMaxJumpBPS(), quoteConfig.getBettermentQty(),
                quoteConfig.isBettermentOppositeSide(), quoteConfig.getBettermentTicks(), fxConfig.getMaxBookAgeMillis(),
                fxConfig.getMaxJumpBPS(), leanConfig.getMaxBookAgeMillis(), leanConfig.getMaxJumpBPS(), leanConfig.getRequiredQty(),
                leanConfig.getMaxPapaWeight(), leanToQuoteRatio, leanPriceAdjustmentRaw, additiveConfig.isEnabled(),
                additiveConfig.getMaxSignalAgeMillis(), additiveConfig.getMinRequiredBPS(), additiveConfig.getMaxBPS(),
                bidPlanConfig.getMinLevelQty(), bidPlanConfig.getMaxLevelQty(), bidPlanConfig.getLotSize(), bidPlanConfig.getMaxLevels(),
                bidPlanConfig.getMinPicardQty(), bidStrategyConfig.getMaxOrdersPerLevel(), bidStrategyConfig.isOnlySubmitBestLevel(),
                bidStrategyConfig.isQuoteBettermentOn(), bidStrategyConfig.getModTicks(), bidStrategyConfig.getQuoteFlickerBufferPercent(),
                bidStrategyConfig.getQuotePicardMaxBPSThrough(), bidStrategyConfig.getPicardMaxPapaWeight(),
                bidStrategyConfig.getPicardMaxPerSec(), bidStrategyConfig.getPicardMaxPerMin(), bidStrategyConfig.getPicardMaxPerHour(),
                bidStrategyConfig.getPicardMaxPerDay(), askPlanConfig.getMinLevelQty(), askPlanConfig.getMaxLevelQty(),
                askPlanConfig.getLotSize(), askPlanConfig.getMaxLevels(), askPlanConfig.getMinPicardQty(),
                askStrategyConfig.getMaxOrdersPerLevel(), askStrategyConfig.isOnlySubmitBestLevel(),
                askStrategyConfig.isQuoteBettermentOn(), askStrategyConfig.getModTicks(), askStrategyConfig.getQuoteFlickerBufferPercent(),
                askStrategyConfig.getQuotePicardMaxBPSThrough(), askStrategyConfig.getPicardMaxPapaWeight(),
                askStrategyConfig.getPicardMaxPerSec(), askStrategyConfig.getPicardMaxPerMin(), askStrategyConfig.getPicardMaxPerHour(),
                askStrategyConfig.getPicardMaxPerDay());
    }

    public void serverConnectionLost(final String nibblerName) {
        nibblerConfigs.get(nibblerName).clear();
        views.all().removeAll(nibblerName);
    }

    @FromWebSocketView
    public void submitChange(final String nibblerName, final String configGroupIDStr, final int quoteMaxBookAgeMillis,
            final boolean quoteIsAuctionQuotingEnabled, final boolean quoteIsOnlyAuction, final int quoteAuctionTheoMaxBPSThrough,
            final boolean isAllowEmptyBook, final int quoteMaxJumpBPS, final int quoteBettermentQty,
            final boolean quoteIsBettermentOppositeSide, final int quoteBettermentTicks, final int fxMaxBookAgeMillis,
            final int fxMaxJumpBPS, final int leanMaxBookAgeMillis, final int leanMaxJumpBPS, final int leanRequiredQty,
            final int leanMaxPapaWeight, final String leanToQuoteRatioStr, final String leanPriceAdjustmentRawStr,
            final boolean additiveIsEnabled, final int additiveMaxSignalAgeMillis, final int additiveMinRequiredBPS,
            final int additiveMaxBPS, final int bidPlanMinLevelQty, final int bidPlanMaxLevelQty, final int bidPlanLotSize,
            final int bidPlanMaxLevels, final int bidMinPicardQty, final int bidMaxOrdersPerLevel, final boolean bidIsOnlySubmitBestLevel,
            final boolean bidIsQuoteBettermentOn, final int bidModTicks, final int bidQuoteFlickerBufferPercent,
            final int bidPicardMaxBPSThrough, final int bidPicardMaxPapaWeight, final int bidPicardMaxPerSec, final int bidPicardMaxPerMin,
            final int bidPicardMaxPerHour, final int bidPicardMaxPerDay, final int askPlanMinLevelQty, final int askPlanMaxLevelQty,
            final int askPlanLotSize, final int askPlanMaxLevels, final int askMinPicardQty, final int askMaxOrdersPerLevel,
            final boolean askIsOnlySubmitBestLevel, final boolean askIsQuoteBettermentOn, final int askModTicks,
            final int askQuoteFlickerBufferPercent, final int askPicardMaxBPSThrough, final int askPicardMaxPapaWeight,
            final int askPicardMaxPerSec, final int askPicardMaxPerMin, final int askPicardMaxPerHour, final int askPicardMaxPerDay) {

        final double leanToQuoteRatio = Double.parseDouble(leanToQuoteRatioStr);
        final long leanPriceAdjustment = (long) (Double.parseDouble(leanPriceAdjustmentRawStr) * Constants.NORMALISING_FACTOR);

        final StackClientHandler configClient = configClients.get(nibblerName);
        if (null != configClient) {
            final long configGroupID = Long.parseLong(configGroupIDStr);

            configClient.quoteConfigUpdated(SOURCE, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
                    quoteAuctionTheoMaxBPSThrough, isAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteIsBettermentOppositeSide,
                    quoteBettermentTicks);
            configClient.fxConfigUpdated(SOURCE, configGroupID, fxMaxBookAgeMillis, fxMaxJumpBPS);
            configClient.leanConfigUpdated(SOURCE, configGroupID, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty,
                    (byte) leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustment);

            configClient.additiveConfigUpdated(SOURCE, configGroupID, additiveMaxSignalAgeMillis, additiveIsEnabled, additiveMinRequiredBPS,
                    additiveMaxBPS);

            configClient.planConfigUpdated(SOURCE, configGroupID, BookSide.BID, bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize,
                    bidPlanMaxLevels, bidMinPicardQty);
            configClient.strategyConfigUpdated(SOURCE, configGroupID, BookSide.BID, bidMaxOrdersPerLevel, bidIsOnlySubmitBestLevel,
                    bidIsQuoteBettermentOn, bidModTicks, (byte) bidQuoteFlickerBufferPercent, bidPicardMaxBPSThrough,
                    (byte) bidPicardMaxPapaWeight, bidPicardMaxPerSec, bidPicardMaxPerMin, bidPicardMaxPerHour, bidPicardMaxPerDay);

            configClient.planConfigUpdated(SOURCE, configGroupID, BookSide.ASK, askPlanMinLevelQty, askPlanMaxLevelQty, askPlanLotSize,
                    askPlanMaxLevels, askMinPicardQty);
            configClient.strategyConfigUpdated(SOURCE, configGroupID, BookSide.ASK, askMaxOrdersPerLevel, askIsOnlySubmitBestLevel,
                    askIsQuoteBettermentOn, askModTicks, (byte) askQuoteFlickerBufferPercent, askPicardMaxBPSThrough,
                    (byte) askPicardMaxPapaWeight, askPicardMaxPerSec, askPicardMaxPerMin, askPicardMaxPerHour, askPicardMaxPerDay);

            configClient.batchComplete();
        }
    }
}
