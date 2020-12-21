package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds.IAutoPullerCmd;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.updates.IAutoPullerUpdate;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.ladders.HeartbeatRoundtrip;
import com.drwtrading.london.reddal.ladders.ISingleOrderCommand;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.ladders.RecenterLadder;
import com.drwtrading.london.reddal.ladders.RecenterLaddersForUser;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsPrefLoaded;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsStoreLadderPref;
import com.drwtrading.london.reddal.opxl.ISINsGoingEx;
import com.drwtrading.london.reddal.opxl.OPXLDeskPositions;
import com.drwtrading.london.reddal.opxl.OpxlLadderText;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import com.drwtrading.london.reddal.orderManagement.NibblerTransportConnected;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntrySymbolChannel;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPrices;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPricesRequest;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCSupportedSymbol;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.picard.LiquidityFinderData;
import com.drwtrading.london.reddal.picard.PicardRow;
import com.drwtrading.london.reddal.picard.PicardRowWithInstID;
import com.drwtrading.london.reddal.pks.PKSExposures;
import com.drwtrading.london.reddal.premium.Premium;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.stockAlerts.RfqAlert;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.ChixSymbolPair;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.symbols.SymbolIndyData;
import com.drwtrading.london.reddal.symbols.SymbolReferencePrice;
import com.drwtrading.london.reddal.util.BogusErrorFilteringPublisher;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuoteObligationsEnableCmd;
import com.drwtrading.london.reddal.workspace.HostWorkspaceRequest;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.monitoring.stats.status.StatusStat;
import com.drwtrading.photons.eeif.configuration.EeifConfiguration;
import com.drwtrading.photons.ladder.LadderMetadata;
import drw.eeif.photons.mrchill.Position;
import drw.london.json.Jsonable;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.Set;

class ReddalChannels {

    final TypedChannel<Throwable> error;
    final Publisher<Throwable> errorPublisher;
    final TypedChannel<LaserLineValue> opxlLaserLineData;
    final TypedChannel<OpxlLadderText> ladderText;
    final TypedChannel<LadderMetadata> metaData;
    final TypedChannel<OPXLDeskPositions> deskPositions;
    final TypedChannel<Position> position;
    final TypedChannel<PKSExposures> pksExposures;
    final TypedChannel<NibblerTransportConnected> nibblerTransportConnected;
    final Channel<GTCSupportedSymbol> supportedGTCSymbols;
    final TypedChannel<StatusStat> stats;
    final TypedChannel<IOrderCmd> cmdsForNibblers;
    final TypedChannel<LadderSettingsPrefLoaded> ladderPrefsLoaded;
    final TypedChannel<LadderSettingsStoreLadderPref> storeLadderPref;
    final TypedChannel<InstrumentDef> instDefs;
    final TypedChannel<ETFDef> etfDefs;
    final TypedChannel<SymbolIndyData> symbolDescs;
    final TypedChannel<DisplaySymbol> displaySymbol;
    final TypedChannel<SearchResult> searchResults;
    final TypedChannel<SymbolReferencePrice> symbolRefPrices;
    final TypedChannel<StockAlert> stockAlerts;
    final TypedChannel<RfqAlert> rfqStockAlerts;
    final TypedChannel<HeartbeatRoundtrip> heartbeatRoundTrips;

    final TypedChannel<RecenterLaddersForUser> recenterLaddersForUser;
    final TypedChannel<RecenterLadder> recenterLadder;
    final TypedChannel<SpreadContractSet> contractSets;
    final TypedChannel<HostWorkspaceRequest> userWorkspaceRequests;
    final TypedChannel<ChixSymbolPair> chixSymbolPairs;
    final TypedChannel<ISingleOrderCommand> singleOrderCommand;
    final TypedChannel<Jsonable> trace;
    final TypedChannel<ReplaceCommand> replaceCommand;
    final TypedChannel<LadderClickTradingIssue> ladderClickTradingIssues;
    final TypedChannel<UserCycleRequest> userCycleContractPublisher;
    final TypedChannel<OrderEntryFromServer> orderEntryFromServer;
    final TypedChannel<OrderEntryCommandToServer> orderEntryCommandToServer;
    final TypedChannel<OrderEntrySymbolChannel> orderEntrySymbols;
    final TypedChannel<ISINsGoingEx> isinsGoingEx;

    final TypedChannel<SymbolSelection> symbolSelections;
    final TypedChannel<UltimateParentMapping> ultimateParents;

    final TypedChannel<String> stackParentSymbolPublisher;
    final TypedChannel<StackIncreaseParentOffsetCmd> increaseParentOffsetCmds;
    final TypedChannel<StackIncreaseChildOffsetCmd> increaseChildOffsetBPSCmds;
    final TypedChannel<StacksSetSiblingsEnableCmd> setSiblingsEnabledCmds;

