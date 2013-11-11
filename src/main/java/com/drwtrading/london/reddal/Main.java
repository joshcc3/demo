package com.drwtrading.london.reddal;

import com.drw.nns.api.MulticastGroup;
import com.drw.nns.api.NnsApi;
import com.drw.nns.api.NnsFactory;
import com.drwtrading.esquilatency.NanoClock;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.config.Config;
import com.drwtrading.london.jetlang.DefaultJetlangFactory;
import com.drwtrading.london.jetlang.FiberGroup;
import com.drwtrading.london.jetlang.JetlangFactory;
import com.drwtrading.london.jetlang.transport.LowTrafficMulticastTransport;
import com.drwtrading.london.logging.ErrorLogger;
import com.drwtrading.london.logging.JsonChannelLogger;
import com.drwtrading.london.monitoring.MonitoringHeartbeat;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.opxl.OpxlLadderTextSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlPositionSubscriber;
import com.drwtrading.london.reddal.util.PhotocolsStatsPublisher;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.StatsPublisher;
import com.drwtrading.monitoring.stats.Transport;
import com.drwtrading.photocols.easy.monitoring.IdleConnectionCloser;
import com.drwtrading.photocols.handlers.JetlangChannelHandler;
import com.drwtrading.photocols.nio.PhotocolsNioClient;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketControlMessage;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.drwtrading.jetlang.autosubscribe.TypedChannels.create;

public class Main {

    public static TypedChannel<WebSocketControlMessage> createWebPageWithWebSocket(String alias, String name, FiberBuilder fiber, WebApplication webapp) {
        webapp.alias("/" + alias, "/" + name + ".html");
        TypedChannel<WebSocketControlMessage> websocketChannel = create(WebSocketControlMessage.class);
        webapp.createWebSocket("/" + name + "/ws/", websocketChannel, fiber.getFiber());
        return websocketChannel;
    }

    public static class Channels {
        public static final TypedChannel<Throwable> error = create(Throwable.class);
        public static final TypedChannel<MarketDataEvent> fullBook = create(MarketDataEvent.class);
        public static final TypedChannel<LadderMetadata> metaData = create(LadderMetadata.class);
        public static final TypedChannel<WorkingOrderUpdateFromServer> workingOrders = create(WorkingOrderUpdateFromServer.class);
        public static final TypedChannel<RemoteOrderEventFromServer> remoteOrderEvents = create(RemoteOrderEventFromServer.class);
        public static final Publisher<RemoteOrderCommandToServer> remoteOrderCommand = new Publisher<RemoteOrderCommandToServer>() {
            @Override
            public void publish(RemoteOrderCommandToServer msg) {
                remoteOrderCommandByServer.get(msg.toServer).publish(msg.value);
            }
        };
        public static final Map<String, TypedChannel<RemoteOrderManagementCommand>> remoteOrderCommandByServer = new MapMaker().makeComputingMap(new Function<String, TypedChannel<RemoteOrderManagementCommand>>() {
            @Override
            public TypedChannel<RemoteOrderManagementCommand> apply(java.lang.String from) {
                return create(RemoteOrderManagementCommand.class);
            }
        });
    }

    public static class Fibers {

        private static final JetlangFactory jetlangFactory = new DefaultJetlangFactory(Channels.error);
        private static final FiberGroup fiberGroup = new FiberGroup(jetlangFactory, "Fibers", Channels.error);
        public static final Fiber starter = jetlangFactory.createFiber("Starter");
        public static final FiberBuilder logging = fiberGroup.create("Logging");
        public static final FiberBuilder ui = fiberGroup.create("UI");
        public static final FiberBuilder marketData = fiberGroup.create("Market data");
        public static final FiberBuilder metaData = fiberGroup.create("Metadata");
        public static final FiberBuilder workingOrders = fiberGroup.create("Working orders");
        public static final FiberBuilder remoteOrders = fiberGroup.create("Remote orders");
        public static final FiberBuilder ladder = fiberGroup.create("Ladder");
        public static final FiberBuilder opxlPosition = fiberGroup.create("OPXL Position");
        public static final FiberBuilder opxlText = fiberGroup.create("OPXL Text");

