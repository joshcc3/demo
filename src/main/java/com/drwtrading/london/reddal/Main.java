package com.drwtrading.london.reddal;

import com.drw.nns.api.MulticastGroup;
import com.drw.nns.api.NnsApi;
import com.drw.nns.api.NnsFactory;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.photocols.client.OnHeapBufferPhotocolsNioClient;
import com.drwtrading.london.eeif.position.transport.PositionTransportComponents;
import com.drwtrading.london.eeif.position.transport.cache.PositionCacheFactory;
import com.drwtrading.london.eeif.position.transport.io.PositionClientHandler;
import com.drwtrading.london.eeif.stack.transport.StackTransportComponents;
import com.drwtrading.london.eeif.stack.transport.cache.StackCacheFactory;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.Application;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.config.ConfigParam;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.io.SelectIOComponents;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.MDTransportComponents;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClientFactory;
import com.drwtrading.london.eeif.utils.monitoring.ExpandedDetailResourceMonitor;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.monitoring.MultiLayeredResourceMonitor;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.eeif.utils.time.SystemClock;
import com.drwtrading.london.eeif.utils.transport.io.TransportTCPKeepAliveConnection;
import com.drwtrading.london.eeif.yoda.transport.YodaSignalType;
import com.drwtrading.london.eeif.yoda.transport.YodaTransportComponents;
import com.drwtrading.london.eeif.yoda.transport.cache.YodaClientCacheFactory;
import com.drwtrading.london.eeif.yoda.transport.cache.YodaNullClient;
import com.drwtrading.london.eeif.yoda.transport.io.YodaClientHandler;
import com.drwtrading.london.indy.transport.IndyTransportComponents;
import com.drwtrading.london.indy.transport.cache.IIndyCacheListener;
import com.drwtrading.london.indy.transport.cache.IndyCacheFactory;
import com.drwtrading.london.jetlang.FiberGroup;
import com.drwtrading.london.jetlang.JetlangFactory;
import com.drwtrading.london.jetlang.stats.MonitoredJetlangFactory;
import com.drwtrading.london.jetlang.transport.LowTrafficMulticastTransport;
import com.drwtrading.london.logging.ErrorLogger;
import com.drwtrading.london.logging.JsonChannelLogger;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommandMsg;
import com.drwtrading.london.photons.eeifoe.OrderEntryReplyMsg;
import com.drwtrading.london.photons.eeifoe.OrderUpdateEvent;
import com.drwtrading.london.photons.eeifoe.OrderUpdateEventMsg;
import com.drwtrading.london.photons.eeifoe.Update;
import com.drwtrading.london.photons.reddal.Heartbeat;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import eeif.execution.RemoteOrderManagementCommand;
import eeif.execution.RemoteOrderManagementEvent;
import eeif.execution.WorkingOrderEvent;
import eeif.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.autopull.AutoPullPersistence;
import com.drwtrading.london.reddal.autopull.AutoPuller;
import com.drwtrading.london.reddal.autopull.AutoPullerUI;
import com.drwtrading.london.reddal.autopull.PullerBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.DepthBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.LevelThreeBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.LevelTwoBookSubscriber;
import com.drwtrading.london.reddal.ladders.LadderMessageRouter;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.LadderSettings;
import com.drwtrading.london.reddal.ladders.LadderWorkspace;
import com.drwtrading.london.reddal.ladders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.WorkspaceRequestHandler;
import com.drwtrading.london.reddal.opxl.OpxlLadderTextSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlPositionSubscriber;
import com.drwtrading.london.reddal.orderentry.OrderEntryClient;
import com.drwtrading.london.reddal.orderentry.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderentry.ServerDisconnected;
import com.drwtrading.london.reddal.orderentry.UpdateFromServer;
import com.drwtrading.london.reddal.pks.PKSPositionClient;
import com.drwtrading.london.reddal.position.PositionSubscriptionPhotocolsHandler;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.stacks.StackCallbackBatcher;
import com.drwtrading.london.reddal.stacks.StackGroupCallbackBatcher;
import com.drwtrading.london.reddal.stacks.configui.StackConfigUIRouter;
import com.drwtrading.london.reddal.stacks.opxl.StackGroupOPXLView;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesNibblerView;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesUIRouter;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.stockAlerts.StockAlertPresenter;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaRestingOrderClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaSweepClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaTWAPClient;
import com.drwtrading.london.reddal.symbols.DisplaySymbolMapper;
import com.drwtrading.london.reddal.symbols.IndexUIPresenter;
import com.drwtrading.london.reddal.symbols.IndyClient;
import com.drwtrading.london.reddal.util.ConnectionCloser;
import com.drwtrading.london.reddal.util.FileLogger;
import com.drwtrading.london.reddal.util.PhotocolsStatsPublisher;
import com.drwtrading.london.reddal.util.ReconnectingOPXLClient;
import com.drwtrading.london.reddal.util.SelectIOFiber;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderConnectionEstablished;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderEventFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersPresenter;
import com.drwtrading.london.time.Clock;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.MsgCodec;
import com.drwtrading.monitoring.stats.StatsPublisher;
import com.drwtrading.monitoring.stats.Transport;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.monitoring.transport.LoggingTransport;
import com.drwtrading.monitoring.transport.MultiplexTransport;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.drwtrading.photocols.easy.Photocols;
import com.drwtrading.photocols.handlers.ConnectionAwareJetlangChannelHandler;
import com.drwtrading.photocols.handlers.InboundTimeoutWatchdog;
import com.drwtrading.photocols.handlers.JetlangChannelHandler;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.photons.mrphil.Subscription;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jndi.toolkit.url.Uri;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class Main {

    public static final int MD_SERVER_TIMEOUT = 5000;

    public static final long SERVER_TIMEOUT = 3000L;
    public static final long HEARTBEAT_INTERVAL_MS = 3000;
    public static final int NUM_DISPLAY_THREADS = 6;
    public static final long RECONNECT_INTERVAL_MILLIS = 10000;

    private static final String EWOK_BASE_URL_PARAM = "ewokBaseURL";
    private static final Pattern PROD_REPLACE = Pattern.compile("prod-", Pattern.LITERAL);

    public static void createWebPageWithWebSocket(final String alias, final String name, final FiberBuilder fiber,
            final WebApplication webapp, final TypedChannel<WebSocketControlMessage> websocketChannel) {
        webapp.alias('/' + alias, '/' + name + ".html");
        webapp.createWebSocket('/' + name + "/ws/", websocketChannel, fiber.getFiber());

    }

    public static final TypedChannel<Throwable> ERROR_CHANNEL = TypedChannels.create(Throwable.class);

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

    private static void start(final String[] args) throws Exception {

        final Application<ReddalComponents> app = new Application<>(args, "Reddal", ReddalComponents.class);

        final IClock clock = app.clock;
        final ConfigGroup root = app.config;
        final Path logDir = app.logDir;
        final IErrorLogger errorLog = app.errorLog;
        final IResourceMonitor<ReddalComponents> monitor = app.monitor;
        final SelectIO selectIO = app.selectIO;

        final Environment environment = new Environment(root);

        final NnsApi nnsApi = new NnsFactory().create();
        final LoggingTransport fileTransport = new LoggingTransport(logDir.resolve("jetlang.log").toFile());
        fileTransport.start();
        final MulticastGroup statsGroup = nnsApi.multicastGroupFor(environment.getStatsNns());
        final LowTrafficMulticastTransport lowTrafficMulticastTransport =
                new LowTrafficMulticastTransport(statsGroup.getAddress(), statsGroup.getPort(), environment.getStatsInterface());
        final AtomicBoolean multicastEnabled = new AtomicBoolean(false);
        final Transport enableableMulticastTransport = createEnableAbleTransport(lowTrafficMulticastTransport, multicastEnabled);
        final StatsPublisher statsPublisher =
                new StatsPublisher(environment.getStatsName(), new MultiplexTransport(fileTransport, enableableMulticastTransport));

        final MonitoredJetlangFactory monitoredJetlangFactory = new MonitoredJetlangFactory(statsPublisher, ERROR_CHANNEL);
        final ReddalChannels channels = new ReddalChannels(monitoredJetlangFactory);
        final ReddalFibers fibers = new ReddalFibers(channels, monitoredJetlangFactory);

        final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (t, e) -> channels.errorPublisher.publish(e);

        { // Monitoring
            fibers.stats.subscribe(statsPublisher::publish, channels.stats);
            fibers.stats.getFiber().schedule(() -> {
                statsPublisher.start();
                multicastEnabled.set(true);
            }, 10, TimeUnit.SECONDS);
        }

        final UILogger webLog = new UILogger(new SystemClock(), logDir);

        final Map<String, TypedChannel<WebSocketControlMessage>> websocketsForLogging = Maps.newHashMap();
        { // WebApp

            final WebApplication webapp = new WebApplication(environment.getWebPort(), channels.errorPublisher);
            System.out.println("http://localhost:" + environment.getWebPort());
            webapp.enableSingleSignOn();

            fibers.onStart(() -> fibers.ui.execute(() -> {
                try {
                    setupEntityAliases(environment, webapp);
                    webapp.serveStaticContent("web");
                    webapp.start();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            { // Index presenter
                final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("/", "index", fibers.ui, webapp, websocket);
                websocketsForLogging.put("index", websocket);
                final IndexUIPresenter indexPresenter = new IndexUIPresenter(webLog);
                fibers.ui.subscribe(indexPresenter, channels.displaySymbol, websocket);
                channels.searchResults.subscribe(fibers.ui.getFiber(), indexPresenter::addSearchResult);
            }

            { // Orders presenter
                final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("orders", "orders", fibers.ui, webapp, websocket);
                websocketsForLogging.put("orders", websocket);
                final OrdersPresenter ordersPresenter = new OrdersPresenter(webLog, channels.singleOrderCommand);
                fibers.ui.subscribe(ordersPresenter, websocket);
                channels.workingOrders.subscribe(
                        new BatchSubscriber<>(fibers.ui.getFiber(), ordersPresenter::onWorkingOrderBatch, 100, TimeUnit.MILLISECONDS));
            }

            { // Working orders screen
                final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("workingorders", "workingorders", fibers.ui, webapp, ws);

                final Collection<String> nibblers = environment.getList(Environment.WORKING_ORDERS);
                final WorkingOrdersPresenter presenter =
                        new WorkingOrdersPresenter(clock, monitor, webLog, fibers.ui.getFiber(), channels.stats,
                                channels.remoteOrderCommand, nibblers);
                fibers.ui.subscribe(presenter, ws);
                channels.searchResults.subscribe(fibers.ui.getFiber(), presenter::addSearchResult);
                channels.workingOrders.subscribe(fibers.ui.getFiber(), presenter::onWorkingOrder);
                channels.workingOrderConnectionEstablished.subscribe(fibers.ui.getFiber(), presenter::nibblerConnectionEstablished);
            }

            { // Stock alert screen
                final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("stockalerts", "stockalerts", fibers.ui, webapp, ws);

                final StockAlertPresenter presenter = new StockAlertPresenter(webLog);
                fibers.ui.subscribe(presenter, ws);
                channels.stockAlerts.subscribe(fibers.ui.getFiber(), presenter::addAlert);
            }

            // Ladder presenters
            final MultiLayeredResourceMonitor<ReddalComponents> parentMonitor =
                    new MultiLayeredResourceMonitor<>(monitor, ReddalComponents.class, errorLog);

            final ConfigGroup mdConfig = root.getEnabledGroup("md");
            final ConfigGroup stackConfig = root.getEnabledGroup("stacks");

            final List<TypedChannel<WebSocketControlMessage>> webSockets = Lists.newArrayList();
            for (int i = 0; i < NUM_DISPLAY_THREADS; i++) {

                final String name = "Ladder-" + i;
                final IResourceMonitor<ReddalComponents> displayMonitor = parentMonitor.createChildResourceMonitor(name);
                final IResourceMonitor<SelectIOComponents> selectIOMonitor =
                        new ExpandedDetailResourceMonitor<>(displayMonitor, name, errorLog, SelectIOComponents.class,
                                ReddalComponents.UI_SELECT_IO);

                final SelectIO displaySelectIO = new SelectIO(selectIOMonitor);

                final TypedChannel<WebSocketControlMessage> webSocket = TypedChannels.create(WebSocketControlMessage.class);
                webSockets.add(webSocket);
                final SelectIOFiber displaySelectIOFiber = new SelectIOFiber(displaySelectIO, errorLog, name);

                final boolean isPrimary = 0 == i;
                final LevelThreeBookSubscriber l3BookHandler =
                        new LevelThreeBookSubscriber(isPrimary, displayMonitor, channels.searchResults, channels.stockAlerts,
                                channels.stackRefPriceDetailChannel);
                final LevelTwoBookSubscriber l2BookHandler =
                        new LevelTwoBookSubscriber(isPrimary, displayMonitor, channels.searchResults, channels.stockAlerts,
                                channels.stackRefPriceDetailChannel);

                final MultiLayeredResourceMonitor<MDTransportComponents> mdParentMonitor =
                        MultiLayeredResourceMonitor.getMappedMultiLayerMonitor(displayMonitor, MDTransportComponents.class,
                                ReddalComponents.class, "MD_", errorLog);

                final String localAppName = app.env.name() + ':' + app.appName + '-' + i;

                int mdCount = 0;
                for (final ConfigGroup mdSourceGroup : mdConfig.groups()) {

                    final MDSource mdSource = MDSource.get(mdSourceGroup.getKey());
                    if (null == mdSource) {
                        throw new ConfigException("MDSource [" + mdSourceGroup.getKey() + "] is not known.");
                    }

                    final IResourceMonitor<MDTransportComponents> mdClientMonitor =
                            mdParentMonitor.createChildResourceMonitor(mdSource.name());

                    final MDTransportClient mdClient =
                            MDTransportClientFactory.createDepthClient(displaySelectIO, mdClientMonitor, mdSource, localAppName,
                                    l3BookHandler, l2BookHandler, MD_SERVER_TIMEOUT, true);
                    l3BookHandler.setMDClient(mdSource, mdClient);
                    l2BookHandler.setMDClient(mdSource, mdClient);

                    final TransportTCPKeepAliveConnection<?, ?> connection =
                            MDTransportClientFactory.createConnection(displaySelectIO, mdSourceGroup, mdClientMonitor, mdClient);

                    final long staggeringDelay = 250L * ++mdCount;
                    displaySelectIO.execute(() -> displaySelectIO.addDelayedAction(staggeringDelay, () -> {
                        connection.restart();
                        return -1;
                    }));
                }

                final DepthBookSubscriber depthBookSubscriber = new DepthBookSubscriber(l3BookHandler, l2BookHandler);
                final String ewokBaseURL = root.getString(EWOK_BASE_URL_PARAM);

                final LadderPresenter presenter =
                        new LadderPresenter(depthBookSubscriber, ewokBaseURL, channels.remoteOrderCommand, environment.ladderOptions(),
                                channels.stats, channels.storeLadderPref, channels.heartbeatRoundTrips, channels.reddalCommand,
                                channels.recenterLaddersForUser, displaySelectIOFiber, channels.trace, channels.ladderClickTradingIssues,
                                channels.userCycleContractPublisher, channels.orderEntryCommandToServer);

                if (null != stackConfig) {

                    final MultiLayeredResourceMonitor<StackTransportComponents> stackParentMonitor =
                            MultiLayeredResourceMonitor.getExpandedMultiLayerMonitor(displayMonitor, "Stacks", errorLog,
                                    StackTransportComponents.class, ReddalComponents.STACK_GROUP_CLIENT);

                    for (final ConfigGroup stackConnectionConfig : stackConfig.groups()) {

                        final String connectionName = localAppName + '-' + stackConnectionConfig.getKey();
                        final IResourceMonitor<StackTransportComponents> stackMonitor =
                                stackParentMonitor.createChildResourceMonitor(connectionName);

                        final StackGroupCallbackBatcher stackUpdateBatcher = new StackGroupCallbackBatcher(presenter);
                        final StackClientHandler clientHandler =
                                StackCacheFactory.createClientCache(displaySelectIO, stackConnectionConfig, stackMonitor, connectionName,
                                        localAppName, stackUpdateBatcher);

                        stackUpdateBatcher.setStackClient(clientHandler);
                    }
                }

                final FiberBuilder fiberBuilder = fibers.fiberGroup.wrap(displaySelectIOFiber, name);
                fiberBuilder.subscribe(presenter, webSocket, channels.workingOrders, channels.metaData, channels.position,
                        channels.tradingStatus, channels.ladderPrefsLoaded, channels.displaySymbol, channels.reddalCommandSymbolAvailable,
                        channels.recenterLaddersForUser, channels.contractSets, channels.chixSymbolPairs, channels.singleOrderCommand,
                        channels.ladderClickTradingIssues, channels.replaceCommand, channels.userCycleContractPublisher,
                        channels.orderEntrySymbols, channels.orderEntryFromServer, channels.searchResults);

                channels.pksExposure.subscribe(fiberBuilder.getFiber(), presenter::setPKSExposure);

                final long initialDelay = 10 + i * (LadderPresenter.BATCH_FLUSH_INTERVAL_MS / webSockets.size());
                displaySelectIO.addDelayedAction(initialDelay, presenter::flushAllLadders);
                displaySelectIO.addDelayedAction(initialDelay, presenter::sendAllHeartbeats);

            }

            // Auto-puller thread
            {
                final String name = "AutoPuller";
                final IResourceMonitor<ReddalComponents> displayMonitor = parentMonitor.createChildResourceMonitor(name);
                final IResourceMonitor<SelectIOComponents> selectIOMonitor =
                        new ExpandedDetailResourceMonitor<>(displayMonitor, name, errorLog, SelectIOComponents.class,
                                ReddalComponents.AUTO_PULLER_SELECT_IO);
                final SelectIO displaySelectIO = new SelectIO(selectIOMonitor);
                final MultiLayeredResourceMonitor<MDTransportComponents> mdParentMonitor =
                        MultiLayeredResourceMonitor.getMappedMultiLayerMonitor(displayMonitor, MDTransportComponents.class,
                                ReddalComponents.class, "MD_", errorLog);

                PullerBookSubscriber subscriber = new PullerBookSubscriber();

                for (final ConfigGroup mdSourceGroup : mdConfig.groups()) {

                    final MDSource mdSource = MDSource.get(mdSourceGroup.getKey());
                    if (null == mdSource) {
                        throw new ConfigException("MDSource [" + mdSourceGroup.getKey() + "] is not known.");
                    }

                    final IResourceMonitor<MDTransportComponents> mdClientMonitor =
                            mdParentMonitor.createChildResourceMonitor(mdSource.name());

                    final MDTransportClient mdClient =
                            MDTransportClientFactory.createDepthClient(displaySelectIO, mdClientMonitor, mdSource, app.appName + "-pull",
                                    subscriber.getL3(), subscriber.getL2(), MD_SERVER_TIMEOUT, true);

                    final TransportTCPKeepAliveConnection<?, ?> connection =
                            MDTransportClientFactory.createConnection(displaySelectIO, mdSourceGroup, mdClientMonitor, mdClient);
                    subscriber.setClient(mdSource, mdClient);
                    final long staggeringDelay = 300L * NUM_DISPLAY_THREADS;
                    displaySelectIO.execute(() -> displaySelectIO.addDelayedAction(staggeringDelay, () -> {
                        connection.restart();
                        return -1;
                    }));

                }

                final SelectIOFiber displaySelectIOFiber = new SelectIOFiber(displaySelectIO, errorLog, name);
                final FiberBuilder fiberBuilder = fibers.fiberGroup.wrap(displaySelectIOFiber, name);

                AutoPullPersistence persistence = new AutoPullPersistence(Paths.get("/site/drw/reddal/data/").resolve("autopull.json"));
                final AutoPuller puller = new AutoPuller(channels.remoteOrderCommand, subscriber, persistence);
                fiberBuilder.subscribe(puller, channels.workingOrders);
                fiberBuilder.getFiber().scheduleWithFixedDelay(puller::timeChecker, 1, 1, TimeUnit.MINUTES);
                AutoPullerUI autoPullerUI = new AutoPullerUI(puller);
                TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("autopuller", "autopuller", fiberBuilder, webapp, ws);
                fiberBuilder.subscribe(autoPullerUI, ws);
            }

            // Ladder router
            final TypedChannel<WebSocketControlMessage> ladderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("ladder", "ladder", fibers.ladder, webapp, ladderWebSocket);
            final LadderMessageRouter ladderMessageRouter = new LadderMessageRouter(webLog, webSockets, fibers.ui);
            fibers.ladder.subscribe(ladderMessageRouter, ladderWebSocket);

            if (null != stackConfig) {

                final IResourceMonitor<ReddalComponents> stackConfigMonitor = parentMonitor.createChildResourceMonitor("StackConfig");
                final MultiLayeredResourceMonitor<StackTransportComponents> stackParentMonitor =
                        MultiLayeredResourceMonitor.getExpandedMultiLayerMonitor(stackConfigMonitor, "Stacks", errorLog,
                                StackTransportComponents.class, ReddalComponents.STACK_GROUP_CLIENT);

                final IResourceMonitor<SelectIOComponents> selectIOMonitor =
                        new ExpandedDetailResourceMonitor<>(stackConfigMonitor, "Stack Config", errorLog, SelectIOComponents.class,
                                ReddalComponents.STACK_SELECT_IO);
                final SelectIO stackConfigSelectIO = new SelectIO(selectIOMonitor);

                final SelectIOFiber displaySelectIOFiber = new SelectIOFiber(stackConfigSelectIO, errorLog, "Stack Config SelectIO.");
                fibers.fiberGroup.wrap(displaySelectIOFiber, "Stack Config SelectIO Fiber.");

                final StackStrategiesUIRouter stackStrategiesUIRouter = new StackStrategiesUIRouter(fibers.ui, webLog);
                final StackConfigUIRouter stackConfigPresenter = new StackConfigUIRouter(fibers.ui, webLog);

                final String stackOPXLTopic = stackConfig.getString("opxlSpreadTopic");
                final StackGroupOPXLView stackOPXLView = new StackGroupOPXLView(stackConfigMonitor, stackOPXLTopic);
                channels.stackRefPriceDetailChannel.subscribe(displaySelectIOFiber, stackOPXLView::setStackRefPrice);
                stackConfigSelectIO.addDelayedAction(5000, stackOPXLView::update);

                for (final ConfigGroup stackConnectionConfig : stackConfig.groups()) {

                    final String nibblerName = stackConnectionConfig.getKey();
                    final String connectionName = app.appName + " config";
                    final IResourceMonitor<StackTransportComponents> stackMonitor =
                            stackParentMonitor.createChildResourceMonitor(connectionName);

                    final StackStrategiesNibblerView strategiesPresenter = stackStrategiesUIRouter.getNibblerHandler(nibblerName);

                    final StackCallbackBatcher stackUpdateBatcher =
                            new StackCallbackBatcher(nibblerName, strategiesPresenter, stackConfigPresenter, stackOPXLView,
                                    channels.contractSets);
                    final StackClientHandler clientHandler =
                            StackCacheFactory.createClientCache(stackConfigSelectIO, stackConnectionConfig, stackMonitor,
                                    nibblerName + " config", app.env.name() + connectionName, stackUpdateBatcher);

                    strategiesPresenter.setStrategyClient(clientHandler);
                    stackConfigPresenter.setConfigClient(nibblerName, clientHandler);
                }

                channels.searchResults.subscribe(displaySelectIOFiber,
                        searchResult -> stackStrategiesUIRouter.addInstID(searchResult.symbol, searchResult.instID));

                final TypedChannel<WebSocketControlMessage> strategiesWebSocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("stackStrategy", "stackStrategy", fibers.ladder, webapp, strategiesWebSocket);
                fibers.ladder.subscribe(stackStrategiesUIRouter, strategiesWebSocket);

                // Config router
                final TypedChannel<WebSocketControlMessage> configWebSocket = TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("stackConfig", "stackConfig", fibers.ladder, webapp, configWebSocket);
                fibers.ladder.subscribe(stackConfigPresenter, configWebSocket);
            }
        }

        // Non SSO-protected web App to allow AJAX requests
        {

            final WebApplication webapp = new WebApplication(environment.getWebPort() + 1, channels.errorPublisher);

            fibers.onStart(() -> fibers.ui.execute(() -> {
                try {
                    setupEntityAliases(environment, webapp);
                    webapp.serveStaticContent("web");
                    webapp.start();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            webapp.webServer();

            // Workspace
            final TypedChannel<WebSocketControlMessage> workspaceSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("workspace", "workspace", fibers.ui, webapp, workspaceSocket);

            final LadderWorkspace ladderWorkspace = new LadderWorkspace(webLog, channels.replaceCommand);
            fibers.ui.subscribe(ladderWorkspace, workspaceSocket, channels.contractSets);
            webapp.addHandler("/open",
                    new WorkspaceRequestHandler(ladderWorkspace, new Uri(webapp.getBaseUri()).getHost(), environment.getWebPort()));
        }

        // Settings
        {
            final LadderSettings ladderSettings = new LadderSettings(environment.getSettingsFile(), channels.ladderPrefsLoaded);
            fibers.settings.subscribe(ladderSettings, channels.storeLadderPref);
            fibers.onStart(() -> fibers.settings.execute(ladderSettings::load));
        }

        { // Contract sets
            final FuturesContractSetGenerator futuresContractSetGenerator = new FuturesContractSetGenerator(channels.contractSets);
            channels.searchResults.subscribe(fibers.contracts.getFiber(), futuresContractSetGenerator::setSearchResult);

            final ChixInstMatcher chixInstMatcher = new ChixInstMatcher(channels.chixSymbolPairs);
            channels.searchResults.subscribe(fibers.contracts.getFiber(), chixInstMatcher::setSearchResult);
        }

        // EEIF-OE
        {
            final Collection<String> oeList = environment.getList(Environment.EEIF_OE);
            for (final String server : oeList) {

                final ConfigGroup eeifOEGroup = root.getGroup("eeifoe");
                final String instanceName = eeifOEGroup.getString("instance");
                final OrderEntryClient client =
                        new OrderEntryClient(instanceName, new SystemClock(), server, fibers.remoteOrders.getFiber(),
                                channels.orderEntrySymbols, channels.ladderClickTradingIssues);
                final Environment.HostAndNic command = environment.getHostAndNic(Environment.EEIF_OE + "Command", server);
                final OnHeapBufferPhotocolsNioClient<OrderEntryReplyMsg, OrderEntryCommandMsg> cmdClient =
                        OnHeapBufferPhotocolsNioClient.client(command.host, command.nic, OrderEntryReplyMsg.class,
                                OrderEntryCommandMsg.class, fibers.remoteOrders.getFiber(), EXCEPTION_HANDLER);
                cmdClient.reconnectMillis(RECONNECT_INTERVAL_MILLIS).logFile(logDir.resolve("order-entry." + server + ".log").toFile(),
                        fibers.logging.getFiber(), true).handler(
                        new PhotocolsStatsPublisher<>(channels.stats, server + " OE Commands", 10)).handler(
                        new InboundTimeoutWatchdog<>(fibers.remoteOrders.getFiber(),
                                new ConnectionCloser(channels.stats, server + " OE Commands"), SERVER_TIMEOUT)).handler(client);
                fibers.remoteOrders.subscribe(client, channels.orderEntryCommandToServer);
                fibers.remoteOrders.execute(cmdClient::start);
                System.out.println("EEIF-OE: " + server + "\tCommand: " + command.host);

                final Environment.HostAndNic update = environment.getHostAndNic(Environment.EEIF_OE + "Update", server);
                final OnHeapBufferPhotocolsNioClient<OrderUpdateEventMsg, Void> updateClient =
                        OnHeapBufferPhotocolsNioClient.client(update.host, update.nic, OrderUpdateEventMsg.class, Void.class,
                                fibers.remoteOrders.getFiber(), EXCEPTION_HANDLER);
                updateClient.reconnectMillis(RECONNECT_INTERVAL_MILLIS).logFile(logDir.resolve("order-update." + server + ".log").toFile(),
                        fibers.logging.getFiber(), true).handler(
                        new PhotocolsStatsPublisher<>(channels.stats, server + " OE Updates", 10)).handler(
                        new InboundTimeoutWatchdog<>(fibers.remoteOrders.getFiber(),
                                new ConnectionCloser(channels.stats, server + " OE Updates"), SERVER_TIMEOUT)).handler(
                        new ConnectionAwareJetlangChannelHandler<Void, OrderEntryFromServer, OrderUpdateEventMsg, Void>(Constants::NO_OP,
                                channels.orderEntryFromServer, evt -> {
                            if (evt.getMsg().typeEnum() == OrderUpdateEvent.Type.UPDATE) {
                                final Update msg = (Update) evt.getMsg();
                                channels.orderEntryFromServer.publish(new UpdateFromServer(evt.getFromInstance(), msg));
                            }
                        }) {
                            @Override
                            protected Void connected(final PhotocolsConnection<Void> connection) {
                                return null;
                            }

                            @Override
                            protected OrderEntryFromServer disconnected(final PhotocolsConnection<Void> connection) {
                                return new ServerDisconnected(server);
                            }
                        });
                fibers.remoteOrders.execute(updateClient::start);
                System.out.println("EEIF-OE: " + server + "\tUpdate: " + update.host);
            }
        }

        // Meta data
        for (final String server : environment.getList(Environment.METADATA)) {
            final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.METADATA, server);
            final OnHeapBufferPhotocolsNioClient<LadderMetadata, Void> client =
                    OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class,
                            Void.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS);
            client.logFile(logDir.resolve("metadata." + server + ".log").toFile(), fibers.logging.getFiber(), true);
            client.handler(new PhotocolsStatsPublisher<>(channels.stats, environment.getStatsName(), 10));
            client.handler(new JetlangChannelHandler<>(channels.metaData));
            fibers.onStart(client::start);
        }

        // Remote commands
        for (final String server : environment.getList(Environment.REMOTE_COMMANDS)) {
            final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.REMOTE_COMMANDS, server);
            final OnHeapBufferPhotocolsNioClient<RemoteOrderManagementEvent, RemoteOrderManagementCommand> client =
                    OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic),
                            RemoteOrderManagementEvent.class, RemoteOrderManagementCommand.class, fibers.workingOrders.getFiber(),
                            EXCEPTION_HANDLER);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS).logFile(logDir.resolve("remote-commands." + server + ".log").toFile(),
                    fibers.logging.getFiber(), true).handler(
                    new PhotocolsStatsPublisher<>(channels.stats, environment.getStatsName(), 10)).handler(
                    new JetlangChannelHandler<>(msg -> channels.remoteOrderEvents.publish(new RemoteOrderEventFromServer(server, msg)),
                            channels.remoteOrderCommandByServer.get(server), fibers.remoteOrders.getFiber())).handler(
                    new InboundTimeoutWatchdog<>(fibers.remoteOrders.getFiber(),
                            new ConnectionCloser(channels.stats, "Remote order: " + server), SERVER_TIMEOUT));
            fibers.onStart(client::start);
        }

        // Reddal server
        {
            final Photocols<ReddalMessage, ReddalMessage> commandServer =
                    Photocols.server(new InetSocketAddress(environment.getCommandsPort()), ReddalMessage.class, ReddalMessage.class,
                            fibers.metaData.getFiber(), EXCEPTION_HANDLER);
            commandServer.logFile(logDir.resolve("photocols.commands.log").toFile(), fibers.logging.getFiber(), true);
            commandServer.endpoint().add(
                    new JetlangChannelHandler<>(channels.reddalCommandSymbolAvailable, channels.reddalCommand, fibers.metaData.getFiber()));
            fibers.onStart(() -> {
                try {
                    commandServer.start();
                } catch (final InterruptedException e) {
                    channels.errorPublisher.publish(e);
                }
            });
            fibers.metaData.getFiber().scheduleWithFixedDelay(() -> commandServer.publish(new Heartbeat()), 1000, 1000,
                    TimeUnit.MILLISECONDS);
        }

        // Working orders
        for (final String server : environment.getList(Environment.WORKING_ORDERS)) {
            final Environment.HostAndNic hostAndNic = environment.getHostAndNic(Environment.WORKING_ORDERS, server);
            final OnHeapBufferPhotocolsNioClient<WorkingOrderEvent, Void> client =
                    OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), WorkingOrderEvent.class,
                            Void.class, fibers.workingOrders.getFiber(), EXCEPTION_HANDLER);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS);
            client.logFile(logDir.resolve("working-orders." + server + ".log").toFile(), fibers.logging.getFiber(), true);
            client.handler(new PhotocolsStatsPublisher<>(channels.stats, environment.getStatsName(), 10));
            client.handler(new JetlangChannelHandler<>(msg -> {
                if (msg instanceof WorkingOrderUpdate) {
                    channels.workingOrders.publish(new WorkingOrderUpdateFromServer(server, (WorkingOrderUpdate) msg));
                }
                channels.workingOrderEvents.publish(new WorkingOrderEventFromServer(server, msg));
            }));
            client.handler(new InboundTimeoutWatchdog<>(fibers.workingOrders.getFiber(),
                    new ConnectionCloser(channels.stats, "Working order: " + server), SERVER_TIMEOUT));

            final WorkingOrderConnectionEstablished connectionEstablished = new WorkingOrderConnectionEstablished(server, true);
            final WorkingOrderConnectionEstablished connectionLost = new WorkingOrderConnectionEstablished(server, false);
            final PhotocolsHandler<WorkingOrderEvent, Void> workingOrderLostPublisher = new PhotocolsHandler<WorkingOrderEvent, Void>() {
                @Override
                public PhotocolsConnection<Void> onOpen(final PhotocolsConnection<Void> connection) {
                    channels.workingOrderConnectionEstablished.publish(connectionEstablished);
                    return connection;
                }

                @Override
                public void onConnectFailure() {
                    channels.workingOrderConnectionEstablished.publish(connectionLost);
                }

                @Override
                public void onClose(final PhotocolsConnection<Void> connection) {
                    channels.workingOrderConnectionEstablished.publish(connectionLost);
                }

                @Override
                public void onMessage(final PhotocolsConnection<Void> connection, final WorkingOrderEvent message) {
                    // no-op
                }
            };
            client.handler(workingOrderLostPublisher);

            fibers.onStart(client::start);
        }

        // Working orders and remote commands watchdog
        {
            final TradingStatusWatchdog watchdog =
                    new TradingStatusWatchdog(channels.tradingStatus, SERVER_TIMEOUT, Clock.SYSTEM, channels.stats);
            fibers.watchdog.subscribe(watchdog, channels.workingOrderEvents, channels.remoteOrderEvents);
            fibers.watchdog.getFiber().scheduleWithFixedDelay(watchdog::checkHeartbeats, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS,
                    TimeUnit.MILLISECONDS);
        }

        // Mr. Phil position
        {
            final Environment.HostAndNic hostAndNic = environment.getMrPhilHostAndNic();
            final OnHeapBufferPhotocolsNioClient<Position, Subscription> client =
                    OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, hostAndNic.nic, Position.class, Subscription.class,
                            fibers.mrPhil.getFiber(), EXCEPTION_HANDLER);
            final PositionSubscriptionPhotocolsHandler positionHandler = new PositionSubscriptionPhotocolsHandler(channels.position);
            channels.searchResults.subscribe(fibers.mrPhil.getFiber(), positionHandler::setSearchResult);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS).logFile(logDir.resolve("mr-phil.log").toFile(), fibers.logging.getFiber(),
                    true).handler(positionHandler);
            fibers.onStart(client::start);
        }

        // Display symbols
        {
            final DisplaySymbolMapper displaySymbolMapper = new DisplaySymbolMapper(channels.displaySymbol);
            channels.instDefs.subscribe(fibers.indy.getFiber(), displaySymbolMapper::setInstDef);
            channels.searchResults.subscribe(fibers.indy.getFiber(), displaySymbolMapper::setSearchResult);
        }

        // Indy
        final ConfigGroup indyConfig = root.getGroup("indy");

        final IIndyCacheListener indyListener = new IndyClient(channels.instDefs);

        final SelectIOFiber selectIOFiber = new SelectIOFiber(selectIO, errorLog, "Select IO");

        final String indyUsername = indyConfig.getString("username");

        final IResourceMonitor<IndyTransportComponents> indyMonitor =
                new ExpandedDetailResourceMonitor<>(monitor, "Indy", errorLog, IndyTransportComponents.class, ReddalComponents.INDY);
        final TransportTCPKeepAliveConnection<?, ?> indyConnection =
                IndyCacheFactory.createClient(selectIO, indyConfig, indyMonitor, indyUsername, false, indyListener);
        selectIO.execute(indyConnection::restart);

        setupYodaSignals(selectIO, monitor, errorLog, root, app.appName, channels.stockAlerts);

        final ConfigGroup opxlConfig = root.getEnabledGroup("opxl");

        // Desk Position
        final ConfigGroup deskPositionConfig = root.getEnabledGroup("opxl", "deskposition");
        if (null != deskPositionConfig) {
            final ConfigParam keysParam = deskPositionConfig.getParam("keys");
            final Set<String> keys = keysParam.getSet(Pattern.compile(","));
            new ReconnectingOPXLClient(opxlConfig.getString("host"), opxlConfig.getInt("port"),
                    new OpxlPositionSubscriber(channels.errorPublisher, channels.metaData::publish)::onOpxlData, keys,
                    fibers.opxlPosition.getFiber(), channels.error);
        }

        final ConfigGroup pksConfig = root.getEnabledGroup("pks");
        if (null != pksConfig) {

            final IResourceMonitor<PositionTransportComponents> pksMonitor =
                    new ExpandedDetailResourceMonitor<>(monitor, "PKS", errorLog, PositionTransportComponents.class, ReddalComponents.PKS);

            final PKSPositionClient pksClient = new PKSPositionClient(channels.pksExposure);
            channels.searchResults.subscribe(selectIOFiber, pksClient::setSearchResult);

            final PositionClientHandler cache =
                    PositionCacheFactory.createClientCache(selectIO, pksMonitor, "PKS", app.appName, pksClient, pksClient, true);

            final TransportTCPKeepAliveConnection<?, ?> client = PositionCacheFactory.createClient(selectIO, pksConfig, pksMonitor, cache);
            client.restart();
        }

        // Ladder Text
        final ConfigGroup ladderTextConfig = root.getEnabledGroup("opxl", "laddertext");
        if (null != ladderTextConfig) {
            final ConfigParam keysParam = ladderTextConfig.getParam("keys");
            final Set<String> keys = keysParam.getSet(Pattern.compile(","));
            new ReconnectingOPXLClient(opxlConfig.getString("host"), opxlConfig.getInt("port"),
                    new OpxlLadderTextSubscriber(channels.errorPublisher, channels.metaData)::onOpxlData, keys, fibers.metaData.getFiber(),
                    channels.error);
        }

        // Error souting
        {
            channels.error.subscribe(fibers.logging.getFiber(), message -> {
                System.out.println(new Date());
                message.printStackTrace();
            });
            channels.stats.subscribe(fibers.logging.getFiber(), message -> {
                if (message instanceof AdvisoryStat) {
                    System.out.println(new Date());
                    System.out.println(message);
                }
            });
        }

        // Logging
        fibers.logging.subscribe(new ErrorLogger(logDir.resolve("errors.log").toFile()).onThrowableCallback(), channels.error);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "remote-order.json", channels.errorPublisher),
                channels.workingOrderEvents, channels.remoteOrderEvents);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "trading-status.json", channels.errorPublisher),
                channels.tradingStatus);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "preferences.json", channels.errorPublisher),
                channels.ladderPrefsLoaded, channels.storeLadderPref);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "status.json", channels.errorPublisher), channels.stats);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "heartbeats.json", channels.errorPublisher),
                channels.heartbeatRoundTrips);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "contracts.json", channels.errorPublisher), channels.contractSets);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "single-order.json", channels.errorPublisher),
                channels.singleOrderCommand);
        fibers.logging.subscribe(new FileLogger(clock, logDir, "stockAlerts.json", channels.errorPublisher), channels.stockAlerts);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "trace.json", channels.errorPublisher), channels.trace);

        for (final Map.Entry<String, TypedChannel<WebSocketControlMessage>> stringTypedChannelEntry : websocketsForLogging.entrySet()) {
            fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "websocket" + stringTypedChannelEntry.getKey() + ".json",
                    channels.errorPublisher), stringTypedChannelEntry.getValue());
        }

        app.addStartUpAction(fibers::start);
        app.run();
    }

    private static void setupEntityAliases(Environment environment, WebApplication webapp) throws ConfigException {
        webapp.alias("/style.css", "/style-" + environment.getEntity() + ".css");
        webapp.alias("/launcher.js", "/launcher-" + environment.getEntity() + ".js");
    }

    private static Transport createEnableAbleTransport(final LowTrafficMulticastTransport lowTrafficMulticastTransport,
            final AtomicBoolean multicastEnabled) {
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

    private static void setupYodaSignals(final SelectIO selectIO, final IResourceMonitor<ReddalComponents> monitor,
            final IErrorLogger errorLog, final ConfigGroup config, final String appName, final Publisher<StockAlert> stockAlerts)
            throws ConfigException {

        final ConfigGroup yodaConfig = config.getEnabledGroup("yoda");
        if (null != yodaConfig) {
            final IResourceMonitor<YodaTransportComponents> yodaMonitor =
                    new ExpandedDetailResourceMonitor<>(monitor, "Yoda", errorLog, YodaTransportComponents.class, ReddalComponents.YODA);

            final MultiLayeredResourceMonitor<YodaTransportComponents> yodaParentMonitor =
                    new MultiLayeredResourceMonitor<>(yodaMonitor, YodaTransportComponents.class, errorLog);

            final long millisAtMidnight = selectIO.getMillisAtMidnightUTC();

            for (final ConfigGroup yodaInstanceConfig : yodaConfig.groups()) {

                final String instanceName = yodaInstanceConfig.getKey();
                final IResourceMonitor<YodaTransportComponents> yodaChildMonitor =
                        yodaParentMonitor.createChildResourceMonitor(instanceName);

                final YodaRestingOrderClient restingClient = new YodaRestingOrderClient(millisAtMidnight, stockAlerts);
                final YodaSweepClient sweepClient = new YodaSweepClient(millisAtMidnight, stockAlerts);
                final YodaTWAPClient twapClient = new YodaTWAPClient(millisAtMidnight, stockAlerts);

                final YodaClientHandler yodaHandler =
                        YodaClientCacheFactory.createClientCache(selectIO, yodaChildMonitor, "yoda " + instanceName, appName,
                                new YodaNullClient<>(), restingClient, sweepClient, twapClient, new YodaNullClient<>(),
                                EnumSet.of(YodaSignalType.RESTING_ORDER, YodaSignalType.SWEEP, YodaSignalType.TWAP));

                final TransportTCPKeepAliveConnection<?, ?> client =
                        YodaClientCacheFactory.createClient(selectIO, yodaInstanceConfig, yodaChildMonitor, yodaHandler);
                selectIO.execute(client::restart);
            }
        }
    }
}
