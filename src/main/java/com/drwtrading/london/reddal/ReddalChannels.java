package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.jetlang.ChannelFactory;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import eeif.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.reddal.ladders.HeartbeatRoundtrip;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.ladders.LadderSettings;
import com.drwtrading.london.reddal.ladders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.RecenterLaddersForUser;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderentry.OrderEntryFromServer;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.stacks.opxl.StackRefPriceDetail;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.BogusErrorFilteringPublisher;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderConnectionEstablished;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderEventFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.mrphil.Position;
import com.google.common.collect.MapMaker;
import drw.london.json.Jsonable;
import org.jetlang.channels.Publisher;

import java.util.Map;

public class ReddalChannels {

    public final TypedChannel<Throwable> error;
    public final Publisher<Throwable> errorPublisher;
    public final TypedChannel<LadderMetadata> metaData;
    public final TypedChannel<Position> position;
    public final TypedChannel<PKSExposure> pksExposure;
    public final TypedChannel<TradingStatusWatchdog.ServerTradingStatus> tradingStatus;
    public final TypedChannel<WorkingOrderUpdateFromServer> workingOrders;
    public final TypedChannel<WorkingOrderConnectionEstablished> workingOrderConnectionEstablished;
    public final TypedChannel<WorkingOrderEventFromServer> workingOrderEvents;
    public final TypedChannel<Main.RemoteOrderEventFromServer> remoteOrderEvents;
    public final TypedChannel<StatsMsg> stats;
    public final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommand;
    public final Map<String, TypedChannel<RemoteOrderManagementCommand>> remoteOrderCommandByServer;
    public final TypedChannel<LadderSettings.LadderPrefLoaded> ladderPrefsLoaded;
    public final TypedChannel<LadderSettings.StoreLadderPref> storeLadderPref;
    public final TypedChannel<InstrumentDef> instDefs;
    public final TypedChannel<DisplaySymbol> displaySymbol;
    public final TypedChannel<SearchResult> searchResults;
    public final TypedChannel<StockAlert> stockAlerts;
    public final TypedChannel<HeartbeatRoundtrip> heartbeatRoundTrips;
    private final ChannelFactory channelFactory;
    public final TypedChannel<ReddalMessage> reddalCommand;
    public final TypedChannel<ReddalMessage> reddalCommandSymbolAvailable;
    public final TypedChannel<RecenterLaddersForUser> recenterLaddersForUser;
    public final TypedChannel<SpreadContractSet> contractSets;
    public final TypedChannel<ChixSymbolPair> chixSymbolPairs;
    public final TypedChannel<OrdersPresenter.SingleOrderCommand> singleOrderCommand;
    public final TypedChannel<Jsonable> trace;
    public final TypedChannel<ReplaceCommand> replaceCommand;
    public final TypedChannel<LadderClickTradingIssue> ladderClickTradingIssues;
    public final TypedChannel<UserCycleRequest> userCycleContractPublisher;
    public final TypedChannel<OrderEntryFromServer> orderEntryFromServer;
    public final TypedChannel<OrderEntryCommandToServer> orderEntryCommandToServer;
    public final TypedChannel<OrderEntryClient.SymbolOrderChannel> orderEntrySymbols;
    public final TypedChannel<StackRefPriceDetail> stackRefPriceDetailChannel;
    public final TypedChannel<OpxlExDateSubscriber.IsinsGoingEx> isinsGoingEx;

    public ReddalChannels(final ChannelFactory channelFactory) {

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
    }

    public <T> TypedChannel<T> create(final Class<T> clazz) {
        return channelFactory.createChannel(clazz, clazz.getSimpleName());
    }

}
