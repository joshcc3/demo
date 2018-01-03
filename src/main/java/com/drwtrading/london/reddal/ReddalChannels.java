package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.jetlang.ChannelFactory;
import com.drwtrading.london.reddal.ladders.HeartbeatRoundtrip;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.ladders.LadderSettings;
import com.drwtrading.london.reddal.ladders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.RecenterLadder;
import com.drwtrading.london.reddal.ladders.RecenterLaddersForUser;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportConnected;
import com.drwtrading.london.reddal.picard.PicardRow;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.safety.ServerTradingStatus;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.family.StackChildFilter;
import com.drwtrading.london.reddal.stacks.opxl.StackRefPriceDetail;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.ChixSymbolPair;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.BogusErrorFilteringPublisher;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderConnectionEstablished;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderEventFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.reddal.workspace.HostWorkspaceRequest;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.mrphil.Position;
import com.google.common.collect.MapMaker;
import drw.london.json.Jsonable;
import org.jetlang.channels.Publisher;

import java.util.Map;

class ReddalChannels {

    private final ChannelFactory channelFactory;

    final TypedChannel<Throwable> error;
    final Publisher<Throwable> errorPublisher;
    final TypedChannel<LadderMetadata> metaData;
    final TypedChannel<Position> position;
    final TypedChannel<PKSExposure> pksExposure;
    final TypedChannel<ServerTradingStatus> tradingStatus;
    final TypedChannel<WorkingOrderUpdateFromServer> workingOrders;
    final TypedChannel<WorkingOrderConnectionEstablished> workingOrderConnectionEstablished;
    final TypedChannel<WorkingOrderEventFromServer> workingOrderEvents;
    final TypedChannel<NibblerTransportConnected> nibblerTransportConnected;
    final TypedChannel<StatsMsg> stats;
    final Publisher<RemoteOrderCommandToServer> remoteOrderCommand;
    final Map<String, TypedChannel<IOrderCmd>> remoteOrderCommandByServer;
    final TypedChannel<LadderSettings.LadderPrefLoaded> ladderPrefsLoaded;
    final TypedChannel<LadderSettings.StoreLadderPref> storeLadderPref;
    final TypedChannel<InstrumentDef> instDefs;
    final TypedChannel<DisplaySymbol> displaySymbol;
    final TypedChannel<SearchResult> searchResults;
    final TypedChannel<StockAlert> stockAlerts;
    final TypedChannel<HeartbeatRoundtrip> heartbeatRoundTrips;

    final TypedChannel<RecenterLaddersForUser> recenterLaddersForUser;
    final TypedChannel<RecenterLadder> recenterLadder;
    final TypedChannel<SpreadContractSet> contractSets;
    final TypedChannel<HostWorkspaceRequest> userWorkspaceRequests;
    final TypedChannel<ChixSymbolPair> chixSymbolPairs;
    final TypedChannel<OrdersPresenter.SingleOrderCommand> singleOrderCommand;
    final TypedChannel<Jsonable> trace;
    final TypedChannel<ReplaceCommand> replaceCommand;
    final TypedChannel<LadderClickTradingIssue> ladderClickTradingIssues;
    final TypedChannel<UserCycleRequest> userCycleContractPublisher;
    final TypedChannel<OrderEntryFromServer> orderEntryFromServer;
    final TypedChannel<OrderEntryCommandToServer> orderEntryCommandToServer;
    final TypedChannel<OrderEntryClient.SymbolOrderChannel> orderEntrySymbols;
    final TypedChannel<StackRefPriceDetail> stackRefPriceDetailChannel;
    final TypedChannel<OpxlExDateSubscriber.IsinsGoingEx> isinsGoingEx;

    final TypedChannel<SymbolSelection> symbolSelections;
    final TypedChannel<UltimateParentMapping> ultimateParents;

    final TypedChannel<String> stackParentSymbolPublisher;
    final TypedChannel<StackChildFilter> etfOPXLStackFilters;
    final TypedChannel<StackIncreaseParentOffsetCmd> increaseParentOffsetCmds;
    final TypedChannel<StackIncreaseChildOffsetCmd> increaseChildOffsetBPSCmds;

    final TypedChannel<PicardRow> picardRows;
    final TypedChannel<PicardRow> yodaPicardRows;

    ReddalChannels(final ChannelFactory channelFactory) {

        this.channelFactory = channelFactory;
        this.error = Main.ERROR_CHANNEL;
        this.errorPublisher = new BogusErrorFilteringPublisher(error);
        this.metaData = create(LadderMetadata.class);
        this.position = create(Position.class);
        this.pksExposure = create(PKSExposure.class);
        this.tradingStatus = create(ServerTradingStatus.class);
        this.workingOrders = create(WorkingOrderUpdateFromServer.class);
        this.workingOrderConnectionEstablished = create(WorkingOrderConnectionEstablished.class);
        this.workingOrderEvents = create(WorkingOrderEventFromServer.class);
        this.nibblerTransportConnected = create(NibblerTransportConnected.class);
        this.stats = create(StatsMsg.class);
        this.remoteOrderCommandByServer = new MapMaker().makeComputingMap(from -> create(IOrderCmd.class));
        this.remoteOrderCommand = msg -> remoteOrderCommandByServer.get(msg.toServer).publish(msg.value);
        this.ladderPrefsLoaded = create(LadderSettings.LadderPrefLoaded.class);
        this.storeLadderPref = create(LadderSettings.StoreLadderPref.class);
        this.instDefs = create(InstrumentDef.class);
        this.displaySymbol = create(DisplaySymbol.class);
        this.searchResults = create(SearchResult.class);
        this.stockAlerts = create(StockAlert.class);
        this.heartbeatRoundTrips = create(HeartbeatRoundtrip.class);
        this.recenterLaddersForUser = create(RecenterLaddersForUser.class);
        this.recenterLadder = create(RecenterLadder.class);
        this.contractSets = create(SpreadContractSet.class);
        this.userWorkspaceRequests = create(HostWorkspaceRequest.class);
        this.chixSymbolPairs = create(ChixSymbolPair.class);
        this.singleOrderCommand = create(OrdersPresenter.SingleOrderCommand.class);
        this.trace = create(Jsonable.class);
        this.ladderClickTradingIssues = create(LadderClickTradingIssue.class);
        this.userCycleContractPublisher = create(UserCycleRequest.class);
        this.replaceCommand = create(ReplaceCommand.class);
        this.orderEntryFromServer = create(OrderEntryFromServer.class);
        this.orderEntrySymbols = create(OrderEntryClient.SymbolOrderChannel.class);
        this.orderEntryCommandToServer = create(OrderEntryCommandToServer.class);
        this.stackRefPriceDetailChannel = create(StackRefPriceDetail.class);
        this.isinsGoingEx = create(OpxlExDateSubscriber.IsinsGoingEx.class);

        this.symbolSelections = create(SymbolSelection.class);
        this.ultimateParents = create(UltimateParentMapping.class);

        this.stackParentSymbolPublisher = create(String.class);
        this.etfOPXLStackFilters = create(StackChildFilter.class);
        this.increaseParentOffsetCmds = create(StackIncreaseParentOffsetCmd.class);
        this.increaseChildOffsetBPSCmds = create(StackIncreaseChildOffsetCmd.class);

        this.picardRows = create(PicardRow.class);
        this.yodaPicardRows = create(PicardRow.class);
    }

    <T> TypedChannel<T> create(final Class<T> clazz) {
        return channelFactory.createChannel(clazz, clazz.getSimpleName());
    }
}
