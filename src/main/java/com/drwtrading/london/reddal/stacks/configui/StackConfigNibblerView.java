package com.drwtrading.london.reddal.stacks.configui;

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
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Collection;

public class StackConfigNibblerView {

    private static final String SOURCE = "CONFIG_UI";

    private static final Collection<String> STACK_CONFIG_TYPES;

    static {
        final StackConfigType[] configTypes = StackConfigType.values();
        STACK_CONFIG_TYPES = new ArrayList<>(configTypes.length);
        for (final StackConfigType configType : configTypes) {
            STACK_CONFIG_TYPES.add(configType.name());
        }
    }

    private final String nibblerName;
    private final WebSocketViews<IStackConfigUI> views;

    private final LongMap<StackConfigGroup> stackConfigs;

    private StackClientHandler configClient;

    public StackConfigNibblerView(final String nibblerName) {

        this.nibblerName = nibblerName;

        this.views = WebSocketViews.create(IStackConfigUI.class, this);

        this.stackConfigs = new LongMap<>();
    }

    public void setConfigClient(final StackClientHandler cache) {
        this.configClient = cache;
    }

    public void configGroupCreated(final StackConfigGroup configGroup) {
        configUpdated(configGroup);
    }

    public void configUpdated(final StackConfigGroup configGroup) {

        stackConfigs.put(configGroup.configGroupID, configGroup);
        sendLine(views.all(), configGroup);
    }

    public void addUI(final Publisher<WebSocketOutboundData> channel) {

        final IStackConfigUI newView = views.get(channel);
        newView.setConfigTypes(STACK_CONFIG_TYPES);

        for (final LongMapNode<StackConfigGroup> configNode : stackConfigs) {

            final StackConfigGroup config = configNode.getValue();
            sendLine(newView, config);
        }
    }

    private static void sendLine(final IStackConfigUI viewer, final StackConfigGroup configGroup) {

        final StackQuoteConfig quoteConfig = configGroup.quoteConfig;
        final StackFXConfig fxConfig = configGroup.fxConfig;
        final StackLeanConfig leanConfig = configGroup.leanConfig;

        final StackPlanConfig bidPlanConfig = configGroup.bidPlanConfig;
        final StackStrategyConfig bidStrategyConfig = configGroup.bidStrategyConfig;

        final StackPlanConfig askPlanConfig = configGroup.askPlanConfig;
        final StackStrategyConfig askStrategyConfig = configGroup.askStrategyConfig;

        final String leanToQuoteRatio = Double.toString(leanConfig.getLeanToQuoteRatio());

        viewer.setRow(configGroup.configGroupID, configGroup.symbol, configGroup.configType, quoteConfig.getMaxBookAgeMillis(),
                quoteConfig.isAuctionQuotingEnabled(), quoteConfig.isOnlyAuctionQuoting(), quoteConfig.getAuctionTheoMaxTicksThrough(),
                quoteConfig.getMaxJumpBPS(), quoteConfig.getBettermentQty(), quoteConfig.getBettermentTicks(),
                fxConfig.getMaxBookAgeMillis(), fxConfig.getMaxJumpBPS(), leanConfig.getMaxBookAgeMillis(), leanConfig.getMaxJumpBPS(),
                leanConfig.getRequiredQty(), leanConfig.getMaxPapaWeight(), leanToQuoteRatio, bidPlanConfig.getMinLevelQty(),
                bidPlanConfig.getMaxLevelQty(), bidPlanConfig.getLotSize(), bidPlanConfig.getMaxLevels(),
                bidStrategyConfig.getMaxOrdersPerLevel(), bidStrategyConfig.isQuoteBettermentOn(),
                bidStrategyConfig.getQuoteFlickerBufferPercent(), bidStrategyConfig.getQuotePicardMaxTicksThrough(),
                bidStrategyConfig.getPicardMaxPerSec(), bidStrategyConfig.getPicardMaxPerMin(), bidStrategyConfig.getPicardMaxPerHour(),
                bidStrategyConfig.getPicardMaxPerDay(), askPlanConfig.getMinLevelQty(), askPlanConfig.getMaxLevelQty(),
                askPlanConfig.getLotSize(), askPlanConfig.getMaxLevels(), askStrategyConfig.getMaxOrdersPerLevel(),
                askStrategyConfig.isQuoteBettermentOn(), askStrategyConfig.getQuoteFlickerBufferPercent(),
                askStrategyConfig.getQuotePicardMaxTicksThrough(), askStrategyConfig.getPicardMaxPerSec(),
                askStrategyConfig.getPicardMaxPerMin(), askStrategyConfig.getPicardMaxPerHour(), askStrategyConfig.getPicardMaxPerDay());
    }

    public void serverConnectionLost() {
        stackConfigs.clear();
        views.all().removeAll();
    }

    public void removeUI(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    void onMessage(final WebSocketInboundData msg) {
        views.invoke(msg);
    }

    @FromWebSocketView
    public void submitChange(final String configGroupIDStr, final int quoteMaxBookAgeMillis, final boolean quoteIsAuctionQuotingEnabled,
            final boolean quoteIsOnlyAuction, final int quoteAuctionTheoMaxTicksThrough, final int quoteMaxJumpBPS,
            final int quoteBettermentQty, final int quoteBettermentTicks, final int fxMaxBookAgeMillis, final int fxMaxJumpBPS,
            final int leanMaxBookAgeMillis, final int leanMaxJumpBPS, final int leanRequiredQty, final int leanMaxPapaWeight,
            final String leanToQuoteRatioStr, final int bidPlanMinLevelQty, final int bidPlanMaxLevelQty, final int bidPlanLotSize,
            final int bidPlanMaxLevels, final int bidMaxOrdersPerLevel, final boolean bidIsQuoteBettermentOn,
            final int bidQuoteFlickerBufferPercent, final int bidPicardMaxTicksThrough, final int bidPicardMaxPerSec,
            final int bidPicardMaxPerMin, final int bidPicardMaxPerHour, final int bidPicardMaxPerDay, final int askPlanMinLevelQty,
            final int askPlanMaxLevelQty, final int askPlanLotSize, final int askPlanMaxLevels, final int askMaxOrdersPerLevel,
            final boolean askIsQuoteBettermentOn, final int askQuoteFlickerBufferPercent, final int askPicardMaxTicksThrough,
            final int askPicardMaxPerSec, final int askPicardMaxPerMin, final int askPicardMaxPerHour, final int askPicardMaxPerDay) {

        final double leanToQuoteRatio = Double.parseDouble(leanToQuoteRatioStr);

        if (null != configClient) {
            final long configGroupID = Long.parseLong(configGroupIDStr);

            configClient.quoteConfigUpdated(SOURCE, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
                    quoteAuctionTheoMaxTicksThrough, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks);
            configClient.fxConfigUpdated(SOURCE, configGroupID, fxMaxBookAgeMillis, fxMaxJumpBPS);
            configClient.leanConfigUpdated(SOURCE, configGroupID, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty,
                    (byte) leanMaxPapaWeight, leanToQuoteRatio);

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
