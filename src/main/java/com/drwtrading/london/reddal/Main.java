package com.drwtrading.london.reddal;

import com.drw.nns.api.MulticastGroup;
import com.drw.nns.api.NnsApi;
import com.drw.nns.api.NnsFactory;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.IndexPresenter;
import com.drwtrading.london.config.Config;
import com.drwtrading.london.eeif.photocols.client.OnHeapBufferPhotocolsNioClient;
import com.drwtrading.london.jetlang.ChannelFactory;
import com.drwtrading.london.jetlang.FiberGroup;
import com.drwtrading.london.jetlang.JetlangFactory;
import com.drwtrading.london.jetlang.stats.MonitoredJetlangFactory;
import com.drwtrading.london.jetlang.transport.LowTrafficMulticastTransport;
import com.drwtrading.london.logging.ErrorLogger;
import com.drwtrading.london.logging.JsonChannelLogger;
import com.drwtrading.london.monitoring.MonitoringHeartbeat;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.photons.indy.EquityIdAndSymbol;
import com.drwtrading.london.photons.indy.IndyEnvelope;
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
import com.drwtrading.london.reddal.symbols.EquityIdToDisplaySymbolMapper;
import com.drwtrading.london.reddal.util.ConnectionCloser;
import com.drwtrading.london.reddal.util.IdleConnectionTimeoutHandler;
import com.drwtrading.london.reddal.util.PhotocolsStatsPublisher;
import com.drwtrading.london.time.Clock;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.StatsPublisher;
import com.drwtrading.monitoring.stats.Transport;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.monitoring.transport.LoggingTransport;
import com.drwtrading.monitoring.transport.NullTransport;
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
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;
import org.webbitserver.handler.logging.LoggingHandler;
import org.webbitserver.handler.logging.SimpleLogSink;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.drwtrading.jetlang.autosubscribe.TypedChannels.create;

public class Main {

    public static final long SERVER_TIMEOUT = 3000L;
    public static final int BATCH_FLUSH_INTERVAL_MS = 110;
    public static final int HEARTBEAT_INTERVAL_MS = 20 * BATCH_FLUSH_INTERVAL_MS;
    public static final int NUM_DISPLAY_THREADS = 16;

    public static TypedChannel<WebSocketControlMessage> createWebPageWithWebSocket(String alias, String name, FiberBuilder fiber, WebApplication webapp, final TypedChannel<WebSocketControlMessage> websocketChannel) {
        webapp.alias("/" + alias, "/" + name + ".html");
        webapp.createWebSocket("/" + name + "/ws/", websocketChannel, fiber.getFiber());
        return websocketChannel;
    }

    public static final TypedChannel<Throwable> ERROR_CHANNEL = create(Throwable.class);

    public static class ReddalChannels {

        public final TypedChannel<Throwable> error;
        public final TypedChannel<MarketDataEvent> fullBook;
        public final TypedChannel<LadderMetadata> metaData;
        public final TypedChannel<Position> position;
        public final TypedChannel<TradingStatusWatchdog.ServerTradingStatus> tradingStatus;
        public final TypedChannel<WorkingOrderUpdateFromServer> workingOrders;
        public final TypedChannel<WorkingOrderEventFromServer> workingOrderEvents;
        public final TypedChannel<RemoteOrderEventFromServer> remoteOrderEvents;
        public final TypedChannel<StatsMsg> status;
        public final TypedChannel<InstrumentDefinitionEvent> refData;
        public final Publisher<RemoteOrderCommandToServer> remoteOrderCommand;
        public final Map<String, TypedChannel<RemoteOrderManagementCommand>> remoteOrderCommandByServer;
        public final TypedChannel<LadderSettings.LadderPrefLoaded> ladderPrefsLoaded;
        public final TypedChannel<LadderSettings.StoreLadderPref> storeLadderPref;
        public final TypedChannel<EquityIdAndSymbol> equityIdAndSymbol;
        public final TypedChannel<DisplaySymbol> displaySymbol;
        public final TypedChannel<LadderView.HeartbeatRoundtrip> heartbeatRoundTrips;
        private final ChannelFactory channelFactory;
        public final TypedChannel<WebSocketControlMessage> websocket;
        public final TypedChannel<ReddalMessage> reddalCommand;
        public final TypedChannel<ReddalMessage> reddalCommandSymbolAvailable;

