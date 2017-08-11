package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.jetlang.ChannelFactory;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.reddal.ladders.HeartbeatRoundtrip;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.ladders.LadderSettings;
import com.drwtrading.london.reddal.ladders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.RecenterLaddersForUser;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderEntryFromServer;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
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
import eeif.execution.RemoteOrderManagementCommand;
import org.jetlang.channels.Publisher;

import java.util.Map;

class ReddalChannels {

    private final ChannelFactory channelFactory;

    final TypedChannel<Throwable> error;
    final Publisher<Throwable> errorPublisher;
    final TypedChannel<LadderMetadata> metaData;
    final TypedChannel<Position> position;
    final TypedChannel<PKSExposure> pksExposure;
    final TypedChannel<TradingStatusWatchdog.ServerTradingStatus> tradingStatus;
    final TypedChannel<WorkingOrderUpdateFromServer> workingOrders;
    final TypedChannel<WorkingOrderConnectionEstablished> workingOrderConnectionEstablished;
    final TypedChannel<WorkingOrderEventFromServer> workingOrderEvents;
    final TypedChannel<Main.RemoteOrderEventFromServer> remoteOrderEvents;
    final TypedChannel<StatsMsg> stats;
    final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommand;
    final Map<String, TypedChannel<RemoteOrderManagementCommand>> remoteOrderCommandByServer;
    final TypedChannel<LadderSettings.LadderPrefLoaded> ladderPrefsLoaded;
    final TypedChannel<LadderSettings.StoreLadderPref> storeLadderPref;
    final TypedChannel<InstrumentDef> instDefs;
    final TypedChannel<DisplaySymbol> displaySymbol;
    final TypedChannel<SearchResult> searchResults;
    final TypedChannel<StockAlert> stockAlerts;
    final TypedChannel<HeartbeatRoundtrip> heartbeatRoundTrips;

    final TypedChannel<ReddalMessage> reddalCommand;
    final TypedChannel<ReddalMessage> reddalCommandSymbolAvailable;
    final TypedChannel<RecenterLaddersForUser> recenterLaddersForUser;
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

    ReddalChannels(final ChannelFactory channelFactory) {

        this.channelFactory = channelFactory;
        this.error = Main.ERROR_CHANNEL;
        this.errorPublisher = new BogusErrorFilteringPublisher(error);
        this.metaData = create(LadderMetadata.class);
        this.position = create(Position.class);
        this.pksExposure = create(PKSExposure.class);
        this.tradingStatus = create(TradingStatusWatchdog.ServerTradingStatus.class);
        this.workingOrders = create(WorkingOrderUpdateFromServer.class);
        this.workingOrderConnectionEstablished = create(WorkingOrderConnectionEstablished.class);
        this.workingOrderEvents = create(WorkingOrderEventFromServer.class);
        this.remoteOrderEvents = create(Main.RemoteOrderEventFromServer.class);
        this.stats = create(StatsMsg.class);
        this.remoteOrderCommandByServer = new MapMaker().makeComputingMap(from -> create(RemoteOrderManagementCommand.class));
        this.remoteOrderCommand = msg -> remoteOrderCommandByServer.get(msg.toServer).publish(msg.value);
        this.ladderPrefsLoaded = create(LadderSettings.LadderPrefLoaded.class);
        this.storeLadderPref = create(LadderSettings.StoreLadderPref.class);
        this.instDefs = create(InstrumentDef.class);
        this.displaySymbol = create(DisplaySymbol.class);
        this.searchResults = create(SearchResult.class);
        this.stockAlerts = create(StockAlert.class);
        this.heartbeatRoundTrips = create(HeartbeatRoundtrip.class);
        this.reddalCommand = create(ReddalMessage.class);
        this.reddalCommandSymbolAvailable = create(ReddalMessage.class);
        this.recenterLaddersForUser = create(RecenterLaddersForUser.class);
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
    }

    <T> TypedChannel<T> create(final Class<T> clazz) {
        return channelFactory.createChannel(clazz, clazz.getSimpleName());
    }
}