        // Ensure the starterFiber is last
        static {
            fiberGroup.wrap(starter, "Starter");
        }


        public static void onStart(Runnable runnable) {
            starter.execute(runnable);
        }

        public static void start() {
            fiberGroup.start();
        }
    }

    public static final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Channels.error.publish(e);
        }
    };

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

    public static class RemoteOrderCommandToServer extends Struct {
        public final String toServer;
        public final RemoteOrderManagementCommand value;

        public RemoteOrderCommandToServer(String toServer, RemoteOrderManagementCommand value) {
            this.toServer = toServer;
            this.value = value;
        }
    }


    public static class RemoteOrderEventFromServer extends Struct {
        public final String toServer;
        public final RemoteOrderManagementEvent value;

        public RemoteOrderEventFromServer(String toServer, RemoteOrderManagementEvent value) {
            this.toServer = toServer;
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

        // Monitoring
        MulticastGroup statsGroup;
        final StatsPublisher statsPublisher;
        {
            NnsApi nnsApi = new NnsFactory().create();
            statsGroup = nnsApi.multicastGroupFor(environment.getStatsNns());
            Transport statsTransport = new LowTrafficMulticastTransport(statsGroup.getAddress(), statsGroup.getPort(), environment.getStatsInterface());
            statsPublisher = new StatsPublisher(environment.getStatsName(), statsTransport);
            statsPublisher.start();
        }

        // Dashboard / monitoring heartbeat
        {
            MonitoringHeartbeat heartbeat = new MonitoringHeartbeat(statsPublisher);
            Fibers.ui.subscribe(heartbeat, Channels.error);
            heartbeat.start(Fibers.ui.getFiber());
        }

        // WebApp
        {
            final FiberBuilder fiber = Fibers.ui;
            final WebApplication webapp;
            webapp = new WebApplication(environment.getWebPort(), Channels.error);
            webapp.enableSingleSignOn();

            Fibers.onStart(new Runnable() {
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

            // Ladder presenter
            {
                final TypedChannel<WebSocketControlMessage> websocket = createWebPageWithWebSocket("ladder", "ladder", fiber, webapp);
                LadderPresenter presenter = new LadderPresenter(Channels.remoteOrderCommand, environment.ladderOptions());
                Channels.fullBook.subscribe(Fibers.ladder.getFiber(), new BatchSubscriber<MarketDataEvent>(Fibers.ladder.getFiber(), presenter.onMarketData(), 0, TimeUnit.MILLISECONDS));
                Fibers.ladder.subscribe(presenter, websocket, Channels.workingOrders, Channels.metaData);
                Fibers.ladder.getFiber().scheduleWithFixedDelay(presenter.flushBatchedData(), 100, 100, TimeUnit.MILLISECONDS);
            }

        }

        // Market data
        {
            for (String mds : environment.getList(Environment.MARKET_DATA)) {
                final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.MARKET_DATA,mds);
                final PhotocolsNioClient<MarketDataEvent, Void> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), MarketDataEvent.class, Void.class, Fibers.marketData.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(3000).inboundTimeoutMillis(new IdleConnectionCloser(environment.getStatsPublisher()), 2000).handler(new JetlangChannelHandler<MarketDataEvent, Void>(Channels.fullBook));
                Fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Meta data
        {
            for (String server : environment.getList(Environment.METADATA)) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.METADATA, server);
                final PhotocolsNioClient<LadderMetadata, Void> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class, Void.class, Fibers.metaData.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
                        .logFile(new File(logDir, "metadata." + server + ".log"), Fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<LadderMetadata, Void>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<LadderMetadata, Void>(Channels.metaData));
                Fibers.onStart(new Runnable() {
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
                final PhotocolsNioClient<RemoteOrderManagementEvent, RemoteOrderManagementCommand> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), RemoteOrderManagementEvent.class, RemoteOrderManagementCommand.class, Fibers.workingOrders.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
                        .logFile(new File(logDir, "remote-commands." + server + ".log"), Fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(new Publisher<RemoteOrderManagementEvent>() {
                            @Override
                            public void publish(RemoteOrderManagementEvent msg) {
                                Channels.remoteOrderEvents.publish(new RemoteOrderEventFromServer(server, msg));
                            }
                        }, Channels.remoteOrderCommandByServer.get(server), Fibers.remoteOrders.getFiber()));
                Fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Working orders
        {
            for (final String server : environment.getList(Environment.WORKING_ORDERS)) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.WORKING_ORDERS, server);
                final PhotocolsNioClient<WorkingOrderEvent, Void> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), WorkingOrderEvent.class, Void.class, Fibers.workingOrders.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
                        .logFile(new File(logDir, "working-orders." + server + ".log"), Fibers.logging.getFiber(), true)
                        .handler(new PhotocolsStatsPublisher<WorkingOrderEvent, Void>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<WorkingOrderEvent, Void>(new Publisher<WorkingOrderEvent>() {
                            @Override
                            public void publish(WorkingOrderEvent msg) {
                                if(msg instanceof WorkingOrderUpdate) {
                                    Channels.workingOrders.publish(new WorkingOrderUpdateFromServer(server, (WorkingOrderUpdate) msg));
                                }
                            }
                        }));
                Fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
                    }
                });
            }
        }

        // Position
        {
            if (environment.opxlDeskPositionEnabled()) {
                final OpxlPositionSubscriber opxlPositionSubscriber = new OpxlPositionSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), Channels.error, config.get("opxl.deskposition.key"), new Publisher<DeskPosition>() {
                    @Override
                    public void publish(DeskPosition msg) {
                        Channels.metaData.publish(msg);
                    }
                });
                Fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        Fibers.opxlPosition.execute(opxlPositionSubscriber.connectToOpxl());
                    }
                });
            }
        }

        // Ladder Text
        {
            if (environment.opxlLadderTextEnabled()) {
                final OpxlLadderTextSubscriber opxlLadderTextSubscriber = new OpxlLadderTextSubscriber(config.get("opxl.host"), config.getInt("opxl.port"), Channels.error, config.get("opxl.laddertext.key"), new Publisher<LadderText>() {
                    @Override
                    public void publish(LadderText msg) {
                        Channels.metaData.publish(msg);
                    }
                });
                Fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        Fibers.opxlText.execute(opxlLadderTextSubscriber.connectToOpxl());
                    }
                });
            }
        }

        // Error souting
        {
            Channels.error.subscribe(Fibers.logging.getFiber(), new Callback<Throwable>() {
                @Override
                public void onMessage(Throwable message) {
                    message.printStackTrace();
                }
            });
        }

        // Logging
        {
            Fibers.logging.subscribe(new ErrorLogger(new File(logDir, "errors.log")).onThrowableCallback(), Channels.error);
            Fibers.logging.subscribe(new JsonChannelLogger(logDir, "metadata.json", Channels.error), Channels.metaData);
            Fibers.logging.subscribe(new Object() {
                @Subscribe
                public void on(Throwable throwable) {
                    System.out.println("ERROR: " + NanoClock.formatNanoTime(NanoClock.currentTimeNanos()));
                    throwable.printStackTrace();
                }

            }, Channels.error);
        }

        Fibers.start();
        new CountDownLatch(1).await();
    }


    public static <K, T> Map<K, TypedChannel<T>> channelMap(final Class<T> clazz) {
        return new MapMaker().makeComputingMap(new Function<K, TypedChannel<T>>() {
            @Override
            public TypedChannel<T> apply(K from) {
                return TypedChannels.create(clazz);
            }
        });
    }

}
