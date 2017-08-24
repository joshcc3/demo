package com.drwtrading.london.reddal;

import com.drw.nns.api.MulticastGroup;
import com.drw.nns.api.NnsApi;
import com.drw.nns.api.NnsFactory;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.nibbler.transport.NibblerTransportComponents;
import com.drwtrading.london.eeif.nibbler.transport.cache.NibblerCacheFactory;
import com.drwtrading.london.eeif.nibbler.transport.cache.NibblerTransportCaches;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.photocols.client.OnHeapBufferPhotocolsNioClient;
import com.drwtrading.london.eeif.position.transport.PositionTransportComponents;
import com.drwtrading.london.eeif.position.transport.cache.PositionCacheFactory;
import com.drwtrading.london.eeif.position.transport.io.PositionClientHandler;
import com.drwtrading.london.eeif.stack.manager.StackManagerComponents;
import com.drwtrading.london.eeif.stack.manager.io.StackManagerServer;
import com.drwtrading.london.eeif.stack.manager.io.StackNibblerClient;
import com.drwtrading.london.eeif.stack.manager.persistence.StackPersistenceComponents;
import com.drwtrading.london.eeif.stack.manager.persistence.StackPersistenceWriter;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.transport.StackTransportComponents;
import com.drwtrading.london.eeif.stack.transport.cache.StackCacheFactory;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.Application;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.io.SelectIOComponents;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.MDTransportComponents;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClientFactory;
import com.drwtrading.london.eeif.utils.monitoring.ConcurrentMultiLayeredResourceMonitor;
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
import com.drwtrading.london.jetlang.stats.MonitoredJetlangFactory;
import com.drwtrading.london.jetlang.transport.LowTrafficMulticastTransport;
import com.drwtrading.london.logging.JsonChannelLogger;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.photons.eeifoe.OrderEntryCommandMsg;
import com.drwtrading.london.photons.eeifoe.OrderEntryReplyMsg;
import com.drwtrading.london.photons.eeifoe.OrderUpdateEvent;
import com.drwtrading.london.photons.eeifoe.OrderUpdateEventMsg;
import com.drwtrading.london.photons.eeifoe.Update;
import com.drwtrading.london.photons.reddal.Heartbeat;
import com.drwtrading.london.photons.reddal.ReddalMessage;
import com.drwtrading.london.reddal.autopull.AutoPullPersistence;
import com.drwtrading.london.reddal.autopull.AutoPuller;
import com.drwtrading.london.reddal.autopull.AutoPullerUI;
import com.drwtrading.london.reddal.autopull.PullerBookSubscriber;
import com.drwtrading.london.reddal.blotter.BlotterClient;
import com.drwtrading.london.reddal.blotter.MsgBlotterPresenter;
import com.drwtrading.london.reddal.blotter.SafetiesBlotterPresenter;
import com.drwtrading.london.reddal.data.ibook.DepthBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.LevelThreeBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.LevelTwoBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.NoMDSubscriptions;
import com.drwtrading.london.reddal.ladders.LadderMessageRouter;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.LadderSettings;
import com.drwtrading.london.reddal.ladders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.history.HistoryPresenter;
import com.drwtrading.london.reddal.ladders.tradingData.LadderInfoListener;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlLadderTextSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlPositionSubscriber;
import com.drwtrading.london.reddal.opxl.StackManagerFiltersOPXL;
import com.drwtrading.london.reddal.opxl.UltimateParentOPXL;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportConnected;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.pks.PKSPositionClient;
import com.drwtrading.london.reddal.position.PositionSubscriptionPhotocolsHandler;
import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;
import com.drwtrading.london.reddal.stacks.StackCallbackBatcher;
import com.drwtrading.london.reddal.stacks.StackGroupCallbackBatcher;
import com.drwtrading.london.reddal.stacks.StackManagerGroupCallbackBatcher;
import com.drwtrading.london.reddal.stacks.configui.StackConfigPresenter;
import com.drwtrading.london.reddal.stacks.family.StackChildListener;
import com.drwtrading.london.reddal.stacks.family.StackFamilyListener;
import com.drwtrading.london.reddal.stacks.family.StackFamilyPresenter;
import com.drwtrading.london.reddal.stacks.opxl.StackGroupOPXLView;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesPresenter;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.stockAlerts.StockAlertPresenter;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaRestingOrderClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaSweepClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaTWAPClient;
import com.drwtrading.london.reddal.symbols.ChixInstMatcher;
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
import com.drwtrading.london.reddal.workspace.LadderWorkspace;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.london.reddal.workspace.WorkspaceRequestHandler;
import com.drwtrading.london.time.Clock;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sun.jndi.toolkit.url.Uri;
import eeif.execution.WorkingOrderEvent;
import eeif.execution.WorkingOrderUpdate;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.channels.Publisher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static final int MD_SERVER_TIMEOUT = 5000;

    public static final long SERVER_TIMEOUT = 3000L;
    public static final long HEARTBEAT_INTERVAL_MS = 3000;
    public static final long RECONNECT_INTERVAL_MILLIS = 10000;

    private static final String EWOK_BASE_URL_PARAM = "ewokBaseURL";
    private static final String IS_EQUITIES_SEARCHABLE_PARAM = "isEquitiesSearchable";
    private static final String IS_FUTURES_SEARCHABLE_PARAM = "isFuturesSearchable";

    private static final String IS_STACK_MANAGER_PARAM = "isManager";
    private static final String MD_SOURCES_PARAM = "mdSources";

    private static final String TRANSPORT_REMOTE_CMDS_NAME_PARAM = "remoteCommands";

    public static void main(final String[] args) throws Exception {

        final Application<ReddalComponents> app = new Application<>(args, "Reddal", ReddalComponents.class);

        final IClock clock = app.clock;
        final ConfigGroup root = app.config;
        final Path logDir = app.logDir;
        final IErrorLogger errorLog = app.errorLog;
        final IResourceMonitor<ReddalComponents> monitor = app.monitor;
        final SelectIO selectIO = app.selectIO;

        final Environment environment = new Environment(root);

        final String localAppName = app.appName + ':' + app.env.name();

        final NnsApi nnsApi = new NnsFactory().create();
        final LoggingTransport fileTransport = new LoggingTransport(logDir.resolve("jetlang.log").toFile());
        fileTransport.start();
        final MulticastGroup statsGroup = nnsApi.multicastGroupFor(environment.getStatsNns());
        final LowTrafficMulticastTransport lowTrafficMulticastTransport =
                new LowTrafficMulticastTransport(statsGroup.getAddress(), statsGroup.getPort(), environment.getStatsInterface());
        final AtomicBoolean multicastEnabled = new AtomicBoolean(false);
        final Transport enableMulticastTransport = createEnableAbleTransport(lowTrafficMulticastTransport, multicastEnabled);
        final StatsPublisher statsPublisher =
                new StatsPublisher(environment.getStatsName(), new MultiplexTransport(fileTransport, enableMulticastTransport));

        final MonitoredJetlangFactory monitoredJetlangFactory = new MonitoredJetlangFactory(statsPublisher, ERROR_CHANNEL);
        final ReddalChannels channels = new ReddalChannels(monitoredJetlangFactory);
        final ReddalFibers fibers = new ReddalFibers(channels, monitoredJetlangFactory);

        final SelectIOFiber selectIOFiber = new SelectIOFiber(selectIO, errorLog, "Main Select IO Fiber");

        final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (t, e) -> channels.errorPublisher.publish(e);

        channels.stats.subscribe(fibers.stats.getFiber(), statsPublisher::publish);
        fibers.stats.getFiber().schedule(() -> {
            statsPublisher.start();
            multicastEnabled.set(true);
        }, 10, TimeUnit.SECONDS);

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

        final UILogger webLog = new UILogger(new SystemClock(), logDir);

        final boolean isEquitiesSearchable =
                !root.paramExists(IS_EQUITIES_SEARCHABLE_PARAM) || root.getBoolean(IS_EQUITIES_SEARCHABLE_PARAM);
        final boolean isFuturesSearchable = !root.paramExists(IS_FUTURES_SEARCHABLE_PARAM) || root.getBoolean(IS_FUTURES_SEARCHABLE_PARAM);

        final Map<String, TypedChannel<WebSocketControlMessage>> webSocketsForLogging = Maps.newHashMap();

        final WebApplication webapp = new WebApplication(environment.getWebPort(), channels.errorPublisher);
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

        { // Index presenter
            final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("/", "index", fibers.ui, webapp, websocket);
            webSocketsForLogging.put("index", websocket);
            final IndexUIPresenter indexPresenter = new IndexUIPresenter(webLog, isEquitiesSearchable, isFuturesSearchable);
            fibers.ui.subscribe(indexPresenter, channels.displaySymbol, websocket);
            channels.searchResults.subscribe(fibers.ui.getFiber(), indexPresenter::addSearchResult);
        }

        { // Orders presenter
            final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("orders", "orders", fibers.ui, webapp, websocket);
            webSocketsForLogging.put("orders", websocket);
            final OrdersPresenter ordersPresenter =
                    new OrdersPresenter(webLog, channels.singleOrderCommand, channels.orderEntryCommandToServer);
            fibers.ui.subscribe(ordersPresenter, websocket, channels.orderEntryFromServer);
            channels.workingOrders.subscribe(
                    new BatchSubscriber<>(fibers.ui.getFiber(), ordersPresenter::onWorkingOrderBatch, 100, TimeUnit.MILLISECONDS));
        }

        { // Working orders screen
            final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("workingorders", "workingorders", fibers.ui, webapp, ws);

            final Collection<String> nibblers = environment.getList(Environment.WORKING_ORDERS);
            final WorkingOrdersPresenter presenter =
                    new WorkingOrdersPresenter(clock, monitor, webLog, fibers.ui.getFiber(), channels.remoteOrderCommand, nibblers,
                            channels.orderEntryCommandToServer);
            fibers.ui.subscribe(presenter, ws, channels.orderEntryFromServer);
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

        final String ewokBaseURL = root.getString(EWOK_BASE_URL_PARAM);

        final MultiLayeredResourceMonitor<ReddalComponents> parentMonitor =
                new ConcurrentMultiLayeredResourceMonitor<>(monitor, ReddalComponents.class, errorLog);

        final String stackManagerThreadName = "Ladder-StackManager";

        final IResourceMonitor<ReddalComponents> stackManagerMonitor = parentMonitor.createChildResourceMonitor(stackManagerThreadName);
        final IResourceMonitor<SelectIOComponents> stackManagerSelectIOMonitor =
                new ExpandedDetailResourceMonitor<>(stackManagerMonitor, stackManagerThreadName, errorLog, SelectIOComponents.class,
                        ReddalComponents.UI_SELECT_IO);
        final SelectIO stackManagerSelectIO = new SelectIO(stackManagerSelectIOMonitor);
        final IMDSubscriber noBookSubscription = new NoMDSubscriptions();
        final TypedChannel<WebSocketControlMessage> stackManagerWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        final LadderPresenter stackManagerLadderPresenter =
                getLadderPresenter(errorLog, stackManagerMonitor, stackManagerSelectIO, stackManagerThreadName, channels, environment,
                        noBookSubscription, ewokBaseURL, stackManagerWebSocket, fibers);

        final Map<MDSource, LinkedList<ConfigGroup>> stackConfigs = new EnumMap<>(MDSource.class);

        final ConfigGroup stackConfig = root.getEnabledGroup("stacks", "nibblers");
        if (null != stackConfig) {
            for (final ConfigGroup stackClientConfig : stackConfig.groups()) {

                final boolean isStackManager =
                        stackClientConfig.paramExists(IS_STACK_MANAGER_PARAM) && stackClientConfig.getBoolean(IS_STACK_MANAGER_PARAM);

                if (isStackManager) {
                    final StackManagerGroupCallbackBatcher stackUpdateBatcher =
                            new StackManagerGroupCallbackBatcher(stackManagerLadderPresenter, channels.stackParentSymbolPublisher);
                    createStackClient(errorLog, stackManagerMonitor, stackManagerSelectIO, stackManagerThreadName, stackClientConfig,
                            stackUpdateBatcher, localAppName);
                } else {
                    final Set<MDSource> mdSources = stackClientConfig.getEnumSet(MD_SOURCES_PARAM, MDSource.class);
                    for (final MDSource mdSource : mdSources) {
                        final List<ConfigGroup> configGroups = MapUtils.getMappedLinkedList(stackConfigs, mdSource);
                        configGroups.add(stackClientConfig);
                    }
                }
            }
        }

        final SpreadContractSetGenerator contractSetGenerator = new SpreadContractSetGenerator(channels.contractSets);
        channels.searchResults.subscribe(selectIOFiber, contractSetGenerator::setSearchResult);

        setupStackManager(app, fibers, channels, webapp, webLog, selectIOFiber, contractSetGenerator);
        final Map<MDSource, LinkedList<ConfigGroup>> nibblers = setupNibblerTransport(app, fibers, webapp, webLog, selectIOFiber, channels);

        final Map<MDSource, TypedChannel<WebSocketControlMessage>> webSockets = new EnumMap<>(MDSource.class);

        final ConfigGroup mdConfig = root.getGroup("md");
        for (final ConfigGroup mdSourceGroup : mdConfig.groups()) {

            final MDSource mdSource = MDSource.get(mdSourceGroup.getKey());
            if (null == mdSource) {
                throw new ConfigException("MDSource [" + mdSourceGroup.getKey() + "] is not known.");
            } else {

                final String threadName = "Ladder-" + mdSource.name();

                final IResourceMonitor<ReddalComponents> displayMonitor = parentMonitor.createChildResourceMonitor(threadName);
                final IResourceMonitor<SelectIOComponents> selectIOMonitor =
                        new ExpandedDetailResourceMonitor<>(displayMonitor, threadName, errorLog, SelectIOComponents.class,
                                ReddalComponents.UI_SELECT_IO);

                final SelectIO displaySelectIO = new SelectIO(selectIOMonitor);

                final IMDSubscriber depthBookSubscriber =
                        getMDSubscription(app, displayMonitor, displaySelectIO, mdSource, mdSourceGroup, channels, localAppName);

                final TypedChannel<WebSocketControlMessage> webSocket = TypedChannels.create(WebSocketControlMessage.class);
                webSockets.put(mdSource, webSocket);

                final LadderPresenter ladderPresenter =
                        getLadderPresenter(errorLog, displayMonitor, displaySelectIO, threadName, channels, environment,
                                depthBookSubscriber, ewokBaseURL, webSocket, fibers);

                final List<ConfigGroup> mdSourceStackConfigs = stackConfigs.get(mdSource);
                if (null != mdSourceStackConfigs) {

                    final MultiLayeredResourceMonitor<ReddalComponents> stackParentMonitor =
                            new MultiLayeredResourceMonitor<>(displayMonitor, ReddalComponents.class, errorLog);

                    for (final ConfigGroup stackClientConfig : mdSourceStackConfigs) {

                        final IResourceMonitor<ReddalComponents> stackMonitor =
                                stackParentMonitor.createChildResourceMonitor(threadName + '-' + stackClientConfig.getKey());

                        final StackGroupCallbackBatcher stackUpdateBatcher = new StackGroupCallbackBatcher(ladderPresenter);
                        createStackClient(errorLog, stackMonitor, displaySelectIO, threadName, stackClientConfig, stackUpdateBatcher,
                                localAppName);
                    }
                }

                final List<ConfigGroup> nibblerConfigs = nibblers.get(mdSource);
                if (null != nibblerConfigs) {

                    final IResourceMonitor<NibblerTransportComponents> nibblerMonitor =
                            new ExpandedDetailResourceMonitor<>(displayMonitor, threadName + "-Nibblers", errorLog,
                                    NibblerTransportComponents.class, ReddalComponents.TRADING_DATA);
                    final MultiLayeredResourceMonitor<NibblerTransportComponents> nibblerParentMonitor =
                            new MultiLayeredResourceMonitor<>(nibblerMonitor, NibblerTransportComponents.class, errorLog);

                    for (final ConfigGroup nibblerConfig : nibblerConfigs) {

                        final IResourceMonitor<NibblerTransportComponents> childMonitor =
                                nibblerParentMonitor.createChildResourceMonitor(nibblerConfig.getKey());

                        final LadderInfoListener ladderInfoListener = new LadderInfoListener(ladderPresenter);
                        final NibblerClientHandler client =
                                NibblerCacheFactory.createClientCache(displaySelectIO, nibblerConfig, childMonitor,
                                        threadName + "-transport-" + nibblerConfig.getKey(), localAppName + mdSource.name(), true,
                                        ladderInfoListener);

                        client.getCaches().tradingDataCache.addListener(ladderInfoListener);
                        client.getCaches().blotterCache.addListener(ladderInfoListener);
                    }
                }
            }
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
                    MultiLayeredResourceMonitor.getExpandedMultiLayerMonitor(displayMonitor, "Thread MD", errorLog,
                            MDTransportComponents.class, ReddalComponents.MD_TRANSPORT);

            final PullerBookSubscriber subscriber = new PullerBookSubscriber();

            for (final ConfigGroup mdSourceGroup : mdConfig.groups()) {

                final MDSource mdSource = MDSource.get(mdSourceGroup.getKey());
                if (null == mdSource) {
                    throw new ConfigException("MDSource [" + mdSourceGroup.getKey() + "] is not known.");
                }

                final IResourceMonitor<MDTransportComponents> mdClientMonitor = mdParentMonitor.createChildResourceMonitor(mdSource.name());

                final MDTransportClient mdClient =
                        MDTransportClientFactory.createDepthClient(displaySelectIO, mdClientMonitor, mdSource, app.appName + "-pull",
                                subscriber.getL3(), subscriber.getL2(), MD_SERVER_TIMEOUT, true);

                final TransportTCPKeepAliveConnection<?, ?> connection =
                        MDTransportClientFactory.createConnection(displaySelectIO, mdSourceGroup, mdClientMonitor, mdClient);
                subscriber.setClient(mdSource, mdClient);
                final long staggeringDelay = 1500L;
                displaySelectIO.execute(() -> displaySelectIO.addDelayedAction(staggeringDelay, () -> {
                    connection.restart();
                    return -1;
                }));
            }

            final SelectIOFiber displaySelectIOFiber = new SelectIOFiber(displaySelectIO, errorLog, name);
            final FiberBuilder fiberBuilder = fibers.fiberGroup.wrap(displaySelectIOFiber, name);

            final AutoPullPersistence persistence = new AutoPullPersistence(Paths.get("/site/drw/reddal/data/").resolve("autopull.json"));
            final AutoPuller puller = new AutoPuller(channels.remoteOrderCommand, subscriber, persistence);
            fiberBuilder.subscribe(puller, channels.workingOrders);
            fiberBuilder.getFiber().scheduleWithFixedDelay(puller::timeChecker, 1, 1, TimeUnit.MINUTES);
            final AutoPullerUI autoPullerUI = new AutoPullerUI(puller);
            final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("autopuller", "autopuller", fiberBuilder, webapp, ws);
            fiberBuilder.subscribe(autoPullerUI, ws);
        }

        final ChixInstMatcher chixInstMatcher = new ChixInstMatcher(channels.chixSymbolPairs);
        channels.searchResults.subscribe(fibers.contracts.getFiber(), chixInstMatcher::setSearchResult);

        final TypedChannel<WebSocketControlMessage> historyWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("history", "history", fibers.ladderRouter, webapp, historyWebSocket);
        final HistoryPresenter historyPresenter = new HistoryPresenter(webLog);
        fibers.ladderRouter.subscribe(historyPresenter, historyWebSocket);
        channels.symbolSelections.subscribe(fibers.ladderRouter.getFiber(), historyPresenter::addSymbol);

        // Ladder router
        final TypedChannel<WebSocketControlMessage> ladderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("ladder", "ladder", fibers.ladderRouter, webapp, ladderWebSocket);
        final LadderMessageRouter ladderMessageRouter =
                new LadderMessageRouter(monitor, webLog, channels.symbolSelections, stackManagerWebSocket, webSockets, fibers.ui);
        fibers.ladderRouter.subscribe(ladderMessageRouter, ladderWebSocket);
        channels.searchResults.subscribe(fibers.ladderRouter.getFiber(), ladderMessageRouter::setSearchResult);
        channels.stackParentSymbolPublisher.subscribe(fibers.ladderRouter.getFiber(), ladderMessageRouter::setParentStackSymbol);

        // Non SSO-protected web App to allow AJAX requests
        {
            final WebApplication nonSSOWebapp = new WebApplication(environment.getWebPort() + 1, channels.errorPublisher);

            fibers.onStart(() -> fibers.ui.execute(() -> {
                try {
                    nonSSOWebapp.serveStaticContent("web");
                    nonSSOWebapp.start();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            nonSSOWebapp.webServer();

            // Workspace
            final TypedChannel<WebSocketControlMessage> workspaceSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("workspace", "workspace", fibers.ui, nonSSOWebapp, workspaceSocket);

            final LadderWorkspace ladderWorkspace = new LadderWorkspace(webLog, channels.replaceCommand);
            fibers.ui.subscribe(ladderWorkspace, workspaceSocket);
            channels.contractSets.subscribe(fibers.ui.getFiber(), ladderWorkspace::setContractSet);
            channels.userWorkspaceRequests.subscribe(fibers.ui.getFiber(), ladderWorkspace::openLadder);
            nonSSOWebapp.addHandler("/open",
                    new WorkspaceRequestHandler(ladderWorkspace, new Uri(nonSSOWebapp.getBaseUri()).getHost(), environment.getWebPort()));
        }

        // Settings
        {
            final LadderSettings ladderSettings = new LadderSettings(environment.getSettingsFile(), channels.ladderPrefsLoaded);
            fibers.settings.subscribe(ladderSettings, channels.storeLadderPref);
            fibers.onStart(() -> fibers.settings.execute(ladderSettings::load));
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
                cmdClient.reconnectMillis(RECONNECT_INTERVAL_MILLIS);
                cmdClient.logFile(logDir.resolve("order-entry." + server + ".log").toFile(), fibers.logging.getFiber(), true);
                cmdClient.handler(new PhotocolsStatsPublisher<>(channels.stats, server + " OE Commands", 10));
                cmdClient.handler(new InboundTimeoutWatchdog<>(fibers.remoteOrders.getFiber(),
                        new ConnectionCloser(channels.stats, server + " OE Commands"), SERVER_TIMEOUT));
                cmdClient.handler(client);
                fibers.remoteOrders.subscribe(client, channels.orderEntryCommandToServer);
                fibers.remoteOrders.execute(cmdClient::start);
                System.out.println("EEIF-OE: " + server + "\tCommand: " + command.host);

                final Environment.HostAndNic update = environment.getHostAndNic(Environment.EEIF_OE + "Update", server);
                final OnHeapBufferPhotocolsNioClient<OrderUpdateEventMsg, Void> updateClient =
                        OnHeapBufferPhotocolsNioClient.client(update.host, update.nic, OrderUpdateEventMsg.class, Void.class,
                                fibers.remoteOrders.getFiber(), EXCEPTION_HANDLER);
                updateClient.reconnectMillis(RECONNECT_INTERVAL_MILLIS);
                updateClient.logFile(logDir.resolve("order-update." + server + ".log").toFile(), fibers.logging.getFiber(), true);
                updateClient.handler(new PhotocolsStatsPublisher<>(channels.stats, server + " OE Updates", 10));
                updateClient.handler(new InboundTimeoutWatchdog<>(fibers.remoteOrders.getFiber(),
                        new ConnectionCloser(channels.stats, server + " OE Updates"), SERVER_TIMEOUT));
                updateClient.handler(
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
        final TradingStatusWatchdog watchdog =
                new TradingStatusWatchdog(channels.tradingStatus, SERVER_TIMEOUT, Clock.SYSTEM, channels.stats);
        channels.workingOrderEvents.subscribe(fibers.watchdog.getFiber(), watchdog::setWorkingOrderHeartbeat);
        fibers.watchdog.getFiber().scheduleWithFixedDelay(watchdog::checkHeartbeats, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        channels.nibblerTransportConnected.subscribe(fibers.watchdog.getFiber(), watchdog::setNibblerTransportConnected);

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
            channels.ultimateParents.subscribe(fibers.indy.getFiber(), displaySymbolMapper::setUltimateParent);
        }

        // Indy
        final ConfigGroup indyConfig = root.getGroup("indy");

        final IIndyCacheListener indyListener = new IndyClient(channels.instDefs);

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
            final Set<String> keys = deskPositionConfig.getSet("keys");
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
            final Set<String> keys = ladderTextConfig.getSet("keys");
            new ReconnectingOPXLClient(opxlConfig.getString("host"), opxlConfig.getInt("port"),
                    new OpxlLadderTextSubscriber(channels.errorPublisher, channels.metaData)::onOpxlData, keys, fibers.metaData.getFiber(),
                    channels.error);
        }

        final UltimateParentOPXL ultimateParentOPXL = new UltimateParentOPXL(selectIO, monitor, channels.ultimateParents, logDir);
        app.addStartUpAction(ultimateParentOPXL::connectToOpxl);

        final StackManagerFiltersOPXL etfStackFiltersOPXL =
                new StackManagerFiltersOPXL(selectIO, monitor, channels.etfOPXLStackFilters, logDir);
        app.addStartUpAction(etfStackFiltersOPXL::connectToOpxl);

        // Ex-dates
        new ReconnectingOPXLClient(opxlConfig.getString("host"), opxlConfig.getInt("port"),
                new OpxlExDateSubscriber(channels.errorPublisher, channels.isinsGoingEx)::onOpxlData,
                ImmutableSet.of(OpxlExDateSubscriber.OPXL_KEY), fibers.opxlPosition.getFiber(), channels.errorPublisher);

        // Logging
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "working-orders.json", channels.errorPublisher),
                channels.workingOrderEvents);
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

        for (final Map.Entry<String, TypedChannel<WebSocketControlMessage>> stringTypedChannelEntry : webSocketsForLogging.entrySet()) {
            fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "websocket" + stringTypedChannelEntry.getKey() + ".json",
                    channels.errorPublisher), stringTypedChannelEntry.getValue());
        }

        app.addStartUpAction(fibers::start);
        app.run();
    }

    private static DepthBookSubscriber getMDSubscription(final Application<?> app, final IResourceMonitor<ReddalComponents> displayMonitor,
            final SelectIO displaySelectIO, final MDSource mdSource, final ConfigGroup mdConfig, final ReddalChannels channels,
            final String localAppName) throws ConfigException {

        final LevelThreeBookSubscriber l3BookHandler =
                new LevelThreeBookSubscriber(displayMonitor, channels.searchResults, channels.stockAlerts,
                        channels.stackRefPriceDetailChannel);
        final LevelTwoBookSubscriber l2BookHandler =
                new LevelTwoBookSubscriber(displayMonitor, channels.searchResults, channels.stockAlerts,
                        channels.stackRefPriceDetailChannel);

        final IResourceMonitor<MDTransportComponents> mdClientMonitor =
                new ExpandedDetailResourceMonitor<>(displayMonitor, mdSource.name() + "-Thread", app.errorLog, MDTransportComponents.class,
                        ReddalComponents.MD_TRANSPORT);

        final MDTransportClient mdClient =
                MDTransportClientFactory.createDepthClient(displaySelectIO, mdClientMonitor, mdSource, localAppName, l3BookHandler,
                        l2BookHandler, MD_SERVER_TIMEOUT, true);
        l3BookHandler.setMDClient(mdSource, mdClient);
        l2BookHandler.setMDClient(mdSource, mdClient);

        final TransportTCPKeepAliveConnection<?, ?> connection =
                MDTransportClientFactory.createConnection(displaySelectIO, mdConfig, mdClientMonitor, mdClient);

        app.addStartUpAction(() -> displaySelectIO.execute(connection::restart));

        return new DepthBookSubscriber(l3BookHandler, l2BookHandler);
    }

    private static LadderPresenter getLadderPresenter(final IErrorLogger errorLog, final IResourceMonitor<ReddalComponents> displayMonitor,
            final SelectIO displaySelectIO, final String name, final ReddalChannels channels, final Environment environment,
            final IMDSubscriber depthBookSubscriber, final String ewokBaseURL, final TypedChannel<WebSocketControlMessage> webSocket,
            final ReddalFibers fibers) throws ConfigException {

        final SelectIOFiber displaySelectIOFiber = new SelectIOFiber(displaySelectIO, errorLog, name);

        final LadderPresenter ladderPresenter =
                new LadderPresenter(displayMonitor, depthBookSubscriber, ewokBaseURL, channels.remoteOrderCommand,
                        environment.ladderOptions(), channels.storeLadderPref, channels.heartbeatRoundTrips, channels.reddalCommand,
                        channels.recenterLaddersForUser, displaySelectIOFiber, channels.trace, channels.ladderClickTradingIssues,
                        channels.userCycleContractPublisher, channels.orderEntryCommandToServer, channels.userWorkspaceRequests);

        final FiberBuilder fiberBuilder = fibers.fiberGroup.wrap(displaySelectIOFiber, name);
        fiberBuilder.subscribe(ladderPresenter, webSocket, channels.workingOrders, channels.metaData, channels.position,
                channels.tradingStatus, channels.ladderPrefsLoaded, channels.displaySymbol, channels.reddalCommandSymbolAvailable,
                channels.recenterLaddersForUser, channels.contractSets, channels.chixSymbolPairs, channels.singleOrderCommand,
                channels.replaceCommand, channels.userCycleContractPublisher, channels.orderEntrySymbols, channels.orderEntryFromServer,
                channels.searchResults, channels.isinsGoingEx);

        channels.ladderClickTradingIssues.subscribe(displaySelectIOFiber, ladderPresenter::displayTradeIssue);
        channels.pksExposure.subscribe(displaySelectIOFiber, ladderPresenter::setPKSExposure);

        displaySelectIO.addDelayedAction(1000, ladderPresenter::flushAllLadders);
        displaySelectIO.addDelayedAction(1500, ladderPresenter::sendAllHeartbeats);

        return ladderPresenter;
    }

    private static void createStackClient(final IErrorLogger errorLog, final IResourceMonitor<ReddalComponents> displayMonitor,
            final SelectIO displaySelectIO, final String name, final ConfigGroup stackConfig,
            final StackGroupCallbackBatcher stackUpdateBatcher, final String localAppName) throws ConfigException {

        final MultiLayeredResourceMonitor<StackTransportComponents> stackParentMonitor =
                MultiLayeredResourceMonitor.getExpandedMultiLayerMonitor(displayMonitor, "Stacks", errorLog, StackTransportComponents.class,
                        ReddalComponents.STACK_GROUP_CLIENT);

        final String connectionName = name + "-stack-" + stackConfig.getKey();
        final IResourceMonitor<StackTransportComponents> stackMonitor = stackParentMonitor.createChildResourceMonitor(connectionName);

        final StackClientHandler client =
                StackCacheFactory.createClientCache(displaySelectIO, stackConfig, stackMonitor, connectionName, localAppName,
                        stackUpdateBatcher);
        stackUpdateBatcher.setStackClient(client);
    }

    public static void createWebPageWithWebSocket(final String alias, final String name, final FiberBuilder fiber,
            final WebApplication webapp, final TypedChannel<WebSocketControlMessage> websocketChannel) {
        webapp.alias('/' + alias, '/' + name + ".html");
        webapp.createWebSocket('/' + name + "/ws/", websocketChannel, fiber.getFiber());

    }

    public static final TypedChannel<Throwable> ERROR_CHANNEL = TypedChannels.create(Throwable.class);

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

    private static void setupStackManager(final Application<ReddalComponents> app, final ReddalFibers fibers, final ReddalChannels channels,
            final WebApplication webApp, final UILogger webLog, final SelectIOFiber selectIOFiber,
            final SpreadContractSetGenerator contractSetGenerator) throws Exception {

        final ConfigGroup stackConfig = app.config.getEnabledGroup("stacks");
        if (null != stackConfig) {

            final IResourceMonitor<StackManagerComponents> stackManagerMonitor =
                    new ExpandedDetailResourceMonitor<>(app.monitor, "Stack Manager", app.errorLog, StackManagerComponents.class,
                            ReddalComponents.STACK_MANAGER);

            final StackManagerServer server =
                    new StackManagerServer(app.selectIO, app.selectIO, stackConfig, stackManagerMonitor, app.errorLog, app.persistenceDir,
                            app.logDir);
            final StackCommunityManager communityManager = server.getCommunityManager();

            final StackFamilyPresenter stackFamilyPresenter = new StackFamilyPresenter(fibers.ui, webLog, contractSetGenerator);
            final StackConfigPresenter stackConfigPresenter = new StackConfigPresenter(fibers.ui, webLog);
            final StackStrategiesPresenter strategiesPresenter = new StackStrategiesPresenter(fibers.ui, webLog);

            final String stackOPXLTopic = stackConfig.getString("opxlSpreadTopic");
            final StackGroupOPXLView stackOPXLView = new StackGroupOPXLView(app.monitor, stackOPXLTopic);

            channels.metaData.subscribe(new BatchSubscriber<>(selectIOFiber, stackOPXLView::setLaserLines, 100, TimeUnit.MILLISECONDS));

            app.selectIO.addDelayedAction(5000, stackOPXLView::update);

            stackFamilyPresenter.setCommunityManager(communityManager);

            final StackFamilyListener familyListener = new StackFamilyListener(stackFamilyPresenter);
            server.addStrategyListener(familyListener);
            server.addStacksListener(familyListener);
            server.addRelationshipListener(stackFamilyPresenter);

            final Path logPath = app.logDir.resolve("stackLog.csv");
            final IResourceMonitor<StackPersistenceComponents> logMonitor =
                    new ExpandedDetailResourceMonitor<>(stackManagerMonitor, "Stacks log", app.errorLog, StackPersistenceComponents.class,
                            StackManagerComponents.LOGGER);

            final StackPersistenceWriter stackLogger = new StackPersistenceWriter(app.selectIO, logMonitor, logPath);

            final MultiLayeredResourceMonitor<StackTransportComponents> clientMonitorParent =
                    MultiLayeredResourceMonitor.getExpandedMultiLayerMonitor(stackManagerMonitor, "Stacks", app.errorLog,
                            StackTransportComponents.class, StackManagerComponents.NIBBLER_CACHE);

            final ConfigGroup nibblerConfigs = stackConfig.getGroup("nibblers");

            for (final ConfigGroup nibblerConfig : nibblerConfigs.groups()) {

                final String nibbler = nibblerConfig.getKey();
                final String connectionName = app.appName + " config";

                final StackChildListener childListener = new StackChildListener(nibbler, stackFamilyPresenter);

                final boolean isStackManager =
                        nibblerConfig.paramExists(IS_STACK_MANAGER_PARAM) && nibblerConfig.getBoolean(IS_STACK_MANAGER_PARAM);
                final StackCallbackBatcher stackUpdateBatcher =
                        new StackCallbackBatcher(nibbler, strategiesPresenter, stackConfigPresenter, childListener, isStackManager,
                                contractSetGenerator);

                final StackNibblerClient nibblerClient = new StackNibblerClient(nibbler, communityManager, stackUpdateBatcher);

                final IResourceMonitor<StackTransportComponents> nibblerMonitor =
                        clientMonitorParent.createChildResourceMonitor(connectionName);
                final StackClientHandler client =
                        StackCacheFactory.createClientCache(app.selectIO, nibblerConfig, nibblerMonitor, "Stacks-" + nibbler,
                                app.env.name() + connectionName, nibblerClient);
                client.addLogger(stackLogger);

                nibblerClient.setClient(client);
                if (!isStackManager) {
                    stackFamilyPresenter.setStrategyClient(nibbler, client);
                }
                strategiesPresenter.setStrategyClient(nibbler, client);
                stackConfigPresenter.setConfigClient(nibbler, client);
            }

            channels.searchResults.subscribe(selectIOFiber, searchResult -> {
                stackFamilyPresenter.setSearchResult(searchResult);
                strategiesPresenter.addInstID(searchResult.symbol, searchResult.instID);
            });
            channels.symbolSelections.subscribe(selectIOFiber, stackFamilyPresenter::symbolSelected);
            channels.etfOPXLStackFilters.subscribe(selectIOFiber, stackFamilyPresenter::setFilter);

            final TypedChannel<WebSocketControlMessage> familyWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackManager", "stackManager", fibers.ladderRouter, webApp, familyWebSocket);
            familyWebSocket.subscribe(selectIOFiber, stackFamilyPresenter::webControl);

            final TypedChannel<WebSocketControlMessage> configWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackConfig", "stackConfig", fibers.ladderRouter, webApp, configWebSocket);
            configWebSocket.subscribe(selectIOFiber, stackConfigPresenter::webControl);

            final TypedChannel<WebSocketControlMessage> strategiesWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackStrategy", "stackStrategy", fibers.ladderRouter, webApp, strategiesWebSocket);
            strategiesWebSocket.subscribe(selectIOFiber, strategiesPresenter::webControl);
        }
    }

    private static Map<MDSource, LinkedList<ConfigGroup>> setupNibblerTransport(final Application<ReddalComponents> app,
            final ReddalFibers fibers, final WebApplication webApp, final UILogger webLog, final SelectIOFiber selectIOFiber,
            final ReddalChannels channels) throws ConfigException, IOException {

        final Map<MDSource, LinkedList<ConfigGroup>> result = new EnumMap<>(MDSource.class);

        final ConfigGroup nibblerConfigs = app.config.getEnabledGroup("nibblers");
        if (null != nibblerConfigs) {

            final MsgBlotterPresenter msgBlotter = new MsgBlotterPresenter(app.selectIO, fibers.ui, webLog);
            final SafetiesBlotterPresenter safetiesBlotter = new SafetiesBlotterPresenter(fibers.ui, webLog);

            final MultiLayeredResourceMonitor<ReddalComponents> clientMonitorParent =
                    new MultiLayeredResourceMonitor<>(app.monitor, ReddalComponents.class, app.errorLog);

            for (final ConfigGroup nibblerConfig : nibblerConfigs.groups()) {

                final String nibbler = nibblerConfig.getKey();
                final String connectionName = app.appName + " config";

                final IResourceMonitor<ReddalComponents> childMonitor = clientMonitorParent.createChildResourceMonitor(connectionName);

                if (nibblerConfig.paramExists(MD_SOURCES_PARAM)) {
                    final Set<MDSource> mdSources = nibblerConfig.getEnumSet(MD_SOURCES_PARAM, MDSource.class);
                    for (final MDSource mdSource : mdSources) {
                        final List<ConfigGroup> nibblerConnectionConfigs = MapUtils.getMappedLinkedList(result, mdSource);
                        nibblerConnectionConfigs.add(nibblerConfig);
                    }
                }

                final boolean isTransportForTrading = nibblerConfig.paramExists(TRANSPORT_REMOTE_CMDS_NAME_PARAM);
                final Publisher<NibblerTransportConnected> connectedNibblerChannel;
                final String remoteOrderNibblerName;
                if (isTransportForTrading) {
                    connectedNibblerChannel = channels.nibblerTransportConnected;
                    remoteOrderNibblerName = nibblerConfig.getString(TRANSPORT_REMOTE_CMDS_NAME_PARAM);
                } else {
                    connectedNibblerChannel = Constants::NO_OP;
                    remoteOrderNibblerName = null;
                }

                final IResourceMonitor<NibblerTransportComponents> nibblerMonitor =
                        new ExpandedDetailResourceMonitor<>(childMonitor, "Nibbler Transport", app.errorLog,
                                NibblerTransportComponents.class, ReddalComponents.BLOTTER_CONNECTION);

                final BlotterClient blotterClient =
                        new BlotterClient(nibbler, msgBlotter, safetiesBlotter, connectedNibblerChannel, remoteOrderNibblerName);

                final NibblerClientHandler client =
                        NibblerCacheFactory.createClientCache(app.selectIO, nibblerConfig, nibblerMonitor, "nibblers-" + nibbler,
                                connectionName, false, blotterClient);

                final NibblerTransportCaches cache = client.getCaches();
                cache.addListener(blotterClient);

                safetiesBlotter.setNibblerClient(nibbler, client);

                if (isTransportForTrading) {

                    final TypedChannel<IOrderCmd> sendCmds = channels.remoteOrderCommandByServer.get(remoteOrderNibblerName);
                    final NibblerTransportOrderEntry orderEntry =
                            new NibblerTransportOrderEntry(app.selectIO, childMonitor, client, app.logDir);
                    sendCmds.subscribe(selectIOFiber, orderEntry::submit);
                }
            }

            final TypedChannel<WebSocketControlMessage> msgBlotterWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("blotter", "blotter", fibers.ladderRouter, webApp, msgBlotterWebSocket);
            msgBlotterWebSocket.subscribe(selectIOFiber, msgBlotter::webControl);

            final TypedChannel<WebSocketControlMessage> safetiesWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("safeties", "safeties", fibers.ladderRouter, webApp, safetiesWebSocket);
            safetiesWebSocket.subscribe(selectIOFiber, safetiesBlotter::webControl);
        }

        return result;
    }
}
