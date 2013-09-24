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
import com.drwtrading.london.monitoring.MonitoringHeartbeat;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementEvent;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.util.KeyedPublisher;
import com.drwtrading.london.reddal.util.PhotocolsStatsPublisher;
import com.drwtrading.monitoring.stats.StatsPublisher;
import com.drwtrading.monitoring.stats.Transport;
import com.drwtrading.photocols.easy.monitoring.IdleConnectionCloser;
import com.drwtrading.photocols.handlers.JetlangChannelHandler;
import com.drwtrading.photocols.nio.PhotocolsNioClient;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;
import org.webbitserver.handler.logging.LoggingHandler;
import org.webbitserver.handler.logging.SimpleLogSink;

import java.io.File;
import java.io.FileWriter;
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
        public static final TypedChannel<WorkingOrderEvent> workingOrders = create(WorkingOrderEvent.class);
        public static final TypedChannel<RemoteOrderManagementEvent> remoteOrderEvents = create(RemoteOrderManagementEvent.class);
        public static final TypedChannel<LadderMetadata> metaData = create(LadderMetadata.class);
        public static final Map<String, TypedChannel<RemoteOrderManagementCommand>> remoteOrderCommandByServer = new MapMaker().makeComputingMap(new Function<String, TypedChannel<RemoteOrderManagementCommand>>() {
            @Override
            public TypedChannel<RemoteOrderManagementCommand> apply(java.lang.String from) {
                return create(RemoteOrderManagementCommand.class);
            }
        });
        public static final KeyedPublisher<String, RemoteOrderManagementCommand> remoteOrderCommandPublisher = new KeyedPublisher<String, RemoteOrderManagementCommand>() {
            @Override
            public void publish(String key, RemoteOrderManagementCommand value) {
                Channels.remoteOrderCommandByServer.get(key).publish(value);
            }
        };
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

    public static void main(String[] args) throws Exception {

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
                LadderPresenter presenter = new LadderPresenter(Channels.remoteOrderCommandPublisher, environment.ladderOptions());
                Channels.fullBook.subscribe(Fibers.ladder.getFiber(), new BatchSubscriber<MarketDataEvent>(Fibers.ladder.getFiber(), presenter.onMarketData(), 0, TimeUnit.MILLISECONDS));
                Fibers.ladder.subscribe(presenter, websocket, Channels.workingOrders, Channels.metaData);
                Fibers.ladder.getFiber().scheduleWithFixedDelay(presenter.flushBatchedData(), 100, 30, TimeUnit.MILLISECONDS);
            }

        }

        // Market data
        {
            for (String mds : environment.marketDataServers()) {
                final Environment.HostAndNic hostAndNic = environment.getHostAndNic(mds);
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
            for (String server : environment.metadataServers()) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(server);
                final PhotocolsNioClient<LadderMetadata, Void> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class, Void.class, Fibers.metaData.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
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
            for (String server : environment.remoteCommandServers()) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(server);
                final PhotocolsNioClient<RemoteOrderManagementEvent, RemoteOrderManagementCommand> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), RemoteOrderManagementEvent.class, RemoteOrderManagementCommand.class, Fibers.workingOrders.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
                        .handler(new PhotocolsStatsPublisher<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<RemoteOrderManagementEvent, RemoteOrderManagementCommand>(Channels.remoteOrderEvents, Channels.remoteOrderCommandByServer.get(server), Fibers.remoteOrders.getFiber()));
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
            for (String server : environment.workingOrderServers()) {
                Environment.HostAndNic hostAndNic = environment.getHostAndNic(server);
                final PhotocolsNioClient<WorkingOrderEvent, Void> client = PhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), WorkingOrderEvent.class, Void.class, Fibers.workingOrders.getFiber(), Main.EXCEPTION_HANDLER);
                client.reconnectMillis(5000)
                        .handler(new PhotocolsStatsPublisher<WorkingOrderEvent, Void>(statsPublisher, environment.getStatsName(), 10))
                        .handler(new JetlangChannelHandler<WorkingOrderEvent, Void>(Channels.workingOrders));
                Fibers.onStart(new Runnable() {
                    @Override
                    public void run() {
                        client.start();
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