        public ReddalChannels(ChannelFactory channelFactory) {
            this.channelFactory = channelFactory;
            error = ERROR_CHANNEL;
            fullBook = create(MarketDataEvent.class);
            metaData = create(LadderMetadata.class);
            position = create(Position.class);
            tradingStatus = create(TradingStatusWatchdog.ServerTradingStatus.class);
            workingOrders = create(WorkingOrderUpdateFromServer.class);
            workingOrderEvents = create(WorkingOrderEventFromServer.class);
            remoteOrderEvents = create(RemoteOrderEventFromServer.class);
            status = create(StatsMsg.class);
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
            websocket = create(WebSocketControlMessage.class);
            reddalCommand = create(ReddalMessage.class);
            reddalCommandSymbolAvailable = create(ReddalMessage.class);
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
        public final FiberBuilder marketData;
        public final FiberBuilder metaData;
        public final FiberBuilder workingOrders;
        public final FiberBuilder selecta;
        public final FiberBuilder remoteOrders;
        public final FiberBuilder ladder;
        public final FiberBuilder opxlPosition;
        public final FiberBuilder opxlText;
        public final FiberBuilder mrPhil;
        public final FiberBuilder indy;
        public final FiberBuilder watchdog;
        public final FiberBuilder settings;

        public ReddalFibers(ReddalChannels channels, final MonitoredJetlangFactory factory) throws IOException {
            jetlangFactory = factory;
            fiberGroup = new FiberGroup(jetlangFactory, "Fibers", channels.error);
            starter = jetlangFactory.createFiber("Starter");
            fiberGroup.wrap(starter, "Starter");
            logging = fiberGroup.create("Logging");
            ui = fiberGroup.create("UI");
            marketData = fiberGroup.create("Market data");
            metaData = fiberGroup.create("Metadata");
            workingOrders = fiberGroup.create("Working orders");
            selecta = fiberGroup.create("Selecta");
            remoteOrders = fiberGroup.create("Remote orders");
            ladder = fiberGroup.create("Ladder");
            opxlPosition = fiberGroup.create("OPXL Position");
            opxlText = fiberGroup.create("OPXL Text");
            mrPhil = fiberGroup.create("Mr Phil");
            indy = fiberGroup.create("Indy");
            watchdog = fiberGroup.create("Watchdog");
            settings = fiberGroup.create("Settings");
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
        final LoggingTransport transport = new LoggingTransport(new File(logDir, "jetlang.log"));
        final MonitoredJetlangFactory monitoredJetlangFactory = new MonitoredJetlangFactory(new StatsPublisher("Reddal Monitoring", transport), ERROR_CHANNEL);
        final ReddalChannels channels = new ReddalChannels(monitoredJetlangFactory);
        final ReddalFibers fibers = new ReddalFibers(channels, monitoredJetlangFactory);
        fibers.onStart(new Runnable() {
            @Override
            public void run() {
                transport.start();
            }
        });

        final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                channels.error.publish(e);
            }
        };

        // Monitoring
        MulticastGroup statsGroup;
        final StatsPublisher statsPublisher;
        {
            NnsApi nnsApi = new NnsFactory().create();
            statsGroup = nnsApi.multicastGroupFor(environment.getStatsNns());
            Transport statsTransport = new LowTrafficMulticastTransport(statsGroup.getAddress(), statsGroup.getPort(), environment.getStatsInterface());
            final StatsPublisher localStatusPublisher = new StatsPublisher(environment.getStatsName(), statsTransport);
            localStatusPublisher.start();
            fibers.ui.subscribe(new Callback<StatsMsg>() {
                @Override
                public void onMessage(StatsMsg message) {
                    localStatusPublisher.publish(message);
                }
            }, channels.status);
            statsPublisher = new StatsPublisher(configName, new NullTransport()) {
                public void publish(StatsMsg statsMsg) {
                    channels.status.publish(statsMsg);
                }
            };
        }

        // Dashboard / monitoring heartbeat
        {
            MonitoringHeartbeat heartbeat = new MonitoringHeartbeat(statsPublisher);
            fibers.ui.subscribe(heartbeat, channels.error);
            heartbeat.start(fibers.ui.getFiber());
        }

