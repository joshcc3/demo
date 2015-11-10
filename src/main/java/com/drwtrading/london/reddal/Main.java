package com.drwtrading.london.reddal;

import com.drw.eurex.gen.transport.StreamEnv;
import com.drw.nns.api.MulticastGroup;
import com.drw.nns.api.NnsApi;
import com.drw.nns.api.NnsFactory;
import com.drwtrading.eeif.md.eurex.EasyEurexNaMDS;
import com.drwtrading.eeif.md.eurex.monitors.StatsPublisherErrorMonitor;
import com.drwtrading.eeif.md.euronext_cash.EuronextComponents;
import com.drwtrading.eeif.md.euronext_cash.EuronextMarketDataService;
import com.drwtrading.eeif.md.euronext_cash.EuronextXdpStream;
import com.drwtrading.eeif.md.publishing.MarketDataEventSnapshottingPublisher;
import com.drwtrading.eeif.md.remote.MarketDataSubscriberImpl;
import com.drwtrading.eeif.md.remote.RemoteFilteredClient;
import com.drwtrading.eeif.md.remote.SubscribeMarketData;
import com.drwtrading.eeif.md.remote.UnsubscribeMarketData;
import com.drwtrading.eeif.md.utils.NetworkInterfaceFinder;
import com.drwtrading.eeif.md.utils.TotalTradedVolumeAccumulator;
import com.drwtrading.eeif.md.xetra.XetraComponents;
import com.drwtrading.eeif.md.xetra.XetraMarketDataService;
import com.drwtrading.esquilatency.BatchedChainEventRecorder;
import com.drwtrading.jetlang.NoOpPublisher;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.config.Config;
import com.drwtrading.london.eeif.photocols.client.OnHeapBufferPhotocolsNioClient;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import com.drwtrading.london.eeif.utils.monitoring.ResourceMonitor;
import com.drwtrading.london.jetlang.ChannelFactory;
import com.drwtrading.london.jetlang.FiberGroup;
import com.drwtrading.london.jetlang.JetlangFactory;
import com.drwtrading.london.jetlang.stats.MonitoredJetlangFactory;
import com.drwtrading.london.jetlang.transport.LowTrafficMulticastTransport;
import com.drwtrading.london.logging.ErrorLogger;
import com.drwtrading.london.logging.JsonChannelLogger;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.photons.indy.EquityIdAndSymbol;
import com.drwtrading.london.photons.indy.IndyEnvelope;
import com.drwtrading.london.photons.mdreq.FeedType;
import com.drwtrading.london.photons.mdreq.MdRequest;
import com.drwtrading.london.photons.reddal.Heartbeat;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.data.DisplaySymbol;
import com.drwtrading.london.reddal.opxl.OpxlLadderTextSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlPositionSubscriber;
import com.drwtrading.london.reddal.position.PositionSubscriptionPhotocolsHandler;
import com.drwtrading.london.reddal.safety.ProductResetter;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.symbols.DisplaySymbolMapper;
import com.drwtrading.london.reddal.util.BogusErrorFilteringPublisher;
import com.drwtrading.london.reddal.util.ConnectionCloser;
import com.drwtrading.london.reddal.util.IdleConnectionTimeoutHandler;
import com.drwtrading.london.reddal.util.PhotocolsStatsPublisher;
import com.drwtrading.london.time.Clock;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.MsgCodec;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.StatsPublisher;
import com.drwtrading.monitoring.stats.Transport;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.monitoring.transport.LoggingTransport;
import com.drwtrading.monitoring.transport.MultiplexTransport;
import com.drwtrading.photocols.PhotocolsHandler;
import com.drwtrading.photocols.easy.Photocols;
import com.drwtrading.photocols.handlers.InboundTimeoutWatchdog;
import com.drwtrading.photocols.handlers.JetlangChannelHandler;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.photons.mrphil.Subscription;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.google.common.collect.MapMaker;
import com.sun.jndi.toolkit.url.Uri;
import drw.london.json.Jsonable;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.drwtrading.jetlang.autosubscribe.TypedChannels.create;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class Main {

    public static final long SERVER_TIMEOUT = 3000L;
    public static final int BATCH_FLUSH_INTERVAL_MS = 200;
    public static final int HEARTBEAT_INTERVAL_MS = 20 * BATCH_FLUSH_INTERVAL_MS;
    public static final int NUM_DISPLAY_THREADS = 8;
    public static final long RECONNECT_INTERVAL_MILLIS = 10000;

    public static void createWebPageWithWebSocket(final String alias, final String name, final FiberBuilder fiber, final WebApplication webapp, final TypedChannel<WebSocketControlMessage> websocketChannel) {
        webapp.alias('/' + alias, '/' + name + ".html");
        webapp.createWebSocket('/' + name + "/ws/", websocketChannel, fiber.getFiber());

    }

    public static final TypedChannel<Throwable> ERROR_CHANNEL = create(Throwable.class);

    public static class ReddalChannels {

        public final TypedChannel<Throwable> error;
        public final Publisher<Throwable> errorPublisher;
        public final TypedChannel<LadderMetadata> metaData;
        public final TypedChannel<Position> position;
        public final TypedChannel<TradingStatusWatchdog.ServerTradingStatus> tradingStatus;
        public final TypedChannel<WorkingOrderUpdateFromServer> workingOrders;
        public final TypedChannel<WorkingOrderEventFromServer> workingOrderEvents;
        public final TypedChannel<RemoteOrderEventFromServer> remoteOrderEvents;
        public final TypedChannel<StatsMsg> stats;
        public final TypedChannel<InstrumentDefinitionEvent> refData;
        public final Publisher<RemoteOrderCommandToServer> remoteOrderCommand;
        public final Map<String, TypedChannel<RemoteOrderManagementCommand>> remoteOrderCommandByServer;
        public final TypedChannel<LadderSettings.LadderPrefLoaded> ladderPrefsLoaded;
        public final TypedChannel<LadderSettings.StoreLadderPref> storeLadderPref;
        public final TypedChannel<EquityIdAndSymbol> equityIdAndSymbol;
        public final TypedChannel<DisplaySymbol> displaySymbol;
        public final TypedChannel<LadderView.HeartbeatRoundtrip> heartbeatRoundTrips;
        private final ChannelFactory channelFactory;
        public final TypedChannel<ReddalMessage> reddalCommand;
        public final TypedChannel<ReddalMessage> reddalCommandSymbolAvailable;
        public final TypedChannel<SubscribeMarketData> subscribeToMarketData;
        public final TypedChannel<UnsubscribeMarketData> unsubscribeFromMarketData;
        public final TypedChannel<LadderPresenter.RecenterLaddersForUser> recenterLaddersForUser;
        public final TypedChannel<SpreadContractSet> contractSets;
        public final TypedChannel<OrdersPresenter.SingleOrderCommand> singleOrderCommand;
        public final TypedChannel<Jsonable> trace;
        public final TypedChannel<ReplaceCommand> replaceCommand;
        public final TypedChannel<LadderClickTradingIssue> ladderClickTradingIssues;
        public final TypedChannel<UserCycleRequest> userCycleContractPublisher;

        public ReddalChannels(final ChannelFactory channelFactory) {
            this.channelFactory = channelFactory;
            error = ERROR_CHANNEL;
            errorPublisher = new BogusErrorFilteringPublisher(error);
            metaData = create(LadderMetadata.class);
            position = create(Position.class);
            tradingStatus = create(TradingStatusWatchdog.ServerTradingStatus.class);
            workingOrders = create(WorkingOrderUpdateFromServer.class);
            workingOrderEvents = create(WorkingOrderEventFromServer.class);
            remoteOrderEvents = create(RemoteOrderEventFromServer.class);
            stats = create(StatsMsg.class);
            refData = create(InstrumentDefinitionEvent.class);
            remoteOrderCommandByServer = new MapMaker().makeComputingMap(
                    from -> create(RemoteOrderManagementCommand.class));
            remoteOrderCommand = msg -> remoteOrderCommandByServer.get(msg.toServer).publish(msg.value);
            ladderPrefsLoaded = create(LadderSettings.LadderPrefLoaded.class);
            storeLadderPref = create(LadderSettings.StoreLadderPref.class);
            equityIdAndSymbol = create(EquityIdAndSymbol.class);
            displaySymbol = create(DisplaySymbol.class);
            heartbeatRoundTrips = create(LadderView.HeartbeatRoundtrip.class);
            reddalCommand = create(ReddalMessage.class);
            reddalCommandSymbolAvailable = create(ReddalMessage.class);
            subscribeToMarketData = create(SubscribeMarketData.class);
            unsubscribeFromMarketData = create(UnsubscribeMarketData.class);
            recenterLaddersForUser = create(LadderPresenter.RecenterLaddersForUser.class);
            contractSets = create(SpreadContractSet.class);
            singleOrderCommand = create(OrdersPresenter.SingleOrderCommand.class);
            trace = create(Jsonable.class);
            ladderClickTradingIssues = create(LadderClickTradingIssue.class);
            userCycleContractPublisher = create(UserCycleRequest.class);
            replaceCommand = create(ReplaceCommand.class);
        }

        public <T> TypedChannel<T> create(final Class<T> clazz) {
            return channelFactory.createChannel(clazz, clazz.getSimpleName());
        }

    }

    public static class ReddalFibers {

        private final JetlangFactory jetlangFactory;
        public final FiberGroup fiberGroup;
        public final Fiber starter;
        public final FiberBuilder logging;
        public final FiberBuilder ui;
        public final FiberBuilder stats;
        public final FiberBuilder marketData;
        public final FiberBuilder metaData;
        public final FiberBuilder workingOrders;
        public final FiberBuilder selecta;
        public final FiberBuilder remoteOrders;
        public final FiberBuilder opxlPosition;
        public final FiberBuilder opxlText;
        public final FiberBuilder mrPhil;
        public final FiberBuilder indy;
        public final FiberBuilder watchdog;
        public final FiberBuilder settings;
        public final FiberBuilder ladder;
        public final FiberBuilder contracts;

        public ReddalFibers(final ReddalChannels channels, final MonitoredJetlangFactory factory) {
            jetlangFactory = factory;
            fiberGroup = new FiberGroup(jetlangFactory, "Fibers", channels.error);
            starter = jetlangFactory.createFiber("Starter");
            fiberGroup.wrap(starter, "Starter");
            logging = fiberGroup.create("Logging");
            ui = fiberGroup.create("UI");
            ladder = fiberGroup.create("Ladder");
            stats = fiberGroup.create("Stats");
            marketData = fiberGroup.create("Market data");
            metaData = fiberGroup.create("Metadata");
            workingOrders = fiberGroup.create("Working orders");
            selecta = fiberGroup.create("Selecta");
            remoteOrders = fiberGroup.create("Remote orders");
            opxlPosition = fiberGroup.create("OPXL Position");
            opxlText = fiberGroup.create("OPXL Text");
            mrPhil = fiberGroup.create("Mr Phil");
            indy = fiberGroup.create("Indy");
            watchdog = fiberGroup.create("Watchdog");
            settings = fiberGroup.create("Settings");
            contracts = fiberGroup.create("Contracts");
        }

        public void onStart(final Runnable runnable) {
            starter.execute(runnable);
        }

        public void start() {
            fiberGroup.start();
        }
    }

    public static class WorkingOrderUpdateFromServer extends Struct {
        public final String fromServer;
        public final WorkingOrderUpdate value;

        public WorkingOrderUpdateFromServer(final String fromServer, final WorkingOrderUpdate value) {
            this.fromServer = fromServer;
            this.value = value;
        }

        public String key() {
            return fromServer + '_' + value.getChainId();
        }
    }

    public static class WorkingOrderEventFromServer extends Struct {
        public final String fromServer;
        public final WorkingOrderEvent value;

        public WorkingOrderEventFromServer(final String fromServer, final WorkingOrderEvent value) {
            this.fromServer = fromServer;
            this.value = value;
        }
    }

    public static class RemoteOrderCommandToServer extends Struct {
        public final String toServer;
        public final RemoteOrderManagementCommand value;

        public RemoteOrderCommandToServer(final String toServer, final RemoteOrderManagementCommand value) {
            this.toServer = toServer;
            this.value = value;
        }
    }

    public static class RemoteOrderEventFromServer extends Struct {
        public final String fromServer;
        public final RemoteOrderManagementEvent value;

        public RemoteOrderEventFromServer(final String fromServer, final RemoteOrderManagementEvent value) {
            this.fromServer = fromServer;
            this.value = value;
        }
    }

    public static void main(final String[] args) {
        try {
            start(args);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static void start(final String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.out.println("Configuration name required as argument.");
            System.exit(-1);
        }


        final String configName = args[0];

        System.out.println("Starting with configuration: " + configName);
        final Path configFile = Paths.get("./etc", configName + ".properties");
        final Config config = Config.fromFile(configFile.toFile());
        final Environment environment = new Environment(config);
        final File logDir = environment.getLogDirectory(configName);

        final NnsApi nnsApi = new NnsFactory().create();
        final LoggingTransport fileTransport = new LoggingTransport(new File(logDir, "jetlang.log"));
        fileTransport.start();
        final MulticastGroup statsGroup = nnsApi.multicastGroupFor(environment.getStatsNns());
        final LowTrafficMulticastTransport lowTrafficMulticastTransport = new LowTrafficMulticastTransport(statsGroup.getAddress(), statsGroup.getPort(), environment.getStatsInterface());
        final AtomicBoolean multicastEnabled = new AtomicBoolean(false);
        final Transport enableableMulticastTransport = createEnableAbleTransport(lowTrafficMulticastTransport, multicastEnabled);
        final StatsPublisher statsPublisher = new StatsPublisher(environment.getStatsName(), new MultiplexTransport(fileTransport, enableableMulticastTransport));

        final MonitoredJetlangFactory monitoredJetlangFactory = new MonitoredJetlangFactory(statsPublisher, ERROR_CHANNEL);
        final ReddalChannels channels = new ReddalChannels(monitoredJetlangFactory);
        final ReddalFibers fibers = new ReddalFibers(channels, monitoredJetlangFactory);


        final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (t, e) -> channels.errorPublisher.publish(e);

        // Monitoring
        {
            fibers.stats.subscribe(statsPublisher::publish, channels.stats);
            fibers.stats.getFiber().schedule(() -> {
                statsPublisher.start();
                multicastEnabled.set(true);
            }, 10, TimeUnit.SECONDS);
        }


        // WebApp
        final Map<String, TypedChannel<WebSocketControlMessage>> websocketsForLogging = newHashMap();
        {
            final WebApplication webapp;
            webapp = new WebApplication(environment.getWebPort(), channels.errorPublisher);
            System.out.println("http://localhost:" + environment.getWebPort());
            webapp.enableSingleSignOn();

            fibers.onStart(() -> fibers.ui.execute(() -> {
                try {
                    webapp.serveStaticContent("web");
                    webapp.start();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            // Index presenter
            {
                final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("/", "index", fibers.ui, webapp, websocket);
                websocketsForLogging.put("index", websocket);
                final IndexPresenter indexPresenter = new IndexPresenter();
                fibers.ui.subscribe(indexPresenter, channels.displaySymbol, channels.refData, websocket);
            }

            // Orders presenter
            {
                final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("orders", "orders", fibers.ui, webapp, websocket);
                websocketsForLogging.put("orders", websocket);
                final OrdersPresenter ordersPresenter = new OrdersPresenter(channels.singleOrderCommand);
                fibers.ui.subscribe(ordersPresenter, websocket);
                channels.workingOrders.subscribe(new BatchSubscriber<>(fibers.ui.getFiber(), ordersPresenter::onWorkingOrderBatch, 100, TimeUnit.MILLISECONDS));
            }

            // Working orders screen
            {
                TypedChannel<WebSocketControlMessage> ws = create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("workingorders", "workingorders", fibers.ui, webapp, ws);
                WorkingOrdersPresenter presenter = new WorkingOrdersPresenter(fibers.ui.getFiber(), channels.stats, channels.remoteOrderCommand);
                fibers.ui.subscribe(presenter, channels.refData, ws);
                channels.workingOrders.subscribe(new BatchSubscriber<>(fibers.ui.getFiber(), presenter::onWorkingOrderBatch, 100, TimeUnit.MILLISECONDS));
            }

            // Ladder presenters
            final List<TypedChannel<WebSocketControlMessage>> websockets = newArrayList();
            {
                for (int i = 0; i < NUM_DISPLAY_THREADS; i++) {
                    final TypedChannel<WebSocketControlMessage> websocket = create(WebSocketControlMessage.class);
                    websockets.add(websocket);
                    final String name = "Ladder-" + (i);
                    final FiberBuilder fiberBuilder = fibers.fiberGroup.create(name);
                    final LadderPresenter presenter =
                            new LadderPresenter(channels.remoteOrderCommand, environment.ladderOptions(), channels.stats,
                                    channels.storeLadderPref, channels.heartbeatRoundTrips, channels.reddalCommand,
                                    channels.subscribeToMarketData, channels.unsubscribeFromMarketData, channels.recenterLaddersForUser,
                                    fiberBuilder.getFiber(), channels.trace, channels.ladderClickTradingIssues,
                                    channels.userCycleContractPublisher);
                    fiberBuilder.subscribe(presenter,
                            websocket,
                            channels.workingOrders,
                            channels.metaData,
                            channels.position,
                            channels.tradingStatus,
                            channels.ladderPrefsLoaded,
                            channels.displaySymbol,
                            channels.reddalCommandSymbolAvailable,
                            channels.recenterLaddersForUser,
                            channels.contractSets,
                            channels.singleOrderCommand,
                            channels.ladderClickTradingIssues,
                            channels.replaceCommand,
                            channels.userCycleContractPublisher);
                    fiberBuilder.getFiber().scheduleWithFixedDelay(presenter::flushAllLadders, 10 + i * (BATCH_FLUSH_INTERVAL_MS / websockets.size()), BATCH_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    fiberBuilder.getFiber().scheduleWithFixedDelay(presenter::sendAllHeartbeats, 10 + i * (HEARTBEAT_INTERVAL_MS / websockets.size()), HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
                }
            }

            // Ladder router
            {
                final TypedChannel<WebSocketControlMessage> ladderWebSocket = create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("ladder", "ladder", fibers.ladder, webapp, ladderWebSocket);
                final LadderMessageRouter ladderMessageRouter = new LadderMessageRouter(websockets);
                fibers.ladder.subscribe(ladderMessageRouter, ladderWebSocket);
            }


        }


        // Non SSO-protected webapp to allow AJAX requests
        {

            final WebApplication webapp;
            webapp = new WebApplication(environment.getWebPort() + 1, channels.errorPublisher);

            fibers.onStart(() -> fibers.ui.execute(() -> {
                try {
                    webapp.serveStaticContent("web");
                    webapp.start();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            webapp.webServer();

            // Workspace
            {
                final TypedChannel<WebSocketControlMessage> workspaceSocket = create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("workspace", "workspace", fibers.ui, webapp, workspaceSocket);

                final LadderWorkspace ladderWorkspace = new LadderWorkspace(channels.replaceCommand);
                fibers.ui.subscribe(ladderWorkspace, workspaceSocket, channels.contractSets);
                webapp.addHandler("/open", new WorkspaceRequestHandler(ladderWorkspace, new Uri(webapp.getBaseUri()).getHost(), environment.getWebPort()));
            }


        }

        // Settings
        {
            final LadderSettings ladderSettings = new LadderSettings(environment.getSettingsFile(), channels.ladderPrefsLoaded);
            fibers.settings.subscribe(ladderSettings, channels.storeLadderPref);
            fibers.onStart(() -> fibers.settings.execute(ladderSettings.loadRunnable()));
        }

        // Contract sets
        {
            final FuturesContractSetGenerator futuresContractSetGenerator = new FuturesContractSetGenerator(channels.contractSets);
            fibers.contracts.subscribe(futuresContractSetGenerator, channels.refData);

            final SyntheticSpreadContractSetGenerator generator = new SyntheticSpreadContractSetGenerator(channels.contractSets);
            fibers.contracts.subscribe(generator, channels.refData);
        }

        // Market data
        {


            IErrorLogger errorLog = new IErrorLogger() {
                @Override
                public boolean isDebugEnabled() {
                    return true;
                }

                @Override
                public void debug(String msg) {
                    System.out.println(new DateTime() + "\tDBG: " + msg);
                }

                @Override
                public void debug(String msg, Throwable t) {
                    debug(msg);
                    t.printStackTrace();
                }

                @Override
                public void error(String msg) {
                    statsPublisher.publish(new AdvisoryStat("Reddal", AdvisoryStat.Level.WARNING, msg));
                }

                @Override
                public void error(String msg, Throwable t) {
                    error(msg);
                    channels.error.publish(t);
                }
            };


            for (final String mds : environment.getList(Environment.MARKET_DATA)) {


                System.out.println(mds + " connecting to: " + environment.getMarketDataExchange(mds));

                final MarketDataEventSnapshottingPublisher snapshottingPublisher = new MarketDataEventSnapshottingPublisher();
                final FiberBuilder fiber = fibers.fiberGroup.create("Market Data: " + mds);
                final ProductResetter productResetter = new ProductResetter(snapshottingPublisher);
                final MarketDataSubscriberImpl marketDataSubscriber = new MarketDataSubscriberImpl(channels.refData);
                fiber.subscribe(marketDataSubscriber, channels.subscribeToMarketData, channels.unsubscribeFromMarketData);
                Publisher<MarketDataEvent> marketDataEventPublisher = marketDataSubscriber;

                if (environment.getMarketDataExchange(mds) == Environment.Exchange.EUREX) {

                    final TotalTradedVolumeAccumulator totalTradedVolumeAccumulator = new TotalTradedVolumeAccumulator(marketDataEventPublisher);


                    final EasyEurexNaMDS easyEurexNaMDS = new EasyEurexNaMDS(
                            environment.getMarkets(mds),
                            totalTradedVolumeAccumulator, totalTradedVolumeAccumulator, totalTradedVolumeAccumulator,
                            statsPublisher,
                            channels.error,
                            StreamEnv.prod,
                            environment.getMarketDataInterface(mds),
                            new StatsPublisherErrorMonitor(statsPublisher),
                            logDir
                    ).withImpliedTopOfBooks(totalTradedVolumeAccumulator, true);

                    fibers.onStart(() -> fiber.execute(() -> {
                        try {
                            easyEurexNaMDS.start();
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                } else if (environment.getMarketDataExchange(mds) == Environment.Exchange.XETRA) {

                    final TotalTradedVolumeAccumulator totalTradedVolumeAccumulator = new TotalTradedVolumeAccumulator(marketDataEventPublisher);

                    SelectIO selectIO = new SelectIO(errorLog);
                    final XetraMarketDataService xetraMarketDataService = new XetraMarketDataService(
                            selectIO,
                            new ResourceMonitor<>("Xetra", XetraComponents.class, errorLog, true),
                            NetworkInterfaceFinder.find(environment.getMarketDataInterface(mds)),
                            environment.getXetraMarkets(mds),
                            totalTradedVolumeAccumulator,
                            environment.getXetraReferenceDataFile(mds),
                            environment.getXetraReferenceDataStreams(mds),
                            null,
                            null,
                            message -> {
                                try {
                                    System.out.println("Xetra dropped a packet: ");
                                    message.toJson(System.out);
                                } catch (final IOException e) {
                                    channels.error.publish(e);
                                }
                                statsPublisher.publish(new AdvisoryStat("Reddal/Xetra", AdvisoryStat.Level.WARNING, "Dropped a packet."));
                            },
                            ERROR_CHANNEL,
                            BatchedChainEventRecorder.nullRecorder(),
                            new NoOpPublisher<>());

                    selectIO.execute(() -> {
                        try {
                            xetraMarketDataService.initialise();
                        } catch (IOException e) {
                            channels.error.publish(e);
                        }
                    });

                    fibers.onStart(() -> {
                        try {
                            selectIO.start("Xetra MD");
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                } else if (environment.getMarketDataExchange(mds) == Environment.Exchange.EURONEXT) {

                    final String prefix = Environment.MARKET_DATA + "." + mds;
                    SelectIO selectIO = new SelectIO(errorLog);

                    final EuronextMarketDataService marketDataService =
                            new EuronextMarketDataService(selectIO,
                                    new ResourceMonitor<>("Euronext", EuronextComponents.class, errorLog, true),
                                    NetworkInterfaceFinder.find(config.get(prefix + ".nic")),
                                    marketDataEventPublisher,
                                    EuronextXdpStream.Configuration.valueOf(config.get(prefix + ".environment")),
                                    new InetSocketAddress(config.get(prefix + ".refreshAddr"), config.getInt(prefix + ".refreshPort")),
                                    config.get(prefix + ".sourceId"), new NoOpPublisher<>(),
                                    new ObjectArraySet<>(config.get(prefix + ".mics").split(",")),
                                    null,
                                    null);

                    fibers.fiberGroup.create("Euronext Cash Runner").execute(() -> {
                        try {
                            marketDataService.startSync();
                        } catch (final IOException e) {
                            ERROR_CHANNEL.publish(e);
                        }
                    });


                } else if (environment.getMarketDataExchange(mds) == Environment.Exchange.REMOTE) {
                    final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.MARKET_DATA, mds);
                    final OnHeapBufferPhotocolsNioClient<MarketDataEvent, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), MarketDataEvent.class, Void.class, fiber.getFiber(), EXCEPTION_HANDLER);
                    final ConnectionCloser connectionCloser = new ConnectionCloser(channels.stats, "Market data: " + mds, productResetter.resetRunnable());
                    client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                            .handler(new IdleConnectionTimeoutHandler<>(connectionCloser, SERVER_TIMEOUT, fiber.getFiber()))
                            .handler(new JetlangChannelHandler<>(marketDataEventPublisher));
                    fibers.onStart(() -> fiber.execute(client::start));
                } else if (environment.getMarketDataExchange(mds) == Environment.Exchange.FILTERED) {
                    RemoteFilteredClient filteredClient = new RemoteFilteredClient(channels.refData, FeedType.FULL_BOOK);
                    fiber.subscribe(filteredClient, channels.subscribeToMarketData, channels.unsubscribeFromMarketData);
                    final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.MARKET_DATA, mds);
                    final OnHeapBufferPhotocolsNioClient<MarketDataEvent, MdRequest> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), MarketDataEvent.class, MdRequest.class, fiber.getFiber(), EXCEPTION_HANDLER);
                    final ConnectionCloser connectionCloser = new ConnectionCloser(channels.stats, "Market data: " + mds, productResetter.resetRunnable());
                    client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                            .handler(new IdleConnectionTimeoutHandler<>(connectionCloser, SERVER_TIMEOUT, fiber.getFiber()));
                    client.handler(filteredClient);
                    fibers.onStart(() -> fiber.execute(client::start));
                }
            }
        }

        // Meta data
        {
            for (final String server : environment.getList(Environment.METADATA)) {
                final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.METADATA, server);
                final OnHeapBufferPhotocolsNioClient<LadderMetadata, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class, Void.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "metadata." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<>(channels.stats, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<>(channels.metaData));
                fibers.onStart(client::start);
            }
        }

        // Remote commands
        {
            for (final String server : environment.getList(Environment.REMOTE_COMMANDS)) {
                final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.REMOTE_COMMANDS, server);
                final OnHeapBufferPhotocolsNioClient<RemoteOrderManagementEvent, RemoteOrderManagementCommand> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), RemoteOrderManagementEvent.class, RemoteOrderManagementCommand.class, fibers.workingOrders.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "remote-commands." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<>(channels.stats, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<>(msg -> channels.remoteOrderEvents.publish(new RemoteOrderEventFromServer(server, msg)), channels.remoteOrderCommandByServer.get(server), fibers.remoteOrders.getFiber()))
                        .handler(new InboundTimeoutWatchdog<>(fibers.remoteOrders.getFiber(), new ConnectionCloser(channels.stats, "Remote order: " + server), SERVER_TIMEOUT));
                fibers.onStart(client::start);
            }
        }


        // Reddal server
        {
            final Photocols<ReddalMessage, ReddalMessage> commandServer = Photocols.server(new InetSocketAddress(environment.getCommandsPort()), ReddalMessage.class, ReddalMessage.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
            commandServer.logFile(new File(logDir, "photocols.commands.log"), fibers.logging.getFiber(), true);
            commandServer.endpoint().add(new JetlangChannelHandler<>(channels.reddalCommandSymbolAvailable, channels.reddalCommand, fibers.metaData.getFiber()));
            fibers.onStart(() -> {
                try {
                    commandServer.start();
                } catch (final InterruptedException e) {
                    channels.errorPublisher.publish(e);
                }
            });
            fibers.metaData.getFiber().scheduleWithFixedDelay(() -> commandServer.publish(new Heartbeat()), 1000, 1000, TimeUnit.MILLISECONDS);
        }

        // Working orders
        {
            for (final String server : environment.getList(Environment.WORKING_ORDERS)) {
                final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.WORKING_ORDERS, server);
                final OnHeapBufferPhotocolsNioClient<WorkingOrderEvent, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), WorkingOrderEvent.class, Void.class, fibers.workingOrders.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "working-orders." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<>(channels.stats, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<>(msg -> {
                            if (msg instanceof WorkingOrderUpdate) {
                                channels.workingOrders.publish(new WorkingOrderUpdateFromServer(server, (WorkingOrderUpdate) msg));
                            }
                            channels.workingOrderEvents.publish(new WorkingOrderEventFromServer(server, msg));
                        }))
                        .handler(new InboundTimeoutWatchdog<>(fibers.workingOrders.getFiber(), new ConnectionCloser(channels.stats, "Working order: " + server), SERVER_TIMEOUT));
                fibers.onStart(client::start);
            }
        }

        // Working orders and remote commands watchdog
        {
            final TradingStatusWatchdog watchdog = new TradingStatusWatchdog(channels.tradingStatus, SERVER_TIMEOUT, Clock.SYSTEM, channels.stats);
            fibers.watchdog.subscribe(watchdog, channels.workingOrderEvents, channels.remoteOrderEvents);
            fibers.watchdog.getFiber().scheduleWithFixedDelay(watchdog::checkHeartbeats, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        // Mr. Phil position
        {
            final Environment.HostAndNic hostAndNic = environment.getMrPhilHostAndNic();
            final OnHeapBufferPhotocolsNioClient<Position, Subscription> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, hostAndNic.nic,
                    Position.class, Subscription.class, fibers.mrPhil.getFiber(), EXCEPTION_HANDLER);
            final PhotocolsHandler<Position, Subscription> positionHandler = new PositionSubscriptionPhotocolsHandler(channels.position);
            fibers.mrPhil.subscribe(positionHandler, channels.refData);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                    .logFile(new File(logDir, "mr-phil.log"), fibers.logging.getFiber(), true)
                    .handler(positionHandler);
            fibers.onStart(client::start);
        }

        // Indy
        {
            if (environment.indyEnabled()) {
                final Environment.HostAndNic hostAndNic = environment.getIndyHostAndNic();
                final OnHeapBufferPhotocolsNioClient<IndyEnvelope, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, hostAndNic.nic, IndyEnvelope.class, Void.class, fibers.mrPhil.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "indy.log"), fibers.logging.getFiber(), true)
                        .handler(new JetlangChannelHandler<>(msg -> {
                            if (msg.getMessage() instanceof EquityIdAndSymbol) {
                                channels.equityIdAndSymbol.publish((EquityIdAndSymbol) msg.getMessage());
                            }
                        }));
                fibers.onStart(client::start);
            }
        }

        // Display symbols
        {
            fibers.indy.subscribe(new DisplaySymbolMapper(channels.displaySymbol), channels.refData, channels.equityIdAndSymbol);
        }

        // Desk Position
        {
            if (environment.opxlDeskPositionEnabled()) {
                for (final String key : environment.opxlDeskPositionKeys()) {
                    final OpxlPositionSubscriber opxlPositionSubscriber = new OpxlPositionSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), channels.errorPublisher, key, channels.metaData::publish);
                    fibers.onStart(() -> fibers.opxlPosition.execute(opxlPositionSubscriber.connectToOpxl()));
                }
            }
        }

        // Ladder Text
        {
            if (environment.opxlLadderTextEnabled()) {

                final Collection<String> keys = environment.getOpxlLadderTextKeys();

                for (final String key : keys) {
                    final OpxlLadderTextSubscriber opxlLadderTextSubscriber = new OpxlLadderTextSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), channels.errorPublisher, key, channels.metaData);
                    fibers.onStart(() -> fibers.opxlText.execute(opxlLadderTextSubscriber.connectToOpxl()));

                }

            }
        }

        // Error souting
        {
            channels.error.subscribe(fibers.logging.getFiber(), message -> {
                System.out.println(new Date().toString());
                message.printStackTrace();
            });
            channels.stats.subscribe(fibers.logging.getFiber(), message -> {
                if (message instanceof AdvisoryStat) {
                    System.out.println(new Date().toString());
                    System.out.println(message.toString());
                }
            });
        }

        // Logging
        {
            fibers.logging.subscribe(new ErrorLogger(new File(logDir, "errors.log")).onThrowableCallback(), channels.error);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "remote-order.json", channels.errorPublisher), channels.workingOrderEvents, channels.remoteOrderEvents);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "trading-status.json", channels.errorPublisher), channels.tradingStatus);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "preferences.json", channels.errorPublisher), channels.ladderPrefsLoaded, channels.storeLadderPref);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "status.json", channels.errorPublisher), channels.stats);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "reference-data.json", channels.errorPublisher), channels.refData);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "heartbeats.json", channels.errorPublisher), channels.heartbeatRoundTrips);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "contracts.json", channels.errorPublisher), channels.contractSets);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "single-order.json", channels.errorPublisher), channels.singleOrderCommand);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "trace.json", channels.errorPublisher), channels.trace);
            for (final String name : websocketsForLogging.keySet()) {
                fibers.logging.subscribe(new JsonChannelLogger(logDir, "websocket" + name + ".json", channels.errorPublisher), websocketsForLogging.get(
                        name));
            }
        }

        fibers.start();
        new CountDownLatch(1).await();
    }

    private static Transport createEnableAbleTransport(final LowTrafficMulticastTransport lowTrafficMulticastTransport, final AtomicBoolean multicastEnabled) {
        return new Transport() {
            @Override
            public <T> void publish(final MsgCodec<T> codec, final T msg) {
                if (multicastEnabled.get()) {
                    lowTrafficMulticastTransport.publish(codec, msg);
                }
            }

            @Override
            public void start() {
                lowTrafficMulticastTransport.start();
            }

            @Override
            public void stop() {
                lowTrafficMulticastTransport.stop();
            }
        };
    }


}