    final EnumMap<StackCommunity, TypedChannel<InstrumentID>> communityInstrumentIDs;
    final EnumMap<StackCommunity, TypedChannel<String>> communitySymbols;
    final TypedChannel<PicardRowWithInstID> picardRows;
    final SelectIOChannel<Set<String>> picardDMFilterSymbols;
    final TypedChannel<LiquidityFinderData> laserDistances;
    final TypedChannel<PicardRow> yodaPicardRows;
    final TypedChannel<Premium> spreadnoughtPremiums;

    final TypedChannel<IAutoPullerCmd> autoPullerCmds;
    final TypedChannel<IAutoPullerUpdate> autoPullerUpdates;

    final TypedChannel<GTCBettermentPricesRequest> gtcBettermentRequests;
    final TypedChannel<GTCBettermentPrices> gtcBettermentResponses;

    final TypedChannel<EeifConfiguration> eeifConfiguration;

    final TypedChannel<QuoteObligationsEnableCmd> quotingObligationsCmds;

    ReddalChannels() {

        this.error = TypedChannels.create(Throwable.class);
        this.errorPublisher = new BogusErrorFilteringPublisher(error);
        this.opxlLaserLineData = create(LaserLineValue.class);
        this.ladderText = create(OpxlLadderText.class);
        this.metaData = create(LadderMetadata.class);
        this.deskPositions = create(OPXLDeskPositions.class);
        this.position = create(Position.class);
        this.pksExposures = TypedChannels.create(PKSExposures.class);
        this.nibblerTransportConnected = create(NibblerTransportConnected.class);
        this.supportedGTCSymbols = create(GTCSupportedSymbol.class);
        this.stats = create(StatusStat.class);
        this.cmdsForNibblers = create(IOrderCmd.class);
        this.ladderPrefsLoaded = create(LadderSettingsPrefLoaded.class);
        this.storeLadderPref = create(LadderSettingsStoreLadderPref.class);
        this.instDefs = create(InstrumentDef.class);
        this.etfDefs = create(ETFDef.class);
        this.symbolDescs = create(SymbolIndyData.class);
        this.displaySymbol = create(DisplaySymbol.class);
        this.searchResults = create(SearchResult.class);
        this.symbolRefPrices = create(SymbolReferencePrice.class);
        this.stockAlerts = create(StockAlert.class);
        this.heartbeatRoundTrips = create(HeartbeatRoundtrip.class);
        this.recenterLaddersForUser = create(RecenterLaddersForUser.class);
        this.recenterLadder = create(RecenterLadder.class);
        this.contractSets = create(SpreadContractSet.class);
        this.userWorkspaceRequests = create(HostWorkspaceRequest.class);
        this.chixSymbolPairs = create(ChixSymbolPair.class);
        this.singleOrderCommand = create(ISingleOrderCommand.class);
        this.trace = create(Jsonable.class);
        this.ladderClickTradingIssues = create(LadderClickTradingIssue.class);
        this.userCycleContractPublisher = create(UserCycleRequest.class);
        this.replaceCommand = create(ReplaceCommand.class);
        this.orderEntryFromServer = create(OrderEntryFromServer.class);
        this.orderEntrySymbols = create(OrderEntrySymbolChannel.class);
        this.orderEntryCommandToServer = create(OrderEntryCommandToServer.class);
        this.isinsGoingEx = create(ISINsGoingEx.class);

        this.symbolSelections = create(SymbolSelection.class);
        this.ultimateParents = create(UltimateParentMapping.class);

        this.stackParentSymbolPublisher = create(String.class);
        this.increaseParentOffsetCmds = create(StackIncreaseParentOffsetCmd.class);
        this.increaseChildOffsetBPSCmds = create(StackIncreaseChildOffsetCmd.class);
        this.setSiblingsEnabledCmds = create(StacksSetSiblingsEnableCmd.class);

        this.communityInstrumentIDs = new EnumMap<>(StackCommunity.class);
        this.communitySymbols = new EnumMap<>(StackCommunity.class);
        for (final StackCommunity stackCommunity : StackCommunity.values()) {
            this.communityInstrumentIDs.put(stackCommunity, create(InstrumentID.class));
            this.communitySymbols.put(stackCommunity, create(String.class));
        }

        this.picardRows = create(PicardRowWithInstID.class);
        this.picardDMFilterSymbols = new SelectIOChannel<>();

        this.laserDistances = create(LiquidityFinderData.class);
        this.yodaPicardRows = create(PicardRow.class);
        this.rfqStockAlerts = create(RfqAlert.class);
        this.spreadnoughtPremiums = create(Premium.class);

        this.autoPullerCmds = create(IAutoPullerCmd.class);
        this.autoPullerUpdates = create(IAutoPullerUpdate.class);

        this.gtcBettermentRequests = create(GTCBettermentPricesRequest.class);
        this.gtcBettermentResponses = create(GTCBettermentPrices.class);

        this.eeifConfiguration = create(EeifConfiguration.class);

        this.quotingObligationsCmds = create(QuoteObligationsEnableCmd.class);
    }

    private static <T> TypedChannel<T> create(final Class<T> clazz) {
        return TypedChannels.create(clazz);
    }
}