        // WebApp
        {
            final FiberBuilder fiber = fibers.ui;
            final WebApplication webapp;
            webapp = new WebApplication(environment.getWebPort(), channels.error);
            webapp.enableSingleSignOn();

            fibers.onStart(new Runnable() {
                @Override
                public void run() {
                    fiber.execute(new Runnable() {
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

            webapp.webServer().add(new LoggingHandler(new SimpleLogSink(new FileWriter(new File(logDir, "web.log"), true))));

            // Websocket signup broker
            final ArrayList<TypedChannel<WebSocketControlMessage>> websockets;
            {
                websockets = new ArrayList<TypedChannel<WebSocketControlMessage>>();
                for (int i = 0; i < NUM_DISPLAY_THREADS; i++) {
                    websockets.add(TypedChannels.create(WebSocketControlMessage.class));
                }
                final Map<WebSocketClient, TypedChannel<WebSocketControlMessage>> map = new MapMaker().makeComputingMap(new Function<WebSocketClient, TypedChannel<WebSocketControlMessage>>() {
                    int num = 0;

                    @Override
                    public TypedChannel<WebSocketControlMessage> apply(WebSocketClient from) {
                        System.out.println("Ladder #" + num + " for " + from.toString());
                        return websockets.get(num++ % websockets.size());
                    }
                });
                final TypedChannel<WebSocketControlMessage> websocket = createWebPageWithWebSocket("ladder", "ladder", fiber, webapp, channels.websocket);
                fibers.ladder.subscribe(new Callback<WebSocketControlMessage>() {
                    @Override
                    public void onMessage(WebSocketControlMessage message) {
                        map.get(message.getClient()).publish(message);
                        if (message instanceof WebSocketDisconnected) {
                            map.remove(message.getClient());
                        }
                    }
                }, websocket);
            }

            // Ladder presenters
            int num = 0;
            for (TypedChannel<WebSocketControlMessage> websocket : websockets) {
                num++;
                FiberBuilder fiberBuilder = fibers.fiberGroup.create("Ladder-" + (num));
                LadderPresenter presenter = new LadderPresenter(channels.remoteOrderCommand, environment.ladderOptions(), channels.status, channels.storeLadderPref, channels.heartbeatRoundTrips, channels.reddalCommand, fiber.getFiber());
                channels.fullBook.subscribe(fiberBuilder.getFiber(), new BatchSubscriber<MarketDataEvent>(fiberBuilder.getFiber(), presenter.onMarketData(), 5, TimeUnit.MILLISECONDS));
                fiberBuilder.subscribe(presenter,
                        websocket,
                        channels.workingOrders,
                        channels.metaData,
                        channels.position,
                        channels.tradingStatus,
                        channels.ladderPrefsLoaded,
                        channels.displaySymbol,
                        channels.reddalCommandSymbolAvailable);
                fiberBuilder.getFiber().scheduleWithFixedDelay(presenter.flushBatchedData(), 10 + num * (BATCH_FLUSH_INTERVAL_MS / websockets.size()), BATCH_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                fiberBuilder.getFiber().scheduleWithFixedDelay(presenter.sendHeartbeats(), 10 + num * (HEARTBEAT_INTERVAL_MS / websockets.size()), HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }

            // Index presenter
            {
                TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("/", "index", fiber, webapp, websocket);
                IndexPresenter indexPresenter = new IndexPresenter();
                fiber.subscribe(indexPresenter, websocket, channels.refData, channels.displaySymbol);
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

        // Market data
        {
            for (String mds : environment.getList(Environment.MARKET_DATA)) {
                final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.MARKET_DATA, mds);
                final OnHeapBufferPhotocolsNioClient<MarketDataEvent, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), MarketDataEvent.class, Void.class, fibers.marketData.getFiber(), EXCEPTION_HANDLER);
                final ProductResetter productResetter = new ProductResetter(channels.fullBook);
                final ConnectionCloser connectionCloser = new ConnectionCloser(channels.status, "Market data: " + mds, productResetter.resetRunnable());
                client.reconnectMillis(3000)
                        .handler(new IdleConnectionTimeoutHandler(connectionCloser, SERVER_TIMEOUT, fibers.marketData.getFiber()))
                        .handler(new JetlangChannelHandler<MarketDataEvent, Void>(new Publisher<MarketDataEvent>() {
                            @Override
                            public void publish(MarketDataEvent msg) {
                                channels.fullBook.publish(msg);
                                if (msg instanceof InstrumentDefinitionEvent) {
                                    channels.refData.publish((InstrumentDefinitionEvent) msg);
                                    productResetter.on((InstrumentDefinitionEvent) msg);
                                }
                            }
                        }));
                fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        fibers.marketData.execute(new Runnable() {
                            @Override
                            public void run() {
                                client.start();
                            }
                        });
                    }
                });
            }
        }

        // Meta data
        {
            for (String server : environment.getList(Environment.METADATA)) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.METADATA, server);
                final OnHeapBufferPhotocolsNioClient<LadderMetadata, Void> client = OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class, Void.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
                        .logFile(new File(logDir, "metadata." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<LadderMetadata, Void>(statsPublisher, environment.getStatsName(), 10))
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
                client.reconnectMillis(5000)
                        .logFile(new File(logDir, "remote-commands." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(new Publisher<RemoteOrderManagementEvent>() {
                            @Override
                            public void publish(RemoteOrderManagementEvent msg) {
                                channels.remoteOrderEvents.publish(new RemoteOrderEventFromServer(server, msg));
                            }
                        }, channels.remoteOrderCommandByServer.get(server), fibers.remoteOrders.getFiber()))
                        .handler(new InboundTimeoutWatchdog<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(fibers.remoteOrders.getFiber(), new ConnectionCloser(channels.status, "Remote order: " + server), SERVER_TIMEOUT));
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
                        channels.error.publish(e);
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
                client.reconnectMillis(5000)
                        .logFile(new File(logDir, "working-orders." + server + ".log"), fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<WorkingOrderEvent, Void>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<WorkingOrderEvent, Void>(new Publisher<WorkingOrderEvent>() {
                            @Override
                            public void publish(WorkingOrderEvent msg) {
                                if (msg instanceof WorkingOrderUpdate) {
                                    channels.workingOrders.publish(new WorkingOrderUpdateFromServer(server, (WorkingOrderUpdate) msg));
                                }
                                channels.workingOrderEvents.publish(new WorkingOrderEventFromServer(server, msg));
                            }
                        }))
                        .handler(new InboundTimeoutWatchdog<WorkingOrderEvent, Void>(fibers.workingOrders.getFiber(), new ConnectionCloser(channels.status, "Working order: " + server), SERVER_TIMEOUT));
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
            TradingStatusWatchdog watchdog = new TradingStatusWatchdog(channels.tradingStatus, SERVER_TIMEOUT, Clock.SYSTEM, statsPublisher);
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
            client.reconnectMillis(5000)
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
                client.reconnectMillis(5000)
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
            fibers.indy.subscribe(new EquityIdToDisplaySymbolMapper(channels.displaySymbol), channels.refData, channels.equityIdAndSymbol);
        }

        // Desk Position
        {
            if (environment.opxlDeskPositionEnabled()) {
                final OpxlPositionSubscriber opxlPositionSubscriber = new OpxlPositionSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), channels.error, config.get("opxl.deskposition.key"), new Publisher<DeskPosition>() {
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

        // Ladder Text
        {
            if (environment.opxlLadderTextEnabled()) {
                final OpxlLadderTextSubscriber opxlLadderTextSubscriber = new OpxlLadderTextSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), channels.error, config.get("opxl.laddertext.key"), new Publisher<LadderText>() {
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

        // Error souting
        {
            channels.error.subscribe(fibers.logging.getFiber(), new Callback<Throwable>() {
                @Override
                public void onMessage(Throwable message) {
                    System.out.println(new Date().toString());
                    message.printStackTrace();
                }
            });
            channels.status.subscribe(fibers.logging.getFiber(), new Callback<StatsMsg>() {
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
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "metadata.json", channels.error), channels.metaData);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "remote-order.json", channels.error), channels.workingOrderEvents, channels.remoteOrderEvents);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "trading-status.json", channels.error), channels.tradingStatus);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "position.json", channels.error), channels.position);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "preferences.json", channels.error), channels.ladderPrefsLoaded, channels.storeLadderPref);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "status.json", channels.error), channels.status);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "reference-data.json", channels.error), channels.refData);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "heartbeats.json", channels.error), channels.heartbeatRoundTrips);
            fibers.logging.subscribe(new JsonChannelLogger(logDir, "websocket.json", channels.error), channels.websocket);
        }

        fibers.start();
        new CountDownLatch(1).await();
    }


}
