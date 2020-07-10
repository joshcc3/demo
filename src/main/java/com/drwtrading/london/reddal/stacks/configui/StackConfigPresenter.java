package com.drwtrading.london.reddal.stacks.configui;

import com.drwtrading.london.eeif.stack.transport.data.config.StackAdditiveConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.config.StackFXConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackLeanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackPlanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackQuoteConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackStrategyConfig;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StackConfigPresenter {

    private static final String SOURCE = "CONFIG_UI";

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

            final WebSocketInboundData msg = (WebSocketInboundData) webMsg;

            uiLogger.write("StackConfig", msg);
            views.invoke(msg);
        }
    }

    @FromWebSocketView
    public void subscribe(final String symbolsString, final WebSocketInboundData data) {

        final IStackConfigUI newView = views.get(data.getOutboundChannel());

        final Set<String> symbols = new HashSet<>(Arrays.asList(symbolsString.split(";")));

        for (final Map.Entry<String, LongMap<StackConfigGroup>> stackConfigs : nibblerConfigs.entrySet()) {

            final String nibblerName = stackConfigs.getKey();
            for (final LongMapNode<StackConfigGroup> configNode : stackConfigs.getValue()) {

                final StackConfigGroup config = configNode.getValue();
                if (symbols.contains(config.getSymbol())) {
                    sendLine(newView, nibblerName, config);
                }
            }
        }
    }

    private static void sendLine(final IStackConfigUI viewer, final String nibblerName, final StackConfigGroup configGroup) {

        final StackQuoteConfig quoteConfig = configGroup.quoteConfig;
        final StackFXConfig fxConfig = configGroup.fxConfig;
        final StackLeanConfig leanConfig = configGroup.leanConfig;
        final StackAdditiveConfig additiveConfig = configGroup.additiveConfig;

        final StackPlanConfig planConfig = configGroup.planConfig;
        final StackStrategyConfig strategyConfig = configGroup.strategyConfig;

        final String leanToQuoteRatio;
        if (Double.isNaN(leanConfig.getLeanToQuoteRatio())) {
            leanToQuoteRatio = "1";
        } else {
            leanToQuoteRatio = Double.toString(leanConfig.getLeanToQuoteRatio());
        }

        final double leanPriceAdjustmentRaw = leanConfig.getPriceAdjustment() / (double) Constants.NORMALISING_FACTOR;

        viewer.setRow(nibblerName, configGroup.configGroupID, configGroup.symbol, quoteConfig.getMaxBookAgeMillis(),
                quoteConfig.isAuctionQuotingEnabled(), quoteConfig.isOnlyAuctionQuoting(), quoteConfig.getAuctionTheoMaxBPSThrough(),
                quoteConfig.isAllowEmptyBook(), quoteConfig.getMaxJumpBPS(), quoteConfig.getBettermentQty(),
                quoteConfig.getBettermentTicks(), quoteConfig.isBettermentOppositeSide(), quoteConfig.getOppositeSideBettermentTicks(),
                fxConfig.getMaxBookAgeMillis(), fxConfig.getMaxJumpBPS(), leanConfig.getMaxBookAgeMillis(), leanConfig.getMaxJumpBPS(),
                leanConfig.getRequiredQty(), leanConfig.getMaxPapaWeight(), leanToQuoteRatio, leanPriceAdjustmentRaw,
                additiveConfig.isEnabled(), additiveConfig.getMaxSignalAgeMillis(), additiveConfig.getMinRequiredBPS(),
                additiveConfig.getMaxBPS(), planConfig.getMinLevelQty(), planConfig.getMaxLevelQty(), planConfig.getLotSize(),
                planConfig.getMaxLevels(), planConfig.getMinPicardQty(), strategyConfig.getMaxOrdersPerLevel(),
                strategyConfig.isOnlySubmitBestLevel(), strategyConfig.isQuoteBettermentOn(), strategyConfig.getModTicks(),
                strategyConfig.getQuoteFlickerBufferPercent(), strategyConfig.getQuotePicardMaxBPSThrough(),
                strategyConfig.getPicardMaxPapaWeight(), strategyConfig.getPicardMaxPerSec(), strategyConfig.getPicardMaxPerMin(),
                strategyConfig.getPicardMaxPerHour(), strategyConfig.getPicardMaxPerDay());
    }

    public void serverConnectionLost(final String nibblerName) {

        nibblerConfigs.get(nibblerName).clear();
        views.all().removeAll(nibblerName);
    }

    @FromWebSocketView
    public void submitChange(final String nibblerName, final String configGroupIDStr, final int quoteMaxBookAgeMillis,
            final boolean quoteIsAuctionQuotingEnabled, final boolean quoteIsOnlyAuction, final int quoteAuctionTheoMaxBPSThrough,
            final boolean isAllowEmptyBook, final int quoteMaxJumpBPS, final int quoteBettermentQty, final int quoteBettermentTicks,
            final boolean quoteIsBettermentOppositeSide, final int quoteOppositeSideBettermentTicks, final int fxMaxBookAgeMillis,
            final int fxMaxJumpBPS, final int leanMaxBookAgeMillis, final int leanMaxJumpBPS, final int leanRequiredQty,
            final int leanMaxPapaWeight, final String leanToQuoteRatioStr, final String leanPriceAdjustmentRawStr,
            final boolean additiveIsEnabled, final int additiveMaxSignalAgeMillis, final int additiveMinRequiredBPS,
            final int additiveMaxBPS, final int planMinLevelQty, final int planMaxLevelQty, final int planLotSize, final int planMaxLevels,
            final int minPicardQty, final int maxOrdersPerLevel, final boolean isOnlySubmitBestLevel, final boolean isQuoteBettermentOn,
            final int modTicks, final int quoteFlickerBufferPercent, final int picardMaxBPSThrough, final int picardMaxPapaWeight,
            final int picardMaxPerSec, final int picardMaxPerMin, final int picardMaxPerHour, final int picardMaxPerDay) {

//        [nibblerName, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
//		quoteAuctionTheoMaxBPSThrough, quoteIsAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks,
//		quoteIsBettermentOppositeSide, quoteOppositeSideBettermentTicks, fxMaxBookAgeMillis, fxMaxJumpBPS, leanMaxBookAgeMillis,
//		leanMaxJumpBPS, leanRequiredQty, leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustment, additiveIsEnabled,
//		additiveMaxSignalAgeMillis, additiveMinRequiredBPS, additiveMaxBPS, planMinLevelQty, planMaxLevelQty, planLotSize, planMaxLevels,
//		minPicardQty, maxOrdersPerLevel, isOnlySubmitBestLevel, isQuoteBettermentOn, modTicks, quoteFlickerBuffer, quotePicardMaxBPSThrough,
//		picardMaxPapaWeight, picardMaxPerSec, picardMaxPerMin, picardMaxPerHour, picardMaxPerDay]
//
        final double leanToQuoteRatio = Double.parseDouble(leanToQuoteRatioStr);
        final long leanPriceAdjustment = (long) (Double.parseDouble(leanPriceAdjustmentRawStr) * Constants.NORMALISING_FACTOR);

        final StackClientHandler configClient = configClients.get(nibblerName);
        if (null != configClient) {
            final long configGroupID = Long.parseLong(configGroupIDStr);

            final int negativeQuoteOppositeSideTicks = -Math.abs(quoteOppositeSideBettermentTicks);

            configClient.quoteConfigUpdated(SOURCE, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
                    quoteAuctionTheoMaxBPSThrough, isAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks,
                    quoteIsBettermentOppositeSide, negativeQuoteOppositeSideTicks);
            configClient.fxConfigUpdated(SOURCE, configGroupID, fxMaxBookAgeMillis, fxMaxJumpBPS);
            configClient.leanConfigUpdated(SOURCE, configGroupID, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty,
                    (byte) leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustment);

            configClient.additiveConfigUpdated(SOURCE, configGroupID, additiveMaxSignalAgeMillis, additiveIsEnabled, additiveMinRequiredBPS,
                    additiveMaxBPS);

            configClient.planConfigUpdated(SOURCE, configGroupID, planMinLevelQty, planMaxLevelQty, planLotSize, planMaxLevels,
                    minPicardQty);
            configClient.strategyConfigUpdated(SOURCE, configGroupID, maxOrdersPerLevel, isOnlySubmitBestLevel, isQuoteBettermentOn,
                    modTicks, (byte) quoteFlickerBufferPercent, picardMaxBPSThrough, (byte) picardMaxPapaWeight, picardMaxPerSec,
                    picardMaxPerMin, picardMaxPerHour, picardMaxPerDay);

            configClient.batchComplete();
        }
    }
}
