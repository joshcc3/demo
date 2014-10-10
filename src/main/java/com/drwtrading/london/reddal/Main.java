package com.drwtrading.london.reddal;

import com.drw.eurex.gen.transport.StreamEnv;
import com.drw.nns.api.MulticastGroup;
import com.drw.nns.api.NnsApi;
import com.drw.nns.api.NnsFactory;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.IndexPresenter;
import com.drwtrading.london.LadderMessageRouter;
import com.drwtrading.london.config.Config;
import com.drwtrading.london.eeif.photocols.client.OnHeapBufferPhotocolsNioClient;
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
import com.drwtrading.london.photons.reddal.Heartbeat;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BookSnapshot;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.PriceType;
import com.drwtrading.london.protocols.photon.marketdata.ServerHeartbeat;
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
import com.drwtrading.marketdata.service.accumulators.TotalTradedVolumeAccumulator;
import com.drwtrading.marketdata.service.common.KeyedPublisher;
import com.drwtrading.marketdata.service.common.TraderMarket;
import com.drwtrading.marketdata.service.eurex_na.EasyEurexNaMDS;
import com.drwtrading.marketdata.service.eurex_na.monitors.StatsPublisherErrorMonitor;
import com.drwtrading.marketdata.service.snapshot.publishing.MarketDataEventSnapshottingPublisher;
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
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.photons.mrphil.Subscription;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
    public static final int BATCH_FLUSH_INTERVAL_MS = 105;
    public static final int HEARTBEAT_INTERVAL_MS = 20 * BATCH_FLUSH_INTERVAL_MS;
    public static final int NUM_DISPLAY_THREADS = 4;
    private static final long RECONNECT_INTERVAL_MILLIS = 10000;

    public static void createWebPageWithWebSocket(String alias, String name, FiberBuilder fiber, WebApplication webapp, final TypedChannel<WebSocketControlMessage> websocketChannel) {
        webapp.alias("/" + alias, "/" + name + ".html");
        webapp.createWebSocket("/" + name + "/ws/", websocketChannel, fiber.getFiber());
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
        public final TypedChannel<SubscribeToMarketData> subscribeToMarketData;
        public final TypedChannel<UnsubscribeFromMarketData> unsubscribeFromMarketData;
        public final TypedChannel<LadderPresenter.RecenterLaddersForUser> recenterLaddersForUser;
        public final TypedChannel<FuturesContractSetGenerator.FuturesContractSet> futuresContractSets;

        public ReddalChannels(ChannelFactory channelFactory) {
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
            remoteOrderCommandByServer = new MapMaker().makeComputingMap(new Function<String, TypedChannel<RemoteOrderManagementCommand>>() {
                @Override
                public TypedChannel<RemoteOrderManagementCommand> apply(String from) {
                    return create(RemoteOrderManagementCommand.class);
                }
            });
            remoteOrderCommand = new Publisher<RemoteOrderCommandToServer>() {
                @Override
                public void publish(RemoteOrderCommandToServer msg) {
                    remoteOrderCommandByServer.get(msg.toServer).publish(msg.value);
                }
            };
            ladderPrefsLoaded = create(LadderSettings.LadderPrefLoaded.class);
            storeLadderPref = create(LadderSettings.StoreLadderPref.class);
            equityIdAndSymbol = create(EquityIdAndSymbol.class);
            displaySymbol = create(DisplaySymbol.class);
            heartbeatRoundTrips = create(LadderView.HeartbeatRoundtrip.class);
            reddalCommand = create(ReddalMessage.class);
            reddalCommandSymbolAvailable = create(ReddalMessage.class);

            subscribeToMarketData = create(SubscribeToMarketData.class);
            unsubscribeFromMarketData = create(UnsubscribeFromMarketData.class);
            recenterLaddersForUser = create(LadderPresenter.RecenterLaddersForUser.class);
            futuresContractSets = create(FuturesContractSetGenerator.FuturesContractSet.class);
        }

        public <T> TypedChannel<T> create(Class<T> clazz) {
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

        public ReddalFibers(ReddalChannels channels, final MonitoredJetlangFactory factory) throws IOException {
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

        public void onStart(Runnable runnable) {
            starter.execute(runnable);
        }

        public void start() {
            fiberGroup.start();
        }
    }

    public static class WorkingOrderUpdateFromServer extends Struct {
        public final String fromServer;
        public final WorkingOrderUpdate value;

        public WorkingOrderUpdateFromServer(String fromServer, WorkingOrderUpdate value) {
            this.fromServer = fromServer;
            this.value = value;
        }

        public String key() {
            return fromServer + ":" + value.getChainId();
        }
    }

    public static class WorkingOrderEventFromServer extends Struct {
        public final String fromServer;
        public final WorkingOrderEvent value;

        public WorkingOrderEventFromServer(String fromServer, WorkingOrderEvent value) {
            this.fromServer = fromServer;
            this.value = value;
        }
    }

    public static class RemoteOrderCommandToServer extends Struct {
        public final String toServer;
        public final RemoteOrderManagementCommand value;

        public RemoteOrderCommandToServer(String toServer, RemoteOrderManagementCommand value) {
            this.toServer = toServer;
            this.value = value;
        }
    }

    public static class RemoteOrderEventFromServer extends Struct {
        public final String fromServer;
        public final RemoteOrderManagementEvent value;

        public RemoteOrderEventFromServer(String fromServer, RemoteOrderManagementEvent value) {
            this.fromServer = fromServer;
            this.value = value;
        }
    }

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void start(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.out.println("Configuration name required as argument.");
            System.exit(-1);
        }


        final String configName = args[0];

        System.out.println("Starting with configuration: " + configName);
        final Config config = Config.fromFile(new File("./etc", configName + ".properties"));
        final Environment environment = new Environment(config);
        final File logDir = environment.getLogDirectory(configName);

        NnsApi nnsApi = new NnsFactory().create();
        final LoggingTransport fileTransport = new LoggingTransport(new File(logDir, "jetlang.log"));
        fileTransport.start();
        MulticastGroup statsGroup = nnsApi.multicastGroupFor(environment.getStatsNns());
        final LowTrafficMulticastTransport lowTrafficMulticastTransport = new LowTrafficMulticastTransport(statsGroup.getAddress(), statsGroup.getPort(), environment.getStatsInterface());
        final AtomicBoolean multicastEnabled = new AtomicBoolean(false);
        Transport enableableMulticastTransport = createEnableAbleTransport(lowTrafficMulticastTransport, multicastEnabled);
        final StatsPublisher statsPublisher = new StatsPublisher(environment.getStatsName(), new MultiplexTransport(fileTransport, enableableMulticastTransport));

        final MonitoredJetlangFactory monitoredJetlangFactory = new MonitoredJetlangFactory(statsPublisher, ERROR_CHANNEL);
        final ReddalChannels channels = new ReddalChannels(monitoredJetlangFactory);
        final ReddalFibers fibers = new ReddalFibers(channels, monitoredJetlangFactory);


        final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                channels.errorPublisher.publish(e);
            }
        };

        // Monitoring
        {
            fibers.stats.subscribe(new Callback<StatsMsg>() {
                @Override
                public void onMessage(StatsMsg message) {
                    statsPublisher.publish(message);
                }
            }, channels.stats);
            fibers.stats.getFiber().schedule(new Runnable() {
                @Override
                public void run() {
                    statsPublisher.start();
                    multicastEnabled.set(true);
                }
            }, 10, TimeUnit.SECONDS);
        }


        // WebApp
        Map<String, TypedChannel<WebSocketControlMessage>> websocketsForLogging = newHashMap();
        {
            final WebApplication webapp;
            webapp = new WebApplication(environment.getWebPort(), channels.errorPublisher);
            webapp.enableSingleSignOn();

            fibers.onStart(new Runnable() {
                @Override
                public void run() {
                    fibers.ui.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                webapp.serveStaticContent("web");
                                webapp.start();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            });

            webapp.webServer();

            // Index presenter
            {
                TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("/", "index", fibers.ui, webapp, websocket);
                websocketsForLogging.put("index", websocket);
                final IndexPresenter indexPresenter = new IndexPresenter();
                fibers.ui.subscribe(indexPresenter, channels.displaySymbol, channels.refData, websocket);
            }

            // Ladder presenters
            final List<TypedChannel<WebSocketControlMessage>> websockets = newArrayList();
            {
                for (int i = 0; i < NUM_DISPLAY_THREADS; i++) {
                    final TypedChannel<WebSocketControlMessage> websocket = create(WebSocketControlMessage.class);
                    websockets.add(websocket);
                    final String name = "Ladder-" + (i);
                    FiberBuilder fiberBuilder = fibers.fiberGroup.create(name);
                    LadderPresenter presenter = new LadderPresenter(channels.remoteOrderCommand, environment.ladderOptions(), channels.stats, channels.storeLadderPref, channels.heartbeatRoundTrips, channels.reddalCommand, channels.subscribeToMarketData, channels.unsubscribeFromMarketData, channels.recenterLaddersForUser, fiberBuilder.getFiber());
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
                            channels.futuresContractSets);
                    fiberBuilder.getFiber().scheduleWithFixedDelay(presenter.flushBatchedData(), 10 + i * (BATCH_FLUSH_INTERVAL_MS / websockets.size()), BATCH_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    fiberBuilder.getFiber().scheduleWithFixedDelay(presenter.sendHeartbeats(), 10 + i * (HEARTBEAT_INTERVAL_MS / websockets.size()), HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
                }
            }

            // Ladder router
            {
                final TypedChannel<WebSocketControlMessage> ladderWebSocket = create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("ladder", "ladder", fibers.ladder, webapp, ladderWebSocket);
                LadderMessageRouter ladderMessageRouter = new LadderMessageRouter(websockets);
                fibers.ladder.subscribe(ladderMessageRouter, ladderWebSocket);
            }



        }


        // Non SSO-protected webapp to allow AJAX requests
        {

            final WebApplication webapp;
            webapp = new WebApplication(environment.getWebPort() + 1, channels.errorPublisher);

            fibers.onStart(new Runnable() {
                @Override
                public void run() {
                    fibers.ui.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                webapp.serveStaticContent("web");
                                webapp.start();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            });

            webapp.webServer();

            // Workspace
            {
                final TypedChannel<WebSocketControlMessage> workspaceSocket = create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("workspace", "workspace", fibers.ui, webapp, workspaceSocket);

                final LadderWorkspace ladderWorkspace = new LadderWorkspace();
                fibers.ui.subscribe(ladderWorkspace, workspaceSocket);
                webapp.addHandler("/open", new HttpHandler() {
                    @Override
                    public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control) throws Exception {
                        if (request.method().equals("GET")) {
                            String user = request.remoteAddress().toString().split(":")[0].substring(1);
                            String symbol = request.queryParam("symbol");
                            String content;

                            response.status(200);

                            if (symbol == null) {
                                content = "fail";
                            } else if (user == null || !ladderWorkspace.openLadderForUser(user, symbol)) {
                                content = webapp.getBaseUri().split(":")[0] + environment.getWebPort() + "/ladder#" + symbol;
                            } else {
                                content = "success";
                            }

                            String reply;

                            if(request.queryParamKeys().contains("callback")) {
                                reply = request.queryParam("callback") + "(\"" + content + "\")";
                            } else {
                                reply = content;
                            }

                            response.content(reply);
                            response.end();
                        }
                    }
                });
            }

        }

        // Settings
        {
            final LadderSettings ladderSettings = new LadderSettings(environment.getSettingsFile(), channels.ladderPrefsLoaded);
            fibers.settings.subscribe(ladderSettings, channels.storeLadderPref);
            fibers.onStart(new Runnable() {
                @Override
                public void run() {
                    fibers.settings.execute(ladderSettings.loadRunnable());
                }
            });
        }

        // Futures contracts
        {
            FuturesContractSetGenerator futuresContractSetGenerator = new FuturesContractSetGenerator(channels.futuresContractSets);
            fibers.contracts.subscribe(futuresContractSetGenerator, channels.refData);
        }

        // Market data
        {

            for (String mds : environment.getList(Environment.MARKET_DATA)) {

                final MarketDataEventSnapshottingPublisher snapshottingPublisher = new MarketDataEventSnapshottingPublisher();
                final FiberBuilder fiber = fibers.fiberGroup.create("Market Data: " + mds);
                final ProductResetter productResetter = new ProductResetter(snapshottingPublisher);
                final Publisher<MarketDataEvent> marketDataEventPublisher = new Publisher<MarketDataEvent>() {
                    Map<String, InstrumentDefinitionEvent> refData = new HashMap<>();

                    @Override
                    public void publish(MarketDataEvent msg) {
                        if (msg instanceof ServerHeartbeat) {
                            return;
                        }
                        if (msg instanceof InstrumentDefinitionEvent) {
                            channels.refData.publish((InstrumentDefinitionEvent) msg);
                            productResetter.on((InstrumentDefinitionEvent) msg);
                            refData.put(((InstrumentDefinitionEvent) msg).getSymbol(), (InstrumentDefinitionEvent) msg);
                        }
                        if (msg instanceof BookSnapshot) {
                            BookSnapshot snapshot = (BookSnapshot) msg;
                            InstrumentDefinitionEvent instrumentDefinitionEvent = refData.get(snapshot.getSymbol());
                            if (instrumentDefinitionEvent != null && instrumentDefinitionEvent.getExchange().equals("Eurex")) {
                                BookSnapshot reconstructedSnapshot = new BookSnapshot(snapshot.getSymbol(), PriceType.RECONSTRUCTED, snapshot.getSide(), snapshot.getLevels(), snapshot.getSeqNo(), snapshot.getMillis(), snapshot.getNanos(), snapshot.getCorrelationValue());
                                snapshottingPublisher.publish(reconstructedSnapshot);
                            } else {
                                snapshottingPublisher.publish(snapshot);
                            }
                        } else {
                            snapshottingPublisher.publish(msg);
                        }
                    }
                };
                MarketDataSubscriber marketDataSubscriber = new MarketDataSubscriber(snapshottingPublisher);
                fiber.subscribe(marketDataSubscriber, channels.subscribeToMarketData, channels.unsubscribeFromMarketData);

                if (environment.getMarketDataExchange(mds) == Environment.Exchange.EUREX) {

                    final TotalTradedVolumeAccumulator totalTradedVolumeAccumulator = new TotalTradedVolumeAccumulator(marketDataEventPublisher);
                    KeyedPublisher<TraderMarket, MarketDataEvent> keyedPublisher = new KeyedPublisher<TraderMarket, MarketDataEvent>() {
                        @Override
                        public void publish(final TraderMarket key, final MarketDataEvent value) {
                            totalTradedVolumeAccumulator.publish(value);
                        }
                    };

                    final EasyEurexNaMDS easyEurexNaMDS = new EasyEurexNaMDS(
                            environment.getMarkets(mds),
                            keyedPublisher, keyedPublisher, keyedPublisher,
                            statsPublisher,
                            channels.error,
                            StreamEnv.prod,
                            environment.getEurexInterface(mds),
                            new StatsPublisherErrorMonitor(statsPublisher),
                            logDir
                    ).withImpliedTopOfBooks(keyedPublisher, true);

                    fibers.onStart(new Runnable() {
                        @Override
                        public void run() {
                            fiber.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        easyEurexNaMDS.start();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                        }
                    });

                } else {
                    final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.MARKET_DATA, mds);
                    final OnHeapBufferPhotocolsNioClient<MarketDataEvent, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), MarketDataEvent.class, Void.class, fiber.getFiber(), EXCEPTION_HANDLER);
                    final ConnectionCloser connectionCloser = new ConnectionCloser(channels.stats, "Market data: " + mds, productResetter.resetRunnable());

                    client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                            .handler(new IdleConnectionTimeoutHandler(connectionCloser, SERVER_TIMEOUT, fiber.getFiber()))
                            .handler(new JetlangChannelHandler<MarketDataEvent, Void>(marketDataEventPublisher));

                    fibers.onStart(new Runnable() {
                        @Override
                        public void run() {
                            fiber.execute(new Runnable() {
                                @Override
                                public void run() {
                                    client.start();
                                }
                            });
                        }
                    });


                }
            }
        }

        // Meta data
        {
            for (String server : environment.getList(Environment.METADATA)) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.METADATA, server);
                final OnHeapBufferPhotocolsNioClient<LadderMetadata, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class, Void.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "metadata." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<LadderMetadata, Void>(channels.stats, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<LadderMetadata, Void>(channels.metaData));
                fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Remote commands
        {
            for (final String server : environment.getList(Environment.REMOTE_COMMANDS)) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.REMOTE_COMMANDS, server);
                final OnHeapBufferPhotocolsNioClient<RemoteOrderManagementEvent, RemoteOrderManagementCommand> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), RemoteOrderManagementEvent.class, RemoteOrderManagementCommand.class, fibers.workingOrders.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "remote-commands." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(channels.stats, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(new Publisher<RemoteOrderManagementEvent>() {
                            @Override
                            public void publish(RemoteOrderManagementEvent msg) {
                                channels.remoteOrderEvents.publish(new RemoteOrderEventFromServer(server, msg));
                            }
                        }, channels.remoteOrderCommandByServer.get(server), fibers.remoteOrders.getFiber()))
                        .handler(new InboundTimeoutWatchdog<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(fibers.remoteOrders.getFiber(), new ConnectionCloser(channels.stats, "Remote order: " + server), SERVER_TIMEOUT));
                fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Reddal server
        {
            final Photocols<ReddalMessage, ReddalMessage> commandServer = Photocols.server(new InetSocketAddress(environment.getCommandsPort()), ReddalMessage.class, ReddalMessage.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
            commandServer.logFile(new File(logDir, "photocols.commands.log"), fibers.logging.getFiber(), true);
            commandServer.endpoint().add(new JetlangChannelHandler<ReddalMessage, ReddalMessage>(channels.reddalCommandSymbolAvailable, channels.reddalCommand, fibers.metaData.getFiber()));
            fibers.onStart(new Runnable() {
                @Override
                public void run() {
                    try {
                        commandServer.start();
                    } catch (InterruptedException e) {
                        channels.errorPublisher.publish(e);
                    }
                }
            });
            fibers.metaData.getFiber().scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    commandServer.publish(new Heartbeat());
                }
            }, 1000, 1000, TimeUnit.MILLISECONDS);
        }

        // Working orders
        {
            for (final String server : environment.getList(Environment.WORKING_ORDERS)) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.WORKING_ORDERS, server);
                final OnHeapBufferPhotocolsNioClient<WorkingOrderEvent, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), WorkingOrderEvent.class, Void.class, fibers.workingOrders.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "working-orders." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<WorkingOrderEvent, Void>(channels.stats, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<WorkingOrderEvent, Void>(new Publisher<WorkingOrderEvent>() {
                            @Override
                            public void publish(WorkingOrderEvent msg) {
                                if (msg instanceof WorkingOrderUpdate) {
                                    channels.workingOrders.publish(new WorkingOrderUpdateFromServer(server, (WorkingOrderUpdate) msg));
                                }
                                channels.workingOrderEvents.publish(new WorkingOrderEventFromServer(server, msg));
                            }
                        }))
                        .handler(new InboundTimeoutWatchdog<WorkingOrderEvent, Void>(fibers.workingOrders.getFiber(), new ConnectionCloser(channels.stats, "Working order: " + server), SERVER_TIMEOUT));
                fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Working orders and remote commands watchdog
        {
            TradingStatusWatchdog watchdog = new TradingStatusWatchdog(channels.tradingStatus, SERVER_TIMEOUT, Clock.SYSTEM, channels.stats);
            fibers.watchdog.subscribe(watchdog, channels.workingOrderEvents, channels.remoteOrderEvents);
            fibers.watchdog.getFiber().scheduleWithFixedDelay(watchdog.checkRunnable(), HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        // Mr. Phil position
        {
            Environment.HostAndNic hostAndNic = environment.getMrPhilHostAndNic();
            final OnHeapBufferPhotocolsNioClient<Position, Subscription> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, hostAndNic.nic,
                    Position.class, Subscription.class, fibers.mrPhil.getFiber(), EXCEPTION_HANDLER);
            PhotocolsHandler<Position, Subscription> positionHandler = new PositionSubscriptionPhotocolsHandler(channels.position);
            fibers.mrPhil.subscribe(positionHandler, channels.refData);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                    .logFile(new File(logDir, "mr-phil.log"), fibers.logging.getFiber(), true)
                    .handler(positionHandler);
            fibers.onStart(new Runnable() {
                @Override
                public void run() {
                    client.start();
                }
            });
        }

        // Indy
        {
            if (environment.indyEnabled()) {
                Environment.HostAndNic hostAndNic = environment.getIndyHostAndNic();
                final OnHeapBufferPhotocolsNioClient<IndyEnvelope, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, hostAndNic.nic, IndyEnvelope.class, Void.class, fibers.mrPhil.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(RECONNECT_INTERVAL_MILLIS)
                        .logFile(new File(logDir, "indy.log"), fibers.logging.getFiber(), true)
                        .handler(new JetlangChannelHandler<IndyEnvelope, Void>(new Publisher<IndyEnvelope>() {
                            @Override
                            public void publish(IndyEnvelope msg) {
                                if (msg.getMessage() instanceof EquityIdAndSymbol) {
                                    channels.equityIdAndSymbol.publish((EquityIdAndSymbol) msg.getMessage());
                                }
                            }
                        }));
                fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Display symbols
        {
            fibers.indy.subscribe(new DisplaySymbolMapper(channels.displaySymbol), channels.refData, channels.equityIdAndSymbol);
        }

        // Desk Position
        {
            if (environment.opxlDeskPositionEnabled()) {
                for (String key : environment.opxlDeskPositionKeys()) {
                    final OpxlPositionSubscriber opxlPositionSubscriber = new OpxlPositionSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), channels.errorPublisher, key, new Publisher<DeskPosition>() {
                        @Override
                        public void publish(DeskPosition msg) {
                            channels.metaData.publish(msg);
                        }
                    });
                    fibers.onStart(new Runnable() {
                        @Override
                        public void run() {
                            fibers.opxlPosition.execute(opxlPositionSubscriber.connectToOpxl());
                        }
                    });
                }
            }
        }

        // Ladder Text
        {
            if (environment.opxlLadderTextEnabled()) {

                Collection<String> keys = environment.getOpxlLadderTextKeys();

                for (String key : keys) {
                    final OpxlLadderTextSubscriber opxlLadderTextSubscriber = new OpxlLadderTextSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), channels.errorPublisher, key, new Publisher<LadderText>() {
                        @Override
                        public void publish(LadderText msg) {
                            channels.metaData.publish(msg);
                        }
                    });
                    fibers.onStart(new Runnable() {
                        @Override
                        public void run() {
                            fibers.opxlText.execute(opxlLadderTextSubscriber.connectToOpxl());
                        }
                    });

                }

            }
        }

        // Error souting
        {
            channels.error.subscribe(fibers.logging.getFiber(), new Callback<Throwable>() {
                @Override
                public void onMessage(Throwable message) {
                    System.out.println(new Date().toString());
                    message.printStackTrace();
                }
            });
            channels.stats.subscribe(fibers.logging.getFiber(), new Callback<StatsMsg>() {
                @Override
                public void onMessage(StatsMsg message) {
                    if (message instanceof AdvisoryStat) {
                        System.out.println(new Date().toString());
                        System.out.println(message.toString());
                    }
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
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "contracts.json", channels.errorPublisher), channels.futuresContractSets);
            for (String name : websocketsForLogging.keySet()) {
                fibers.logging.subscribe(new JsonChannelLogger(logDir, "websocket" + name + ".json", channels.errorPublisher), websocketsForLogging.get(name));
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
