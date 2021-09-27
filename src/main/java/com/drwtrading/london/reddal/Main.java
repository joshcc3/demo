package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.additiveTransport.AdditiveTransportFuses;
import com.drwtrading.london.eeif.additiveTransport.cache.AdditiveCacheFactory;
import com.drwtrading.london.eeif.additiveTransport.io.AdditiveClientHandler;
import com.drwtrading.london.eeif.nibbler.transport.NibblerTransportComponents;
import com.drwtrading.london.eeif.nibbler.transport.cache.NibblerCacheFactory;
import com.drwtrading.london.eeif.nibbler.transport.cache.NibblerTransportCaches;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.photocols.client.OnHeapBufferPhotocolsNioClient;
import com.drwtrading.london.eeif.position.transport.PositionTransportComponents;
import com.drwtrading.london.eeif.position.transport.cache.PositionCacheFactory;
import com.drwtrading.london.eeif.position.transport.io.PositionClientHandler;
import com.drwtrading.london.eeif.stack.manager.StackManagerComponents;
import com.drwtrading.london.eeif.stack.manager.io.StackManagerServer;
import com.drwtrading.london.eeif.stack.manager.io.StackNibblerClient;
import com.drwtrading.london.eeif.stack.manager.persistence.StackPersistenceComponents;
import com.drwtrading.london.eeif.stack.manager.persistence.StackPersistenceWriter;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.transport.StackTransportComponents;
import com.drwtrading.london.eeif.stack.transport.cache.StackCacheFactory;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.AccountGroup;
import com.drwtrading.london.eeif.utils.application.Application;
import com.drwtrading.london.eeif.utils.application.TradingEntity;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.io.SelectIOComponents;
import com.drwtrading.london.eeif.utils.io.channels.IOConfigParser;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.marketData.fx.md.FXMDUtils;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.MDTransportComponents;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClientFactory;
import com.drwtrading.london.eeif.utils.monitoring.ConcurrentMultiLayeredFuseBox;
import com.drwtrading.london.eeif.utils.monitoring.ExpandedDetailResourceMonitor;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.monitoring.IgnoredFuseBox;
import com.drwtrading.london.eeif.utils.monitoring.MultiLayeredFuseBox;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.eeif.utils.time.SystemClock;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.eeif.utils.transport.cache.TransportCache;
import com.drwtrading.london.eeif.utils.transport.io.TransportTCPKeepAliveConnection;
import com.drwtrading.london.eeif.yoda.transport.YodaSignalType;
import com.drwtrading.london.eeif.yoda.transport.YodaTransportComponents;
import com.drwtrading.london.eeif.yoda.transport.cache.YodaClientCacheFactory;
import com.drwtrading.london.eeif.yoda.transport.cache.YodaNullClient;
import com.drwtrading.london.eeif.yoda.transport.io.YodaClientHandler;
import com.drwtrading.london.icepie.transport.IcePieTransportComponents;
import com.drwtrading.london.icepie.transport.data.LadderTextFreeText;
import com.drwtrading.london.icepie.transport.data.LaserLineValue;
import com.drwtrading.london.icepie.transport.io.IcePieCacheFactory;
import com.drwtrading.london.icepie.transport.io.LadderTextNumber;
import com.drwtrading.london.indy.transport.IndyTransportComponents;
import com.drwtrading.london.indy.transport.cache.IndyCacheFactory;
import com.drwtrading.london.jetlang.DefaultJetlangFactory;
import com.drwtrading.london.logging.JsonChannelLogger;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.reddal.autopull.autopuller.onMD.AutoPuller;
import com.drwtrading.london.reddal.autopull.autopuller.ui.AutoPullPersistence;
import com.drwtrading.london.reddal.autopull.autopuller.ui.AutoPullerUI;
import com.drwtrading.london.reddal.blotter.BlotterClient;
import com.drwtrading.london.reddal.blotter.MsgBlotterPresenter;
import com.drwtrading.london.reddal.blotter.SafetiesBlotterPresenter;
import com.drwtrading.london.reddal.data.ibook.DepthBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.LevelThreeBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.LevelTwoBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.NoMDSubscriptions;
import com.drwtrading.london.reddal.data.ibook.ReddalMDTransportClient;
import com.drwtrading.london.reddal.icepie.FreeTextCacheListener;
import com.drwtrading.london.reddal.icepie.LadderTextNumberCacheListener;
import com.drwtrading.london.reddal.icepie.LaserLineCacheListener;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.ladders.LadderMessageRouter;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.RecenterLadder;
import com.drwtrading.london.reddal.ladders.history.HistoryPresenter;
import com.drwtrading.london.reddal.ladders.impliedGenerator.ImpliedMDInfoGenerator;
import com.drwtrading.london.reddal.ladders.orders.OrderPresenterMsgRouter;
import com.drwtrading.london.reddal.ladders.orders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.settings.LadderSettings;
import com.drwtrading.london.reddal.ladders.shredders.ShredderMessageRouter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;
import com.drwtrading.london.reddal.nibblers.NibblerMetaDataLogger;
import com.drwtrading.london.reddal.nibblers.tradingData.AdditiveOffsetListener;
import com.drwtrading.london.reddal.nibblers.tradingData.LadderInfoListener;
import com.drwtrading.london.reddal.opxl.OPXLEtfStackFilters;
import com.drwtrading.london.reddal.opxl.OPXLPicardFilterReader;
import com.drwtrading.london.reddal.opxl.OPXLSpreadnoughtFilters;
import com.drwtrading.london.reddal.opxl.OpxlDividendTweets;
import com.drwtrading.london.reddal.opxl.OpxlExDateSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlFXCalcUpdater;
import com.drwtrading.london.reddal.opxl.OpxlPositionSubscriber;
import com.drwtrading.london.reddal.opxl.OpxlShortSensitiveIsinsSubscriber;
import com.drwtrading.london.reddal.opxl.UltimateParentOPXL;
import com.drwtrading.london.reddal.orderManagement.NibblerTransportConnected;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.BulkOrderEntryPresenter;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.BulkOrderMarketAnalyser;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPrices;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPricesRequest;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCSupportedSymbol;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.opxl.OPXLBulkOrderPriceLimits;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.picard.DelegatingPicardUI;
import com.drwtrading.london.reddal.picard.IPicardSpotter;
import com.drwtrading.london.reddal.picard.LiquidityFinderData;
import com.drwtrading.london.reddal.picard.LiquidityFinderViewUI;
import com.drwtrading.london.reddal.picard.PicardFXCalcComponents;
import com.drwtrading.london.reddal.picard.PicardRow;
import com.drwtrading.london.reddal.picard.PicardRowWithInstID;
import com.drwtrading.london.reddal.picard.PicardSounds;
import com.drwtrading.london.reddal.picard.PicardSpotter;
import com.drwtrading.london.reddal.picard.PicardUI;
import com.drwtrading.london.reddal.picard.YodaAtCloseClient;
import com.drwtrading.london.reddal.pks.PKSPositionClient;
import com.drwtrading.london.reddal.position.PositionPhotocolsHandler;
import com.drwtrading.london.reddal.premium.IPremiumCalc;
import com.drwtrading.london.reddal.premium.PremiumCalculator;
import com.drwtrading.london.reddal.premium.PremiumOPXLWriter;
import com.drwtrading.london.reddal.signals.SignalsHandler;
import com.drwtrading.london.reddal.stacks.IStackPresenterCallback;
import com.drwtrading.london.reddal.stacks.StackCallbackBatcher;
import com.drwtrading.london.reddal.stacks.StackGroupCallbackBatcher;
import com.drwtrading.london.reddal.stacks.StackManagerGroupCallbackBatcher;
import com.drwtrading.london.reddal.stacks.StackPresenterMultiplexor;
import com.drwtrading.london.reddal.stacks.StackRunnableInfo;
import com.drwtrading.london.reddal.stacks.autoManager.StackAutoManagerPresenter;
import com.drwtrading.london.reddal.stacks.configui.StackConfigPresenter;
import com.drwtrading.london.reddal.stacks.family.StackChildListener;
import com.drwtrading.london.reddal.stacks.family.StackFamilyListener;
import com.drwtrading.london.reddal.stacks.family.StackFamilyPresenter;
import com.drwtrading.london.reddal.stacks.opxl.OpxlStrategyOffsetsUI;
import com.drwtrading.london.reddal.stacks.opxl.OpxlStrategySymbolUI;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesPresenter;
import com.drwtrading.london.reddal.stockAlerts.RfqAlert;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import com.drwtrading.london.reddal.stockAlerts.StockAlertPresenter;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaRestingOrderClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaSweepClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaTWAPClient;
import com.drwtrading.london.reddal.stockAlerts.yoda.YodaTweetClient;
import com.drwtrading.london.reddal.symbols.ChixInstMatcher;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.symbols.DisplaySymbolMapper;
import com.drwtrading.london.reddal.symbols.IndexUIPresenter;
import com.drwtrading.london.reddal.symbols.IndyClient;
import com.drwtrading.london.reddal.symbols.RFQCommunityPublisher;
import com.drwtrading.london.reddal.trades.JasperTradesListener;
import com.drwtrading.london.reddal.trades.MrChillTrade;
import com.drwtrading.london.reddal.util.ConnectionCloser;
import com.drwtrading.london.reddal.util.FileLogger;
import com.drwtrading.london.reddal.util.NibblerNotificationHandler;
import com.drwtrading.london.reddal.util.PhotocolsStatsPublisher;
import com.drwtrading.london.reddal.util.SelectIOFiber;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.NoWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderListener;
import com.drwtrading.london.reddal.workingOrders.bestPrices.BestWorkingOrderMaintainer;
import com.drwtrading.london.reddal.workingOrders.bestPrices.OPXLBestWorkingOrdersPresenter;
import com.drwtrading.london.reddal.workingOrders.gtc.GTCWorkingOrderMaintainer;
import com.drwtrading.london.reddal.workingOrders.gtc.OPXLGTCWorkingOrdersPresenter;
import com.drwtrading.london.reddal.workingOrders.obligations.futures.FutureObligationPresenter;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuotingObligationsPresenter;
import com.drwtrading.london.reddal.workingOrders.ui.WorkingOrdersPresenter;
import com.drwtrading.london.reddal.workspace.LadderWorkspace;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.london.reddal.workspace.WorkspaceRequestHandler;
import com.drwtrading.monitoring.stats.status.StatusStat;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.handlers.ConnectionAwareJetlangChannelHandler;
import com.drwtrading.photocols.handlers.InboundTimeoutWatchdog;
import com.drwtrading.photocols.handlers.JetlangChannelHandler;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketControlMessage;
import drw.eeif.eeifoe.OrderEntryCommandMsg;
import drw.eeif.eeifoe.OrderEntryReplyMsg;
import drw.eeif.eeifoe.OrderUpdateEvent;
import drw.eeif.eeifoe.OrderUpdateEventMsg;
import drw.eeif.eeifoe.Update;
import drw.eeif.phockets.Phockets;
import drw.eeif.phockets.tcp.PhocketClient;
import drw.eeif.photons.mrchill.Position;
import drw.eeif.photons.signals.Signals;
import drw.eeif.trades.transport.outbound.io.TradesClientFactory;
import drw.eeif.trades.transport.outbound.io.TradesClientHandler;
import drw.eeif.trades.transport.outbound.io.TradesTransportComponents;
import drw.eeif.trades.transport.outbound.messages.TradesTransportBaseMsg;
import org.jetlang.channels.BatchSubscriber;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Main {

    private static final int MD_SERVER_TIMEOUT = 5000;

    private static final long SERVER_TIMEOUT = 3000L;
    private static final long RECONNECT_INTERVAL_MILLIS = 10000;

    private static final String EWOK_BASE_URL_PARAM = "ewokBaseURL";
    private static final String IS_EQUITIES_SEARCHABLE_PARAM = "isEquitiesSearchable";
    private static final String IS_FUTURES_SEARCHABLE_PARAM = "isFuturesSearchable";

    private static final String IS_STACK_MANAGER_PARAM = "isManager";
    private static final String MD_SOURCES_PARAM = "mdSources";

    private static final String IS_FOR_TRADING_PARAM = "isTradable";
    private static final Pattern PARENT_STACK_SUFFIX = Pattern.compile(";S", Pattern.LITERAL);

    private static final String EEIF_OE = "eeifoe";

    public static void main(final String[] args) throws Exception {

        final Application<ReddalComponents> app =
                new Application<>(args, "Reddal", ReddalComponents.class, Constants::NO_OP, Constants::NO_OP, true);

        final IClock clock = app.clock;
        final ConfigGroup root = app.config;
        final Path logDir = app.logDir;
        final IErrorLogger errorLog = app.errorLog;
        final IFuseBox<ReddalComponents> monitor = app.monitor;

        final Environment environment = new Environment(root);

        final String localAppName = app.appName + ':' + app.env.name();

        final ReddalChannels channels = new ReddalChannels();
        final DefaultJetlangFactory jetlangFactory = new DefaultJetlangFactory(channels.error);

        final IFuseBox<SelectIOComponents> uiSelectIOMonitor =
                new ExpandedDetailResourceMonitor<>(app.monitor, "UI Select IO", app.errorLog, SelectIOComponents.class,
                        ReddalComponents.UI_SELECT_IO);
        final SelectIO uiSelectIO = new SelectIO(app.clock, uiSelectIOMonitor, Constants::NO_OP, Constants::NO_OP);
        final ReddalFibers fibers = new ReddalFibers(channels, jetlangFactory, uiSelectIO, app.errorLog);

        final IFuseBox<OPXLComponents> opxlMonitor =
                new ExpandedDetailResourceMonitor<>(app.monitor, "OPXL Select IO", app.errorLog, OPXLComponents.class,
                        ReddalComponents.OPXL_READERS);
        final IFuseBox<SelectIOComponents> opxlSelectIOMonitor =
                new ExpandedDetailResourceMonitor<>(opxlMonitor, "OPXL Select IO", app.errorLog, SelectIOComponents.class,
                        OPXLComponents.SELECT_IO);

        final SelectIO opxlSelectIO = new SelectIO(opxlSelectIOMonitor);
        app.addStartUpAction(() -> opxlSelectIO.start("OPXL Select IO"));

        final SelectIOFiber selectIOFiber = new SelectIOFiber(app.selectIO, errorLog, "Main Select IO Fiber");

        final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (t, e) -> channels.errorPublisher.publish(e);

        final Set<String> errorStates = new HashSet<>();
        channels.stats.subscribe(selectIOFiber, msg -> {
            if (StatusStat.State.GREEN == msg.getState()) {
                if (errorStates.remove(msg.getName()) && errorStates.isEmpty()) {
                    monitor.setOK(ReddalComponents.OLD_ERRORS);
                }
            } else if (errorStates.add(msg.getName())) {
                monitor.logError(ReddalComponents.OLD_ERRORS, "Error on [" + msg.getName() + "].");
            }
        });

        channels.error.subscribe(fibers.logging.getFiber(), message -> {
            System.out.print(new Date() + ": ");
            message.printStackTrace();
        });

        final int webPort = root.getGroup("web").getInt("port");
        final UILogger webLog = new UILogger(uiSelectIO, logDir);

        final boolean isEquitiesSearchable =
                !root.paramExists(IS_EQUITIES_SEARCHABLE_PARAM) || root.getBoolean(IS_EQUITIES_SEARCHABLE_PARAM);
        final boolean isFuturesSearchable = !root.paramExists(IS_FUTURES_SEARCHABLE_PARAM) || root.getBoolean(IS_FUTURES_SEARCHABLE_PARAM);

        final Map<String, TypedChannel<WebSocketControlMessage>> webSocketsForLogging = new HashMap<>();

        final WebApplication webApp = new WebApplication(webPort, channels.errorPublisher);
        System.out.println("http://localhost:" + webPort);
        webApp.enableSingleSignOn();
        webApp.protectContent("/", User.viewers());

        app.addStartUpAction(() -> fibers.ui.execute(() -> {
            try {
                webApp.serveStaticContent("web");
                webApp.start();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }));

        // Index presenter
        final TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("/", "index", fibers.ui, webApp, websocket);
        webSocketsForLogging.put("index", websocket);
        final IndexUIPresenter indexPresenter = new IndexUIPresenter(webLog, isEquitiesSearchable, isFuturesSearchable);
        fibers.ui.subscribe(indexPresenter, channels.displaySymbol, websocket);
        channels.searchResults.subscribe(fibers.ui.getFiber(), indexPresenter::addSearchResult);

        final FXCalc<?> stockAlertsFXCalc = createOPXLFXCalc(app, opxlSelectIO, uiSelectIO, opxlMonitor);
        { // Stock alert screen
            final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stockalerts", "stockalerts", fibers.ui, webApp, ws);

            final StockAlertPresenter presenter = new StockAlertPresenter(new SystemClock(), stockAlertsFXCalc, webLog);
            for (final Map.Entry<StackCommunity, TypedChannel<String>> entry : channels.communitySymbols.entrySet()) {
                entry.getValue().subscribe(fibers.ui.getFiber(), symbol -> {
                    final StackCommunity community = entry.getKey();
                    presenter.setCommunityForSymbol(symbol, community);
                });
            }
            fibers.ui.subscribe(presenter, ws);
            channels.stockAlerts.subscribe(fibers.ui.getFiber(), presenter::addAlert);
            channels.rfqStockAlerts.subscribe(fibers.ui.getFiber(), presenter::addRfq);
        }

        // TODO - this doesn't work as far as I can tell (ws paths are shared with above and they all seem to come in above anyway rite>) - must confirm but should remove.
        { // ETF RFQ screen
            final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("rfqs", "stockalerts", fibers.ui, webApp, ws);
            final StockAlertPresenter presenter = new StockAlertPresenter(new SystemClock(), stockAlertsFXCalc, webLog);
            fibers.ui.subscribe(presenter, ws);
            channels.rfqStockAlerts.subscribe(fibers.ui.getFiber(), presenter::addRfq);
        }

        final String ewokBaseURL = root.getString(EWOK_BASE_URL_PARAM);

        final MultiLayeredFuseBox<ReddalComponents> parentMonitor =
                new ConcurrentMultiLayeredFuseBox<>(monitor, ReddalComponents.class, errorLog);

        final String stackManagerThreadName = "Ladder-StackManager";

        final IFuseBox<ReddalComponents> stackManagerFuseBox = parentMonitor.createChildResourceMonitor(stackManagerThreadName);
        final IFuseBox<SelectIOComponents> stackManagerSelectIOMonitor =
                new ExpandedDetailResourceMonitor<>(stackManagerFuseBox, stackManagerThreadName, errorLog, SelectIOComponents.class,
                        ReddalComponents.UI_SELECT_IO);
        final SelectIO stackManagerSelectIO = new SelectIO(stackManagerSelectIOMonitor);
        final IMDSubscriber noBookSubscription = new NoMDSubscriptions();
        final TypedChannel<WebSocketControlMessage> stackManagerWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        final SelectIOFiber stackManagerSelectIOFiber = new SelectIOFiber(stackManagerSelectIO, errorLog, stackManagerThreadName);

        final FXCalc<?> stackManagerFXCalc = createOPXLFXCalc(app, opxlSelectIO, stackManagerSelectIO, opxlMonitor);
        final FiberBuilder stackManagerFiberBuilder = fibers.fiberGroup.wrap(stackManagerSelectIOFiber, stackManagerThreadName);
        final LadderPresenter stackManagerLadderPresenter =
                getLadderPresenter(stackManagerFuseBox, stackManagerSelectIO, channels, environment, stackManagerFXCalc, noBookSubscription,
                        ewokBaseURL, stackManagerWebSocket, stackManagerFiberBuilder, Constants::NO_OP, Constants::NO_OP);

        // Load stacks
        final Map<MDSource, LinkedList<ConfigGroup>> stackConfigs = new EnumMap<>(MDSource.class);

        final ConfigGroup stackConfig = root.getEnabledGroup("stacks", "nibblers");
        if (null != stackConfig) {

            final MultiLayeredFuseBox<StackTransportComponents> stackParentMonitor =
                    MultiLayeredFuseBox.getExpandedMultiLayerMonitor(stackManagerFuseBox, "Stacks", errorLog,
                            StackTransportComponents.class, ReddalComponents.STACK_GROUP_CLIENT);

            for (final ConfigGroup stackClientConfig : stackConfig.groups()) {

                final boolean isStackManager =
                        stackClientConfig.paramExists(IS_STACK_MANAGER_PARAM) && stackClientConfig.getBoolean(IS_STACK_MANAGER_PARAM);

                if (isStackManager) {

                    final StackManagerGroupCallbackBatcher stackUpdateBatcher =
                            new StackManagerGroupCallbackBatcher(stackManagerLadderPresenter, channels.stackParentSymbolPublisher);
                    createStackClient(stackParentMonitor, stackManagerSelectIO, stackManagerThreadName, stackClientConfig,
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

        final OpxlClient<OPXLComponents> opxlClient = new OpxlClient<>(opxlSelectIO, opxlMonitor, OPXLComponents.OPXL_WRITER_CLIENT);
        app.addStartUpAction(opxlClient::start);

        final Set<StackCommunity> primaryCommunities = app.config.getGroup("stacks").getEnumSet("primaryCommunities", StackCommunity.class);

        setupStackManager(app, fibers, channels, webApp, webLog, selectIOFiber, opxlSelectIO, opxlMonitor, opxlClient, contractSetGenerator,
                isEquitiesSearchable, primaryCommunities);
        final Map<MDSource, LinkedList<ConfigGroup>> nibblers =
                setupBackgroundNibblerTransport(app, opxlSelectIO, opxlMonitor, opxlClient, fibers, webApp, webLog, selectIOFiber, channels,
                        EXCEPTION_HANDLER, primaryCommunities);

        final Map<MDSource, TypedChannel<WebSocketControlMessage>> ladderWebSockets = new EnumMap<>(MDSource.class);
        final Map<MDSource, TypedChannel<WebSocketControlMessage>> orderWebSockets = new EnumMap<>(MDSource.class);
        final Map<MDSource, TypedChannel<WebSocketControlMessage>> shredderWebSockets = new EnumMap<>(MDSource.class);

        setupPicardUI(app.selectIO, selectIOFiber, webLog, channels.picardRows, channels.yodaPicardRows, channels.recenterLadder,
                channels.displaySymbol, channels.runnableInfo, webApp, channels.communitySymbols, channels.picardDMFilterSymbols);

        new OPXLPicardFilterReader(opxlClient, app.selectIO, opxlMonitor, com.drwtrading.london.eeif.utils.application.Environment.PROD,
                "EEIF", app.logDir, channels.picardDMFilterSymbols);

        setupLaserDistancesUI(app.selectIO, selectIOFiber, webLog, channels.laserDistances, channels.displaySymbol, webApp);

        final ConfigGroup premiumConfig = root.getEnabledGroup("premiumOPXL");
        if (null != premiumConfig) {
            final PremiumOPXLWriter writer = new PremiumOPXLWriter(opxlSelectIO, premiumConfig, opxlMonitor);
            channels.spreadnoughtPremiums.subscribe(selectIOFiber, writer::onPremium);
            app.selectIO.addDelayedAction(1000, writer::flush);
        }

        // Auto-puller
        final Path dataDir = app.persistenceDir.resolve("autopull.json");
        final AutoPullPersistence persistence = new AutoPullPersistence(dataDir);
        final AutoPullerUI autoPullerUI = new AutoPullerUI(persistence, channels.autoPullerCmds);
        channels.autoPullerUpdates.subscribe(fibers.ui.getFiber(), update -> update.executeOn(autoPullerUI));
        uiSelectIO.execute(() -> autoPullerUI.start(uiSelectIO));

        final TypedChannel<WebSocketControlMessage> autoPullerWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("autopuller", "autopuller", fibers.ui, webApp, autoPullerWebSocket);
        fibers.ui.subscribe(autoPullerUI, autoPullerWebSocket);

        final TypedChannel<MrChillTrade> jasperTradesChan = TypedChannels.create(MrChillTrade.class);
        initJasperTradesPublisher(app, errorLog, parentMonitor, jasperTradesChan);

        final ConfigGroup additiveOffsetConfig = root.getEnabledGroup("additiveOffset");

        // MD Sources
        final ConfigGroup mdConfig = root.getGroup("md");
        for (final ConfigGroup mdSourceGroup : mdConfig.groups()) {

            final MDSource mdSource = MDSource.get(mdSourceGroup.getKey());
            if (null == mdSource) {
                throw new ConfigException("MDSource [" + mdSourceGroup.getKey() + "] is not known.");
            } else {

                final String threadName = "Ladder-" + mdSource.name();

                final IFuseBox<ReddalComponents> displayFuseBox = parentMonitor.createChildResourceMonitor(threadName);
                final IFuseBox<SelectIOComponents> selectIOFuseBox =
                        new ExpandedDetailResourceMonitor<>(displayFuseBox, threadName, errorLog, SelectIOComponents.class,
                                ReddalComponents.UI_SELECT_IO);

                final SelectIO displaySelectIO = new SelectIO(selectIOFuseBox);

                final IMDSubscriber depthBookSubscriber =
                        getMDSubscription(app, displayFuseBox, displaySelectIO, mdSource, mdSourceGroup, channels, localAppName,
                                channels.rfqStockAlerts);

                final TypedChannel<WebSocketControlMessage> ladderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
                ladderWebSockets.put(mdSource, ladderWebSocket);

                final TypedChannel<WebSocketControlMessage> orderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
                orderWebSockets.put(mdSource, orderWebSocket);

                final TypedChannel<WebSocketControlMessage> shredderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
                shredderWebSockets.put(mdSource, shredderWebSocket);

                final SelectIOFiber displaySelectIOFiber = new SelectIOFiber(displaySelectIO, errorLog, threadName);
                final FiberBuilder fiberBuilder = fibers.fiberGroup.wrap(displaySelectIOFiber, threadName);

                final FXCalc<?> fxCalc = createOPXLFXCalc(app, opxlSelectIO, stackManagerSelectIO, opxlMonitor);

                final PicardSpotter picardSpotter =
                        new PicardSpotter(displaySelectIO, depthBookSubscriber, channels.picardRows, channels.laserDistances, fxCalc);
                displaySelectIO.addDelayedAction(1000, picardSpotter::checkAnyCrossed);

                final PremiumCalculator premiumCalc = new PremiumCalculator(depthBookSubscriber, channels.spreadnoughtPremiums);
                displaySelectIO.addDelayedAction(1000, premiumCalc::recalcAll);

                final LadderPresenter ladderPresenter =
                        getLadderPresenter(displayFuseBox, displaySelectIO, channels, environment, fxCalc, depthBookSubscriber, ewokBaseURL,
                                ladderWebSocket, fiberBuilder, picardSpotter, premiumCalc);

                final OrdersPresenter orderPresenter = new OrdersPresenter(channels.singleOrderCommand, channels.orderEntryCommandToServer);
                fiberBuilder.subscribe(orderPresenter, orderWebSocket);

                channels.orderEntryFromServer.subscribe(fiberBuilder.getFiber(), orderPresenter::setOrderEntryUpdate);

                final ShredderPresenter shredderPresenter = new ShredderPresenter(depthBookSubscriber);
                fiberBuilder.subscribe(shredderPresenter, shredderWebSocket);

                channels.laserLineData.subscribe(fiberBuilder.getFiber(), shredderPresenter::overrideLaserLine);
                displaySelectIO.addDelayedAction(1000, shredderPresenter::flushAllShredders);
                displaySelectIO.addDelayedAction(1500, shredderPresenter::sendAllHeartbeats);

                final List<ConfigGroup> mdSourceStackConfigs = stackConfigs.get(mdSource);
                if (null != mdSourceStackConfigs) {

                    final MultiLayeredFuseBox<StackTransportComponents> stackParentMonitor =
                            MultiLayeredFuseBox.getExpandedMultiLayerMonitor(displayFuseBox, "Stacks", errorLog,
                                    StackTransportComponents.class, ReddalComponents.STACK_GROUP_CLIENT);

                    final IStackPresenterCallback presenterSharer = new StackPresenterMultiplexor(ladderPresenter, shredderPresenter);

                    for (final ConfigGroup stackClientConfig : mdSourceStackConfigs) {

                        final StackGroupCallbackBatcher stackUpdateBatcher = new StackGroupCallbackBatcher(presenterSharer);
                        createStackClient(stackParentMonitor, displaySelectIO, threadName, stackClientConfig, stackUpdateBatcher,
                                localAppName);
                    }

                    if (null != additiveOffsetConfig) {

                        final AdditiveOffsetListener additiveListener = new AdditiveOffsetListener(ladderPresenter, shredderPresenter);

                        final String connectionName = threadName + "-roci";

                        final IFuseBox<AdditiveTransportFuses> transportFuseBox =
                                new ExpandedDetailResourceMonitor<>(displayFuseBox, connectionName, errorLog, AdditiveTransportFuses.class,
                                        ReddalComponents.ROCI);

                        final AdditiveClientHandler clientHandler =
                                AdditiveCacheFactory.createClientCache(displaySelectIO, transportFuseBox, "Roci", threadName,
                                        additiveListener);
                        final TransportTCPKeepAliveConnection<?, ?> connection =
                                AdditiveCacheFactory.createClient(displaySelectIO, additiveOffsetConfig, transportFuseBox, clientHandler);

                        displaySelectIO.execute(connection::restart);
                    }
                }

                final List<ConfigGroup> nibblerConfigs = nibblers.get(mdSource);
                if (null != nibblerConfigs) {

                    final IFuseBox<NibblerTransportComponents> nibblerMonitor =
                            new ExpandedDetailResourceMonitor<>(displayFuseBox, threadName + "-Nibblers", errorLog,
                                    NibblerTransportComponents.class, ReddalComponents.TRADING_DATA);
                    final MultiLayeredFuseBox<NibblerTransportComponents> nibblerParentMonitor =
                            new MultiLayeredFuseBox<>(nibblerMonitor, NibblerTransportComponents.class, errorLog);

                    final AutoPuller autoPuller =
                            new AutoPuller(mdSource, depthBookSubscriber, channels.cmdsForNibblers, channels.ladderClickTradingIssues,
                                    channels.autoPullerUpdates);
                    channels.autoPullerCmds.subscribe(fiberBuilder.getFiber(), cmd -> cmd.executeOn(autoPuller));

                    final BulkOrderMarketAnalyser bulkOrderMarketAnalyser =
                            new BulkOrderMarketAnalyser(mdSource, depthBookSubscriber, channels.gtcBettermentResponses);
                    channels.gtcBettermentRequests.subscribe(fiberBuilder.getFiber(), bulkOrderMarketAnalyser::checkForBettermentPrices);

                    if (null != root.getEnabledGroup("divImpliedTheo")) {
                        final ImpliedMDInfoGenerator impliedMDGenerator =
                                new ImpliedMDInfoGenerator(clock, depthBookSubscriber, ladderPresenter, shredderPresenter);
                        channels.searchResults.subscribe(displaySelectIOFiber, impliedMDGenerator::addInstrument);
                    }

                    for (final ConfigGroup nibblerConfig : nibblerConfigs) {

                        final String sourceNibbler = nibblerConfig.getKey();

                        final IFuseBox<NibblerTransportComponents> childMonitor =
                                nibblerParentMonitor.createChildResourceMonitor(sourceNibbler);

                        final LadderInfoListener ladderInfoListener =
                                new LadderInfoListener(sourceNibbler, ladderPresenter, orderPresenter, shredderPresenter, autoPuller,
                                        channels.supportedGTCSymbols);

                        final NibblerClientHandler client =
                                NibblerCacheFactory.createClientCache(displaySelectIO, nibblerConfig, childMonitor,
                                        threadName + "-transport-" + sourceNibbler, localAppName + mdSource.name(), true, true,
                                        ladderInfoListener, NibblerNotificationHandler.INSTANCE);

                        client.getCaches().addTradingDataListener(ladderInfoListener, true, true);
                        client.getCaches().blotterCache.addListener(ladderInfoListener);
                    }
                    jasperTradesChan.subscribe(displaySelectIOFiber, ladderPresenter::setLastTradeForJasper);
                }
            }
        }

        // FX view
        final ConfigGroup fxConfig = root.getEnabledGroup("fx");
        if (null != fxConfig) {

            final String name = "FX UI";
            final IFuseBox<FXFuse> fxFuseBox =
                    new ExpandedDetailResourceMonitor<>(monitor, "FX", errorLog, FXFuse.class, ReddalComponents.FX);
            final IFuseBox<SelectIOComponents> selectIOFuseBox =
                    new ExpandedDetailResourceMonitor<>(fxFuseBox, name, errorLog, SelectIOComponents.class, FXFuse.FX_SELECT_IO);

            final SelectIO fxSelectIO = new SelectIO(selectIOFuseBox);

            final FXCalc<FXFuse> fxCalc =
                    FXMDUtils.createMDFXCalc(fxSelectIO, fxFuseBox, errorLog, fxConfig, FXFuse.FX_CALC, FXFuse.FX_HANDLER, FXFuse.FX_MD,
                            Constants::NO_OP, app.appName);

            final ConfigGroup fxMDConfig = fxConfig.getGroup("md");

            final SelectIOFiber fxFiber = new SelectIOFiber(fxSelectIO, errorLog, "FX SelectIO");
            final FiberBuilder fiberBuilder = fibers.fiberGroup.wrap(fxFiber, name);

            final TypedChannel<WebSocketControlMessage> ws = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("fx", "fx", fiberBuilder, webApp, ws);
            final FxUi ui = new FxUi(fxCalc);
            fiberBuilder.subscribe(ui, ws);
        }

        final ChixInstMatcher chixInstMatcher = new ChixInstMatcher(channels.chixSymbolPairs);
        channels.searchResults.subscribe(selectIOFiber, chixInstMatcher::setSearchResult);

        final TypedChannel<WebSocketControlMessage> historyWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("history", "history", fibers.ladderRouter, webApp, historyWebSocket);
        final HistoryPresenter historyPresenter = new HistoryPresenter(webLog);
        fibers.ladderRouter.subscribe(historyPresenter, historyWebSocket);
        channels.symbolSelections.subscribe(fibers.ladderRouter.getFiber(), historyPresenter::addSymbol);

        // Ladder router
        final TypedChannel<WebSocketControlMessage> ladderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("ladder", "ladder", fibers.ladderRouter, webApp, ladderWebSocket);
        final LadderMessageRouter ladderMessageRouter =
                new LadderMessageRouter(monitor, webLog, channels.symbolSelections, stackManagerWebSocket, ladderWebSockets);
        fibers.ladderRouter.subscribe(ladderMessageRouter, ladderWebSocket, channels.replaceCommand);
        channels.searchResults.subscribe(fibers.ladderRouter.getFiber(), ladderMessageRouter::setSearchResult);
        channels.stackParentSymbolPublisher.subscribe(fibers.ladderRouter.getFiber(), ladderMessageRouter::setParentStackSymbol);

        // Orders router
        final TypedChannel<WebSocketControlMessage> orderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("orders", "orders", fibers.ladderRouter, webApp, orderWebSocket);
        final OrderPresenterMsgRouter ordersPresenterMsgRouter = new OrderPresenterMsgRouter(monitor, webLog, orderWebSockets);
        fibers.ladderRouter.subscribe(ordersPresenterMsgRouter, orderWebSocket);
        channels.searchResults.subscribe(fibers.ladderRouter.getFiber(), ordersPresenterMsgRouter::setSearchResult);

        // Shredder router
        final TypedChannel<WebSocketControlMessage> shredderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
        createWebPageWithWebSocket("shredder", "shredder", fibers.ladderRouter, webApp, shredderWebSocket);
        final ShredderMessageRouter shredderMessageRouter = new ShredderMessageRouter(monitor, webLog, shredderWebSockets);
        fibers.ladderRouter.subscribe(shredderMessageRouter, shredderWebSocket);
        channels.searchResults.subscribe(fibers.ladderRouter.getFiber(), shredderMessageRouter::setSearchResult);

        // Non SSO-protected web App to allow AJAX requests
        final WebApplication nonSSOWebapp = new WebApplication(webPort + 1, channels.errorPublisher);

        app.addStartUpAction(() -> fibers.ui.execute(() -> {
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
        final WorkspaceRequestHandler httpHandler =
                new WorkspaceRequestHandler(ladderWorkspace, new URI(nonSSOWebapp.getBaseUri()).getHost(), webPort);
        nonSSOWebapp.addHandler("/open", httpHandler);

        // Settings
        final Path settingsFile = app.persistenceDir.resolve("settings.json");
        final LadderSettings ladderSettings = new LadderSettings(settingsFile, channels.ladderPrefsLoaded);
        fibers.settings.subscribe(ladderSettings, channels.storeLadderPref);
        app.addStartUpAction(() -> fibers.settings.execute(ladderSettings::load));

        final ConfigGroup oeCmdConfig = root.getEnabledGroup("eeifoeCommand");
        final ConfigGroup oeUpdateConfig = root.getEnabledGroup("eeifoeUpdate");

        final Collection<String> oeList = environment.getList(EEIF_OE);
        for (final String server : oeList) {

            final ConfigGroup eeifOEGroup = root.getGroup("eeifoe");
            final String instanceName = eeifOEGroup.getString("instance");
            final OrderEntryClient client = new OrderEntryClient(instanceName, new SystemClock(), server, fibers.remoteOrders.getFiber(),
                    channels.orderEntrySymbols, channels.ladderClickTradingIssues);

            final ConfigGroup commandConfig = oeCmdConfig.getEnabledGroup(server);
            if (null != commandConfig) {

                final HostAndNic command = Environment.getHostAndNic(commandConfig);
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
            }

            final ConfigGroup updateConfig = oeUpdateConfig.getEnabledGroup(server);
            if (null != updateConfig) {

                final HostAndNic update = Environment.getHostAndNic(updateConfig);
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

        final ConfigGroup metaDataConfig = root.getEnabledGroup("metadata");
        if (null != metaDataConfig) {

            final String statsName = metaDataConfig.getString("name");
            final HostAndNic hostAndNic = Environment.getHostAndNic(metaDataConfig);
            final OnHeapBufferPhotocolsNioClient<LadderMetadata, Void> client =
                    OnHeapBufferPhotocolsNioClient.client(hostAndNic.host, NetworkInterfaces.find(hostAndNic.nic), LadderMetadata.class,
                            Void.class, fibers.metaData.getFiber(), EXCEPTION_HANDLER);
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS);
            client.logFile(logDir.resolve("ladderText.risk.log").toFile(), fibers.logging.getFiber(), true);
            client.handler(new PhotocolsStatsPublisher<>(channels.stats, statsName, 10));
            client.handler(new JetlangChannelHandler<>(channels.metaData));
            app.addStartUpAction(client::start);
        }

        // Mr. Phil position
        final ConfigGroup mrChillPositionsConfig = root.getEnabledGroup("mrchill-positions");
        if (null != mrChillPositionsConfig) {
            final InetSocketAddress address = IOConfigParser.getTargetAddress(mrChillPositionsConfig);
            final OnHeapBufferPhotocolsNioClient<Position, Void> client =
                    OnHeapBufferPhotocolsNioClient.client(address, "0.0.0.0", Position.class, Void.class, fibers.mrPhil.getFiber(),
                            EXCEPTION_HANDLER);

            final PositionPhotocolsHandler positionHandler = new PositionPhotocolsHandler(channels.position);
            final File logFile = logDir.resolve("mr-phil.log").toFile();
            client.reconnectMillis(RECONNECT_INTERVAL_MILLIS).logFile(logFile, fibers.logging.getFiber(), true).handler(positionHandler);
            app.addStartUpAction(client::start);
        }

        // Display symbols
        final DisplaySymbolMapper displaySymbolMapper = new DisplaySymbolMapper(channels.displaySymbol);
        channels.instDefs.subscribe(fibers.indy.getFiber(), displaySymbolMapper::setInstDef);
        channels.searchResults.subscribe(fibers.indy.getFiber(), displaySymbolMapper::setSearchResult);
        channels.ultimateParents.subscribe(fibers.indy.getFiber(), displaySymbolMapper::setUltimateParent);

        // Indy
        final ConfigGroup indyConfig = root.getGroup("indy");
        final IndyClient indyListener = new IndyClient(channels.instDefs, channels.etfDefs, channels.symbolDescs);

        final RFQCommunityPublisher rfqSymbolCommunityPublisher = new RFQCommunityPublisher(channels.communitySymbols);
        channels.etfDefs.subscribe(selectIOFiber, rfqSymbolCommunityPublisher::setCommunityFromETF);
        channels.searchResults.subscribe(selectIOFiber, rfqSymbolCommunityPublisher::setSearchResult);

        final String indyUsername = indyConfig.getString("username");
        final IFuseBox<IndyTransportComponents> indyMonitor =
                new ExpandedDetailResourceMonitor<>(monitor, "Indy", errorLog, IndyTransportComponents.class, ReddalComponents.INDY);
        final TransportTCPKeepAliveConnection<?, ?> indyConnection =
                IndyCacheFactory.createClient(app.selectIO, indyConfig, indyMonitor, indyUsername, true, indyListener);
        app.selectIO.execute(indyConnection::restart);

        setupYodaSignals(app.selectIO, monitor, errorLog, root, app.appName, channels.stockAlerts, channels.yodaPicardRows);
        setupSignals(app, channels.yodaPicardRows, channels.stockAlerts);

        final ConfigGroup opxlConfig = root.getEnabledGroup("opxl");
        final ConfigGroup icepieConfig = app.config.getEnabledGroup("icepie");

        if (null != opxlConfig) {
            // Desk Position
            final ConfigGroup deskPositionConfig = opxlConfig.getEnabledGroup("deskposition");
            if (null != deskPositionConfig) {

                final Set<String> keys = deskPositionConfig.getSet("keys");
                final OpxlPositionSubscriber opxlReader =
                        new OpxlPositionSubscriber(opxlSelectIO, opxlMonitor, keys, channels.deskPositions);
                app.addStartUpAction(opxlReader::start);
            }
        }

        if (null != icepieConfig) {
            initialiseIcePieClient(app, channels);
        } else {
            app.errorLog.error("No ladderText being subscribed to");
        }

        final ConfigGroup pksConfig = root.getEnabledGroup("pks");
        if (null != pksConfig) {

            final IFuseBox<PositionTransportComponents> pksMonitor =
                    new ExpandedDetailResourceMonitor<>(monitor, "PKS", errorLog, PositionTransportComponents.class, ReddalComponents.PKS);

            final PKSPositionClient pksClient = new PKSPositionClient(channels.pksExposures);
            channels.ultimateParents.subscribe(selectIOFiber, pksClient::setUltimateParent);
            channels.searchResults.subscribe(selectIOFiber, pksClient::setSearchResult);

            final ConfigGroup dryConfig = pksConfig.getEnabledGroup("dry");
            final ConfigGroup dripConfig = pksConfig.getEnabledGroup("drip");

            if (dryConfig != null) {
                final PKSPositionClient.DryPksClient dryClient = pksClient.getDryClient();

                final PositionClientHandler dryPositionCache =
                        PositionCacheFactory.createClientCache(app.selectIO, pksMonitor, "PKS", app.appName, dryClient);
                dryPositionCache.addConstituentListener(dryClient);

                final TransportTCPKeepAliveConnection<?, ?> client =
                        PositionCacheFactory.createClient(app.selectIO, dryConfig, pksMonitor, dryPositionCache);
                client.restart();
            }

            if (dripConfig != null) {
                final PKSPositionClient.DripPksClient dripClient = pksClient.getDripClient();

                final PositionClientHandler dripPositionCache =
                        PositionCacheFactory.createClientCache(app.selectIO, pksMonitor, "PKS", app.appName, dripClient);
                dripPositionCache.addConstituentListener(dripClient);

                final TransportTCPKeepAliveConnection<?, ?> client =
                        PositionCacheFactory.createClient(app.selectIO, dripConfig, pksMonitor, dripPositionCache);
                client.restart();

            }

        }

        final UltimateParentOPXL ultimateParentOPXL = new UltimateParentOPXL(opxlSelectIO, opxlMonitor, logDir, channels.ultimateParents);
        app.addStartUpAction(ultimateParentOPXL::start);

        // Ex-dates
        final OpxlExDateSubscriber isinsGoingEx = new OpxlExDateSubscriber(opxlSelectIO, opxlMonitor, logDir, channels.isinsGoingEx);
        app.addStartUpAction(isinsGoingEx::start);

        final OpxlShortSensitiveIsinsSubscriber shortSensitiveIsinsSubscriber =
                new OpxlShortSensitiveIsinsSubscriber(opxlSelectIO, opxlMonitor, logDir, channels.shortSensitiveIsins);
        app.addStartUpAction(shortSensitiveIsinsSubscriber::start);

        if (isFuturesSearchable) {
            final OpxlDividendTweets divTweets = new OpxlDividendTweets(opxlSelectIO, opxlMonitor, logDir, channels.stockAlerts);
            app.addStartUpAction(divTweets::start);
        }

        // Logging
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "trading-status.json", channels.errorPublisher),
                channels.nibblerTransportConnected);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "preferences.json", channels.errorPublisher),
                channels.ladderPrefsLoaded, channels.storeLadderPref);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "status.json", channels.errorPublisher), channels.stats);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "heartbeats.json", channels.errorPublisher),
                channels.heartbeatRoundTrips);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "contracts.json", channels.errorPublisher), channels.contractSets);
        fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "single-order.json", channels.errorPublisher),
                channels.singleOrderCommand);
        fibers.logging.subscribe(new FileLogger(clock, logDir, "stockAlerts.json", channels.errorPublisher), channels.stockAlerts);
        fibers.logging.subscribe(new FileLogger(clock, logDir, "rfqStockAlerts.json", channels.errorPublisher), channels.rfqStockAlerts);
        fibers.logging.subscribe(new FileLogger(clock, logDir, "picardSpots.json", channels.errorPublisher), channels.picardRows);

        for (final Map.Entry<String, TypedChannel<WebSocketControlMessage>> stringTypedChannelEntry : webSocketsForLogging.entrySet()) {
            fibers.logging.subscribe(new JsonChannelLogger(logDir.toFile(), "websocket" + stringTypedChannelEntry.getKey() + ".json",
                    channels.errorPublisher), stringTypedChannelEntry.getValue());
        }

        app.addStartUpAction(fibers::start);
        app.run();
    }

    private static void initialiseIcePieClient(final Application<ReddalComponents> app, final ReddalChannels channels)
            throws ConfigException {
        final IFuseBox<IcePieTransportComponents> icePieMonitor =
                new ExpandedDetailResourceMonitor<>(app.monitor, "IcePie", app.errorLog, IcePieTransportComponents.class,
                        ReddalComponents.ICE_PIE);

        final TransportCache<?, String, LadderTextFreeText> freeTextCache =
                new TransportCache<>(icePieMonitor, IcePieTransportComponents.FREE_TEXT_CACHE);
        final TransportCache<?, String, LadderTextNumber> numberCache =
                new TransportCache<>(icePieMonitor, IcePieTransportComponents.NUMBER_CACHE);
        final TransportCache<?, String, LaserLineValue> laserLineCache =
                new TransportCache<>(icePieMonitor, IcePieTransportComponents.LASER_LINE_CACHE);

        final TransportTCPKeepAliveConnection<?, ?> client =
                IcePieCacheFactory.createClient(app.selectIO, app.config.getGroup("icepie"), icePieMonitor, freeTextCache, numberCache,
                        laserLineCache, app.appName);

        final ITransportCacheListener<String, LadderTextFreeText> textListener = new FreeTextCacheListener(channels.ladderText);
        freeTextCache.addListener(textListener, true);
        final ITransportCacheListener<String, LadderTextNumber> numberListener = new LadderTextNumberCacheListener(channels.ladderNumber);
        numberCache.addListener(numberListener, true);

        final ITransportCacheListener<String, LaserLineValue> laserLineListener = new LaserLineCacheListener(channels.laserLineData);
        laserLineCache.addListener(laserLineListener, true);

        app.addStartUpAction(client::restart);
    }

    private static void initJasperTradesPublisher(final Application<ReddalComponents> app, final IErrorLogger errorLog,
            final MultiLayeredFuseBox<ReddalComponents> parentMonitor, final TypedChannel<MrChillTrade> jasperTradesChan)
            throws IOException, ConfigException {
        final JasperTradesListener jasperTradesPublisher = new JasperTradesListener(jasperTradesChan);
        final String mrChillThreadName = "MrChill-JasperTrades";
        final IFuseBox<ReddalComponents> displayMonitor = parentMonitor.createChildResourceMonitor(mrChillThreadName);
        final IFuseBox<SelectIOComponents> selectIOMonitor =
                new ExpandedDetailResourceMonitor<>(displayMonitor, mrChillThreadName, errorLog, SelectIOComponents.class,
                        ReddalComponents.UI_SELECT_IO);
        final ConfigGroup mrChillConfig = app.config.getGroup("mrchill");
        final SelectIO mrChillSelectIO = new SelectIO(selectIOMonitor);

        final IFuseBox<TradesTransportComponents> tradesMonitor =
                new ExpandedDetailResourceMonitor<>(app.monitor, "Chill Trades", errorLog, TradesTransportComponents.class,
                        ReddalComponents.MR_CHILL_TRADES);
        final TradesClientHandler cache =
                TradesClientFactory.createClientCache(app.appName, EnumSet.allOf(AccountGroup.class), EnumSet.allOf(TradingEntity.class),
                        true, false, mrChillSelectIO, tradesMonitor);
        final TransportTCPKeepAliveConnection<TradesTransportComponents, TradesTransportBaseMsg> tradesClient =
                TradesClientFactory.createClient(mrChillSelectIO, mrChillConfig, tradesMonitor, cache);

        mrChillSelectIO.execute(tradesClient::restart);
        cache.addTradesListener(jasperTradesPublisher);
        app.addStartUpAction(() -> mrChillSelectIO.start(mrChillThreadName));
    }

    private static DepthBookSubscriber getMDSubscription(final Application<?> app, final IFuseBox<ReddalComponents> displayMonitor,
            final SelectIO displaySelectIO, final MDSource mdSource, final ConfigGroup mdConfig, final ReddalChannels channels,
            final String localAppName, final Publisher<RfqAlert> rfqAlerts) throws ConfigException {

        final LevelThreeBookSubscriber l3BookHandler =
                new LevelThreeBookSubscriber(displayMonitor, channels.searchResults, channels.symbolRefPrices, rfqAlerts);
        final LevelTwoBookSubscriber l2BookHandler =
                new LevelTwoBookSubscriber(displayMonitor, channels.searchResults, channels.symbolRefPrices, rfqAlerts);

        final IFuseBox<MDTransportComponents> mdClientMonitor =
                new ExpandedDetailResourceMonitor<>(displayMonitor, mdSource.name() + "-Thread", app.errorLog, MDTransportComponents.class,
                        ReddalComponents.MD_TRANSPORT);

        final MDTransportClient mdClient =
                new ReddalMDTransportClient(displaySelectIO, mdClientMonitor, mdSource, localAppName, l3BookHandler, l2BookHandler,
                        MD_SERVER_TIMEOUT, true);
        l3BookHandler.setMDClient(mdSource, mdClient);
        l2BookHandler.setMDClient(mdSource, mdClient);

        final TransportTCPKeepAliveConnection<?, ?> connection =
                MDTransportClientFactory.createConnection(displaySelectIO, mdConfig, mdClientMonitor, mdClient);

        app.addStartUpAction(() -> displaySelectIO.execute(connection::restart));
        return new DepthBookSubscriber(l3BookHandler, l2BookHandler);
    }

    private static LadderPresenter getLadderPresenter(final IFuseBox<ReddalComponents> displayMonitor, final SelectIO displaySelectIO,
            final ReddalChannels channels, final Environment environment, final FXCalc<?> fxCalc, final IMDSubscriber depthBookSubscriber,
            final String ewokBaseURL, final TypedChannel<WebSocketControlMessage> webSocket, final FiberBuilder fiberBuilder,
            final IPicardSpotter picardSpotter, final IPremiumCalc premiumCalc) throws ConfigException {

        final LadderPresenter ladderPresenter =
                new LadderPresenter(displayMonitor, depthBookSubscriber, ewokBaseURL, channels.cmdsForNibblers, environment.ladderOptions(),
                        picardSpotter, premiumCalc, fxCalc, channels.storeLadderPref, channels.heartbeatRoundTrips,
                        channels.recenterLaddersForUser, fiberBuilder.getFiber(), channels.increaseParentOffsetCmds,
                        channels.increaseChildOffsetBPSCmds, channels.setSiblingsEnabledCmds, channels.ladderClickTradingIssues,
                        channels.userCycleContractPublisher, channels.userPriceModeRequestPublisher, channels.orderEntryCommandToServer,
                        channels.userWorkspaceRequests);

        fiberBuilder.subscribe(ladderPresenter, webSocket, channels.metaData, channels.position, channels.ladderPrefsLoaded,
                channels.displaySymbol, channels.recenterLaddersForUser, channels.contractSets, channels.chixSymbolPairs,
                channels.singleOrderCommand, channels.replaceCommand, channels.userCycleContractPublisher, channels.orderEntrySymbols,
                channels.orderEntryFromServer, channels.searchResults, channels.symbolDescs);
        channels.userPriceModeRequestPublisher.subscribe(fiberBuilder.getFiber(), ladderPresenter::onUserPriceModeRequest);

        channels.nibblerTransportConnected.subscribe(fiberBuilder.getFiber(), ladderPresenter::setNibblerConnected);
        channels.ladderText.subscribe(displaySelectIO, ladderPresenter::setLadderText);
        channels.ladderNumber.subscribe(displaySelectIO, ladderPresenter::setLadderNumber);
        channels.isinsGoingEx.subscribe(fiberBuilder.getFiber(), ladderPresenter::setISINsGoingEx);
        channels.shortSensitiveIsins.subscribe(displaySelectIO, ladderPresenter::setShortSensitiveIsins);

        channels.laserLineData.subscribe(fiberBuilder.getFiber(), ladderPresenter::overrideLaserLine);
        channels.ladderClickTradingIssues.subscribe(fiberBuilder.getFiber(), ladderPresenter::displayTradeIssue);
        channels.deskPositions.subscribe(fiberBuilder.getFiber(), ladderPresenter::setDeskPositions);
        channels.pksExposures.subscribe(fiberBuilder.getFiber(), ladderPresenter::setPKSExposures);
        channels.recenterLadder.subscribe(fiberBuilder.getFiber(), ladderPresenter::recenterLadder);

        displaySelectIO.addDelayedAction(1000, ladderPresenter::flushAllLadders);
        displaySelectIO.addDelayedAction(1500, ladderPresenter::sendAllHeartbeats);

        return ladderPresenter;
    }

    private static void createStackClient(final MultiLayeredFuseBox<StackTransportComponents> fuseBox, final SelectIO displaySelectIO,
            final String name, final ConfigGroup stackConfig, final StackGroupCallbackBatcher stackUpdateBatcher, final String localAppName)
            throws ConfigException {

        final String connectionName = name + "-stack-" + stackConfig.getKey();
        final IFuseBox<StackTransportComponents> stackMonitor = fuseBox.createChildResourceMonitor(connectionName);

        final StackClientHandler client =
                StackCacheFactory.createClientCache(displaySelectIO, stackConfig, stackMonitor, connectionName, localAppName,
                        stackUpdateBatcher);
        stackUpdateBatcher.setStackClient(client);
    }

    private static void createWebPageWithWebSocket(final String alias, final String name, final FiberBuilder fiber,
            final WebApplication webapp, final TypedChannel<WebSocketControlMessage> websocketChannel) {

        webapp.alias('/' + alias, '/' + name + ".html");
        webapp.createWebSocket('/' + name + "/ws/", websocketChannel, fiber.getFiber());

    }

    static void setupSignals(final Application<ReddalComponents> app, final Publisher<PicardRow> atClosePublisher,
            final Publisher<StockAlert> stockAlerts) throws ConfigException {
        final ConfigGroup signalConfig = app.config.getEnabledGroup("signals");
        if (null == signalConfig) {
            return;
        }
        final PhocketClient<Signals, Void> client = Phockets.client(app, signalConfig, Signals.class, Void.class, "Signals",
                new SignalsHandler(atClosePublisher, stockAlerts, app.clock));
        app.addStartUpAction(client::restart);
    }

    private static void setupYodaSignals(final SelectIO selectIO, final IFuseBox<ReddalComponents> monitor, final IErrorLogger errorLog,
            final ConfigGroup config, final String appName, final Publisher<StockAlert> stockAlerts,
            final Publisher<PicardRow> atClosePublisher) throws ConfigException {

        final ConfigGroup yodaConfig = config.getEnabledGroup("yoda");
        if (null != yodaConfig) {
            final IFuseBox<YodaTransportComponents> yodaMonitor =
                    new ExpandedDetailResourceMonitor<>(monitor, "Yoda", errorLog, YodaTransportComponents.class, ReddalComponents.YODA);

            final MultiLayeredFuseBox<YodaTransportComponents> yodaParentMonitor =
                    new MultiLayeredFuseBox<>(yodaMonitor, YodaTransportComponents.class, errorLog);

            final long millisAtMidnight = selectIO.getMillisAtMidnightUTC();

            for (final ConfigGroup yodaInstanceConfig : yodaConfig.groups()) {

                final String instanceName = yodaInstanceConfig.getKey();
                final IFuseBox<YodaTransportComponents> yodaChildMonitor = yodaParentMonitor.createChildResourceMonitor(instanceName);

                final YodaAtCloseClient atCloseClient = new YodaAtCloseClient(selectIO, yodaChildMonitor, atClosePublisher);
                final YodaRestingOrderClient restingClient = new YodaRestingOrderClient(millisAtMidnight, stockAlerts);
                final YodaSweepClient sweepClient = new YodaSweepClient(millisAtMidnight, stockAlerts);
                final YodaTweetClient tweetClient = new YodaTweetClient(millisAtMidnight, stockAlerts);
                final YodaTWAPClient twapClient = new YodaTWAPClient(millisAtMidnight, stockAlerts);

                final YodaClientHandler yodaHandler =
                        YodaClientCacheFactory.createClientCache(selectIO, yodaChildMonitor, "yoda " + instanceName, appName, atCloseClient,
                                new YodaNullClient<>(), restingClient, sweepClient, twapClient, tweetClient, new YodaNullClient<>(),
                                EnumSet.of(YodaSignalType.AT_CLOSE, YodaSignalType.RESTING_ORDER, YodaSignalType.SWEEP, YodaSignalType.TWAP,
                                        YodaSignalType.TWEET));

                final TransportTCPKeepAliveConnection<?, ?> client =
                        YodaClientCacheFactory.createClient(selectIO, yodaInstanceConfig, yodaChildMonitor, yodaHandler);
                selectIO.execute(client::restart);

            }
        }
    }

    private static void setupStackManager(final Application<ReddalComponents> app, final ReddalFibers fibers, final ReddalChannels channels,
            final WebApplication webApp, final UILogger webLog, final SelectIOFiber selectIOFiber, final SelectIO opxlSelectIO,
            final IFuseBox<OPXLComponents> opxlMonitor, final OpxlClient<?> opxlClient,
            final SpreadContractSetGenerator contractSetGenerator, final boolean isForETF, final Set<StackCommunity> primaryCommunity)
            throws Exception {

        final ConfigGroup stackConfig = app.config.getEnabledGroup("stacks");
        if (null != stackConfig) {

            final IFuseBox<StackManagerComponents> stackManagerMonitor =
                    new ExpandedDetailResourceMonitor<>(app.monitor, "Stack Manager", app.errorLog, StackManagerComponents.class,
                            ReddalComponents.STACK_MANAGER);

            final StackManagerServer server =
                    new StackManagerServer(app.selectIO, app.selectIO, stackConfig, stackManagerMonitor, app.errorLog, app.persistenceDir,
                            app.logDir);
            final StackCommunityManager communityManager = server.getCommunityManager();

            final ConfigGroup symbolOPXLConfig = stackConfig.getGroup("symbolOPXL");
            final OpxlStrategySymbolUI strategySymbolUI = new OpxlStrategySymbolUI(opxlClient, symbolOPXLConfig);
            app.selectIO.addDelayedAction(30_000, strategySymbolUI::flush);

            final ConfigGroup symbolOffsetOPXLConfig = stackConfig.getGroup("symbolOffsetsOPXL");
            final OpxlStrategyOffsetsUI symbolOffsetUI = new OpxlStrategyOffsetsUI(opxlClient, symbolOffsetOPXLConfig);
            app.selectIO.addDelayedAction(10_000, symbolOffsetUI::flush);

            final Set<StackCommunity> secondaryViews = stackConfig.getEnumSet("otherCommunities", StackCommunity.class);

            final StackFamilyPresenter stackFamilyPresenter =
                    new StackFamilyPresenter(app.selectIO, opxlSelectIO, stackManagerMonitor, app.errorLog, webLog, contractSetGenerator,
                            primaryCommunity, secondaryViews, strategySymbolUI, channels.quotingObligationsCmds, channels.communitySymbols,
                            channels.communityIsins, channels.runnableInfo);
            channels.etfDefs.subscribe(selectIOFiber, stackFamilyPresenter::autoFamily);

            final StackConfigPresenter stackConfigPresenter = new StackConfigPresenter(webLog);
            final StackStrategiesPresenter strategiesPresenter = new StackStrategiesPresenter(webLog);

            final StackAutoManagerPresenter stackAutoManagerPresenter = new StackAutoManagerPresenter(webLog);

            stackFamilyPresenter.setCommunityManager(communityManager);
            channels.increaseParentOffsetCmds.subscribe(selectIOFiber, msg -> {
                final String familyName = PARENT_STACK_SUFFIX.matcher(msg.familyName).replaceAll("");
                communityManager.increaseOffset(msg.source, familyName, msg.side, msg.multiplier);
            });
            channels.increaseChildOffsetBPSCmds.subscribe(selectIOFiber, msg -> {
                final String childSymbol = PARENT_STACK_SUFFIX.matcher(msg.childName).replaceAll("");
                communityManager.increaseChildPriceOffset(msg.source, childSymbol, msg.side, msg.offsetIncreaseBPS);
            });
            channels.setSiblingsEnabledCmds.subscribe(selectIOFiber, msg -> {

                if (null != msg.familyName) {
                    final String familyName = PARENT_STACK_SUFFIX.matcher(msg.familyName).replaceAll("");
                    stackFamilyPresenter.setChildStackEnabled(msg.source, familyName, msg.side, msg.isEnabled);
                }

                if (null != msg.otcSymbol && !msg.isEnabled) {
                    stackFamilyPresenter.stopChild(msg.source, msg.otcSymbol, msg.side);
                }
            });

            final StackFamilyListener familyListener = new StackFamilyListener(stackFamilyPresenter);
            app.addStartUpAction(() -> {
                server.addStrategyListener(familyListener);
                server.addStacksListener(familyListener);
                server.addRelationshipListener(stackFamilyPresenter);
            });

            final IFuseBox<StackPersistenceComponents> logMonitor =
                    new ExpandedDetailResourceMonitor<>(stackManagerMonitor, "Stacks log", app.errorLog, StackPersistenceComponents.class,
                            StackManagerComponents.LOGGER);

            final MultiLayeredFuseBox<StackTransportComponents> clientMonitorParent =
                    MultiLayeredFuseBox.getExpandedMultiLayerMonitor(stackManagerMonitor, "Stacks", app.errorLog,
                            StackTransportComponents.class, StackManagerComponents.NIBBLER_CACHE);

            final ConfigGroup nibblerConfigs = stackConfig.getGroup("nibblers");

            for (final ConfigGroup nibblerConfig : nibblerConfigs.groups()) {

                final String nibbler = nibblerConfig.getKey();
                final String connectionName = app.appName + " config";

                final boolean isStackManager =
                        nibblerConfig.paramExists(IS_STACK_MANAGER_PARAM) && nibblerConfig.getBoolean(IS_STACK_MANAGER_PARAM);

                final StackChildListener childListener =
                        new StackChildListener(nibbler, isStackManager, stackFamilyPresenter, symbolOffsetUI);

                final StackCallbackBatcher stackUpdateBatcher =
                        new StackCallbackBatcher(nibbler, strategiesPresenter, stackConfigPresenter, stackAutoManagerPresenter,
                                childListener, isStackManager, contractSetGenerator);

                final StackNibblerClient nibblerClient = new StackNibblerClient(nibbler, communityManager, stackUpdateBatcher);

                final IFuseBox<StackTransportComponents> nibblerMonitor = clientMonitorParent.createChildResourceMonitor(connectionName);
                final StackClientHandler client =
                        StackCacheFactory.createClientCache(app.selectIO, nibblerConfig, nibblerMonitor, "Stacks-" + nibbler,
                                app.env.name() + connectionName, nibblerClient);

                final Path logPath = app.logDir.resolve("stackLog-" + nibbler + ".csv");
                final StackPersistenceWriter stackLogger = new StackPersistenceWriter(app.selectIO, logMonitor, logPath);
                client.addLogger(stackLogger);

                nibblerClient.setClient(client);
                if (!isStackManager) {
                    stackFamilyPresenter.setStrategyClient(nibbler, client);
                }
                strategiesPresenter.setStrategyClient(nibbler, client);
                stackConfigPresenter.setConfigClient(nibbler, client);
                stackAutoManagerPresenter.setConfigClient(nibbler, client);
            }

            channels.searchResults.subscribe(selectIOFiber, searchResult -> {
                stackFamilyPresenter.setSearchResult(searchResult);
                strategiesPresenter.addInstID(searchResult.symbol, searchResult.instID);
            });
            channels.symbolRefPrices.subscribe(selectIOFiber, stackAutoManagerPresenter::addRefPrice);
            channels.symbolSelections.subscribe(selectIOFiber, stackFamilyPresenter::symbolSelected);

            if (isForETF) {

                final OPXLEtfStackFilters etfStackFiltersOPXL =
                        new OPXLEtfStackFilters(opxlSelectIO, app.selectIO, opxlMonitor, app.logDir, stackFamilyPresenter);
                app.addStartUpAction(etfStackFiltersOPXL::start);

            }

            if (secondaryViews.contains(OPXLSpreadnoughtFilters.COMMUNITY)) {

                final OPXLSpreadnoughtFilters spreadnoughtFiltersOPXL =
                        new OPXLSpreadnoughtFilters(opxlSelectIO, app.selectIO, opxlMonitor, app.logDir, stackFamilyPresenter);
                app.addStartUpAction(spreadnoughtFiltersOPXL::start);
            }

            final TypedChannel<WebSocketControlMessage> familyWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackManager", "stackManager", fibers.ladderRouter, webApp, familyWebSocket);
            familyWebSocket.subscribe(selectIOFiber, stackFamilyPresenter::webControl);

            final TypedChannel<WebSocketControlMessage> configWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackConfig", "stackConfig", fibers.ladderRouter, webApp, configWebSocket);
            configWebSocket.subscribe(selectIOFiber, stackConfigPresenter::webControl);

            final TypedChannel<WebSocketControlMessage> strategiesWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackStrategy", "stackStrategy", fibers.ladderRouter, webApp, strategiesWebSocket);
            strategiesWebSocket.subscribe(selectIOFiber, strategiesPresenter::webControl);

            final TypedChannel<WebSocketControlMessage> autoManagerWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("stackAutoManager", "stackAutoManager", fibers.ladderRouter, webApp, autoManagerWebSocket);
            autoManagerWebSocket.subscribe(selectIOFiber, stackAutoManagerPresenter::webControl);
        }
    }

    private static Map<MDSource, LinkedList<ConfigGroup>> setupBackgroundNibblerTransport(final Application<ReddalComponents> app,
            final SelectIO opxlSelectIO, final IFuseBox<OPXLComponents> opxlMonitor, final OpxlClient<OPXLComponents> opxlClient,
            final ReddalFibers fibers, final WebApplication webApp, final UILogger webLog, final SelectIOFiber selectIOFiber,
            final ReddalChannels channels, final Thread.UncaughtExceptionHandler uncaughtExceptionHandler,
            final Set<StackCommunity> primaryCommunities) throws ConfigException, IOException {

        final Map<MDSource, LinkedList<ConfigGroup>> result = new EnumMap<>(MDSource.class);

        final ConfigGroup nibblerConfigs = app.config.getEnabledGroup("nibblers");
        if (null != nibblerConfigs) {

            final MsgBlotterPresenter msgBlotter = new MsgBlotterPresenter(app.selectIO, webLog);
            channels.ladderClickTradingIssues.subscribe(selectIOFiber,
                    msg -> msgBlotter.addLine("OrderRouter", app.clock.getReferenceNanoSinceMidnightUTC(), msg.symbol + ": " + msg.issue,
                            false));
            final SafetiesBlotterPresenter safetiesBlotter = new SafetiesBlotterPresenter(webLog);

            final WorkingOrdersPresenter workingOrderPresenter =
                    new WorkingOrdersPresenter(app.selectIO, app.monitor, webLog, channels.cmdsForNibblers,
                            channels.ladderClickTradingIssues, channels.orderEntryCommandToServer);

            channels.orderEntryFromServer.subscribe(selectIOFiber,
                    new BatchSubscriber<>(selectIOFiber, workingOrderPresenter::oeUpdate, 250, TimeUnit.MILLISECONDS));

            final QuotingObligationsPresenter quotingObligationsPresenter =
                    new QuotingObligationsPresenter(primaryCommunities, app.selectIO, webLog);
            for (final Map.Entry<StackCommunity, TypedChannel<String>> entry : channels.communitySymbols.entrySet()) {
                final StackCommunity community = entry.getKey();
                final TypedChannel<String> channel = entry.getValue();
                channel.subscribe(selectIOFiber, symbol -> quotingObligationsPresenter.setSymbol(community, symbol));
            }
            channels.quotingObligationsCmds.subscribe(selectIOFiber, quotingObligationsPresenter::enableQuotes);

            final String futureObligationsTopic;
            final boolean enableFutureObligations = app.config.paramExists("futureObligationsTopic");
            if (enableFutureObligations) {
                futureObligationsTopic = app.config.getString("futureObligationsTopic");
            } else {
                futureObligationsTopic = "";
            }

            final FutureObligationPresenter futureObligationPresenter =
                    new FutureObligationPresenter(opxlClient, app.selectIO, opxlMonitor, OPXLComponents.OPXL_QUOTING_OBLIGATIONS,
                            futureObligationsTopic, app.logDir);

            final IWorkingOrdersCallback bestWorkingOrderMaintainer;

            if (null != app.config.getEnabledGroup("bestWorkingOrders")) {

                final OPXLBestWorkingOrdersPresenter bestWorkingOrdersOPXLWriter =
                        new OPXLBestWorkingOrdersPresenter(opxlSelectIO, opxlMonitor, app.env);
                bestWorkingOrderMaintainer = new BestWorkingOrderMaintainer(bestWorkingOrdersOPXLWriter);
                opxlSelectIO.addDelayedAction(5000, bestWorkingOrdersOPXLWriter::flush);
            } else {
                bestWorkingOrderMaintainer = NoWorkingOrdersCallback.INSTANCE;
            }

            final IWorkingOrdersCallback gtcWorkingOrdersMaintainer;

            if (null != app.config.getEnabledGroup("gtcWorkingOrders")) {

                final OPXLGTCWorkingOrdersPresenter gtcWorkingOrdersOPXLWriter =
                        new OPXLGTCWorkingOrdersPresenter(opxlSelectIO, opxlMonitor, app.env);
                final GTCWorkingOrderMaintainer workingOrdersMaintainer = new GTCWorkingOrderMaintainer(gtcWorkingOrdersOPXLWriter);
                gtcWorkingOrdersMaintainer = workingOrdersMaintainer;
                opxlSelectIO.addDelayedAction(5000, gtcWorkingOrdersOPXLWriter::flush);

                setupBulkOrderSubmitter(app.selectIO, selectIOFiber, opxlMonitor, opxlClient, app.logDir, webLog, webApp,
                        channels.supportedGTCSymbols, channels.cmdsForNibblers, channels.ladderClickTradingIssues,
                        channels.gtcBettermentRequests, channels.gtcBettermentResponses, workingOrdersMaintainer);
            } else {
                gtcWorkingOrdersMaintainer = NoWorkingOrdersCallback.INSTANCE;
            }

            final MultiLayeredFuseBox<ReddalComponents> clientMonitorParent =
                    new MultiLayeredFuseBox<>(app.monitor, ReddalComponents.class, app.errorLog);

            final String[] remoteOrderPriorities = app.config.getString("nibblerPriorities").split(",");
            for (int i = 0; i < remoteOrderPriorities.length; ++i) {
                remoteOrderPriorities[i] = remoteOrderPriorities[i].trim();
            }
            final RemoteOrderServerRouter orderRouter = new RemoteOrderServerRouter(remoteOrderPriorities);
            channels.cmdsForNibblers.subscribe(selectIOFiber, cmd -> cmd.route(orderRouter));

            final Set<String> prioritisedNibblers = new HashSet<>(Arrays.asList(remoteOrderPriorities));

            for (final ConfigGroup nibblerConfig : nibblerConfigs.groups()) {

                final String nibbler = nibblerConfig.getKey();
                final String connectionName = app.appName + " config";

                final IFuseBox<ReddalComponents> childMonitor = clientMonitorParent.createChildResourceMonitor(connectionName);

                if (nibblerConfig.paramExists(MD_SOURCES_PARAM)) {
                    final Set<MDSource> mdSources = nibblerConfig.getEnumSet(MD_SOURCES_PARAM, MDSource.class);
                    for (final MDSource mdSource : mdSources) {
                        final List<ConfigGroup> nibblerConnectionConfigs = MapUtils.getMappedLinkedList(result, mdSource);
                        nibblerConnectionConfigs.add(nibblerConfig);
                    }
                }

                final boolean isTransportForTrading =
                        nibblerConfig.paramExists(IS_FOR_TRADING_PARAM) && nibblerConfig.getBoolean(IS_FOR_TRADING_PARAM);
                final Publisher<NibblerTransportConnected> connectedNibblerChannel;

                if (isTransportForTrading) {
                    connectedNibblerChannel = channels.nibblerTransportConnected;
                } else {
                    connectedNibblerChannel = Constants::NO_OP;
                }

                final IFuseBox<NibblerTransportComponents> nibblerMonitor =
                        new ExpandedDetailResourceMonitor<>(childMonitor, "Nibbler Transport", app.errorLog,
                                NibblerTransportComponents.class, ReddalComponents.BLOTTER_CONNECTION);

                final BlotterClient blotterClient =
                        new BlotterClient(nibbler, msgBlotter, safetiesBlotter, connectedNibblerChannel, nibbler);

                final NibblerClientHandler client =
                        NibblerCacheFactory.createClientCache(app.selectIO, nibblerConfig, nibblerMonitor, "nibblers-" + nibbler,
                                connectionName, isTransportForTrading, isTransportForTrading, blotterClient,
                                NibblerNotificationHandler.INSTANCE);

                final NibblerTransportCaches cache = client.getCaches();
                cache.addListener(blotterClient);

                safetiesBlotter.setNibblerClient(nibbler, client);

                final NibblerTransportOrderEntry orderEntry =
                        new NibblerTransportOrderEntry(app.selectIO, childMonitor, client, app.logDir);

                if (isTransportForTrading) {

                    if (!prioritisedNibblers.remove(nibbler)) {
                        throw new IllegalStateException("Trading nibbler [" + nibbler + "] configured without priority.");
                    } else {

                        quotingObligationsPresenter.setNibblerHandler(nibbler, orderEntry);

                        orderRouter.addNibbler(nibbler, orderEntry);

                        final NibblerMetaDataLogger logger = new NibblerMetaDataLogger(app.selectIO, app.monitor, app.logDir, nibbler);
                        cache.addTradingDataListener(logger, true, true);

                        workingOrderPresenter.addNibbler(nibbler);
                        final WorkingOrderListener workingOrderListener =
                                new WorkingOrderListener(nibbler, workingOrderPresenter, bestWorkingOrderMaintainer,
                                        gtcWorkingOrdersMaintainer, futureObligationPresenter, quotingObligationsPresenter, orderRouter);
                        cache.addTradingDataListener(workingOrderListener, true, true);

                        blotterClient.setWorkingOrderListener(workingOrderListener);
                    }
                } else {

                    orderRouter.addNonTradableNibbler(nibbler, orderEntry);
                }
            }

            if (!prioritisedNibblers.isEmpty()) {
                throw new IllegalStateException("Trading nibblers " + prioritisedNibblers + " were not configured");
            }

            final TypedChannel<WebSocketControlMessage> msgBlotterWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("blotter", "blotter", fibers.ladderRouter, webApp, msgBlotterWebSocket);
            msgBlotterWebSocket.subscribe(selectIOFiber, msgBlotter::webControl);

            final TypedChannel<WebSocketControlMessage> safetiesWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("safeties", "safeties", fibers.ladderRouter, webApp, safetiesWebSocket);
            safetiesWebSocket.subscribe(selectIOFiber, safetiesBlotter::webControl);

            final TypedChannel<WebSocketControlMessage> workingOrderWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("workingorders", "workingorders", fibers.ladderRouter, webApp, workingOrderWebSocket);
            workingOrderWebSocket.subscribe(selectIOFiber, workingOrderPresenter::webControl);

            final TypedChannel<WebSocketControlMessage> quotingObligationsWebSocket = TypedChannels.create(WebSocketControlMessage.class);
            createWebPageWithWebSocket("quotingObligations", "quotingObligations", fibers.ui, webApp, quotingObligationsWebSocket);
            quotingObligationsWebSocket.subscribe(selectIOFiber, quotingObligationsPresenter::webControl);

            if (enableFutureObligations) {
                app.addStartUpAction(futureObligationPresenter::start);
                final TypedChannel<WebSocketControlMessage> futureObligationsWebSocket =
                        TypedChannels.create(WebSocketControlMessage.class);
                createWebPageWithWebSocket("futureObligations", "futureObligations", fibers.ui, webApp, futureObligationsWebSocket);
                futureObligationsWebSocket.subscribe(selectIOFiber, futureObligationPresenter::webControl);
            }
        }

        return result;
    }

    private static void setupPicardUI(final SelectIO selectIO, final SelectIOFiber fiber, final UILogger webLog,
            final Channel<PicardRowWithInstID> picardRows, final Channel<PicardRow> yodaRows,
            final Channel<RecenterLadder> recenterLadderChannel, final TypedChannel<DisplaySymbol> displaySymbol,
            final TypedChannel<StackRunnableInfo> runnableInfo, final WebApplication webApp,
            final Map<StackCommunity, TypedChannel<String>> communitySymbols, final SelectIOChannel<Set<String>> picardDMFilterSymbols) {

        final PicardUI futureUI = setupPicardUI(selectIO, fiber, webLog, recenterLadderChannel, displaySymbol, runnableInfo,
                EnumSet.of(InstType.FUTURE, InstType.FUTURE_SPREAD), PicardSounds.FUTURES, webApp, "picard");
        picardRows.subscribe(fiber, futureUI::addPicardRow);

        final PicardUI spreadUI = setupPicardUI(selectIO, fiber, webLog, recenterLadderChannel, displaySymbol, runnableInfo,
                EnumSet.of(InstType.DR, InstType.EQUITY), PicardSounds.SPREADER, webApp, "picardspread");
        picardRows.subscribe(fiber, spreadUI::addPicardRow);

        setupEtfSplitPicardUI(selectIO, fiber, webLog, picardRows, recenterLadderChannel, communitySymbols, displaySymbol, runnableInfo,
                picardDMFilterSymbols, webApp);

        final PicardUI stocksUI =
                setupPicardUI(selectIO, fiber, webLog, recenterLadderChannel, displaySymbol, runnableInfo, EnumSet.allOf(InstType.class),
                        PicardSounds.STOCKS, webApp, "picardstocks");
        yodaRows.subscribe(fiber, stocksUI::addPicardRow);
    }

    private static void setupEtfSplitPicardUI(final SelectIO selectIO, final SelectIOFiber fiber, final UILogger webLog,
            final Channel<PicardRowWithInstID> picardRows, final Channel<RecenterLadder> recenterLadderChannel,
            final Map<StackCommunity, TypedChannel<String>> communitySymbols, final TypedChannel<DisplaySymbol> displaySymbol,
            final TypedChannel<StackRunnableInfo> runnableInfo, final SelectIOChannel<Set<String>> picardDMFilterSymbols,
            final WebApplication webApp) {

        final PicardUI allViews =
                setupPicardUI(selectIO, fiber, webLog, recenterLadderChannel, displaySymbol, runnableInfo, EnumSet.of(InstType.ETF),
                        PicardSounds.ETF, webApp, "picardetf-all");
        final Map<StackCommunity, PicardUI> communityScreens = new EnumMap<>(StackCommunity.class);
        final DelegatingPicardUI delegatingUI = new DelegatingPicardUI(communityScreens, allViews);

        for (final StackCommunity community : StackCommunity.values()) {
            if (InstType.ETF == community.instType) {
                final String alias = StackCommunity.DM == community ? "picardetf" : "picardetf-" + community.name().toLowerCase();
                final PicardUI ui =
                        setupPicardUI(selectIO, fiber, webLog, recenterLadderChannel, displaySymbol, runnableInfo, EnumSet.of(InstType.ETF),
                                PicardSounds.ETF, webApp, alias);
                communityScreens.put(community, ui);
                final TypedChannel<String> stringTypedChannel = communitySymbols.get(community);
                stringTypedChannel.subscribe(fiber, symbol -> delegatingUI.addSymbol(community, symbol));
            }
        }
        final PicardUI picardDM = communityScreens.get(StackCommunity.DM);
        picardDMFilterSymbols.subscribe(selectIO, picardDM::setOPXLFilterList);
        picardRows.subscribe(fiber, delegatingUI::addPicardRow);
    }

    private static PicardUI setupPicardUI(final SelectIO selectIO, final SelectIOFiber fiber, final UILogger webLog,
            final Channel<RecenterLadder> recenterLadderChannel, final TypedChannel<DisplaySymbol> displaySymbol,
            final TypedChannel<StackRunnableInfo> runnableInfo, final Set<InstType> filterList, final PicardSounds sound,
            final WebApplication webApp, final String alias) {

        final PicardUI picardUI = new PicardUI(webLog, filterList, sound, recenterLadderChannel);
        selectIO.addDelayedAction(1000, picardUI::flush);
        displaySymbol.subscribe(fiber, picardUI::setDisplaySymbol);
        runnableInfo.subscribe(fiber, picardUI::setSymbolRunnable);

        webApp.alias('/' + alias, "/picard.html");
        final TypedChannel<WebSocketControlMessage> webSocketChannel = TypedChannels.create(WebSocketControlMessage.class);
        webApp.createWebSocket('/' + alias + "/ws/", webSocketChannel, fiber);
        webSocketChannel.subscribe(fiber, picardUI::webControl);
        return picardUI;
    }

    private static void setupLaserDistancesUI(final SelectIO selectIO, final SelectIOFiber fiber, final UILogger webLog,
            final Channel<LiquidityFinderData> laserDistances, final TypedChannel<DisplaySymbol> displaySymbol,
            final WebApplication webApp) {

        final LiquidityFinderViewUI distanceUI = new LiquidityFinderViewUI(webLog);
        displaySymbol.subscribe(fiber, distanceUI::setDisplaySymbol);
        laserDistances.subscribe(fiber, distanceUI::setDistanceData);
        selectIO.addDelayedAction(5_000, distanceUI::updateUI);

        webApp.alias("/liquidityFinder", "/liquidityFinder.html");
        final TypedChannel<WebSocketControlMessage> webSocketChannel = TypedChannels.create(WebSocketControlMessage.class);
        webApp.createWebSocket("/liquidityFinder/ws/", webSocketChannel, fiber);
        webSocketChannel.subscribe(fiber, distanceUI::webControl);
    }

    private static FXCalc<?> createOPXLFXCalc(final Application<ReddalComponents> app, final SelectIO opxlSelectIO,
            final SelectIO callbackSelectIO, final IFuseBox<OPXLComponents> opxlMonitor) {

        final IFuseBox<PicardFXCalcComponents> fxMonitor = new IgnoredFuseBox<>();
        final FXCalc<PicardFXCalcComponents> fxCalc = new FXCalc<>(fxMonitor, PicardFXCalcComponents.FX_ERROR, MDSource.HOTSPOT_FX);
        final OpxlFXCalcUpdater opxlFXCalcUpdater = new OpxlFXCalcUpdater(opxlSelectIO, callbackSelectIO, opxlMonitor, fxCalc, app.logDir);
        app.addStartUpAction(opxlFXCalcUpdater::start);

        return fxCalc;
    }

    private static void setupBulkOrderSubmitter(final SelectIO selectIO, final SelectIOFiber fiber, final IFuseBox<OPXLComponents> monitor,
            final OpxlClient<OPXLComponents> opxlClient, final Path logDir, final UILogger webLog, final WebApplication webApp,
            final Channel<GTCSupportedSymbol> supportedSymbols, final Publisher<IOrderCmd> remoteOrderCommandToServerPublisher,
            final Channel<LadderClickTradingIssue> ladderClickTradingIssues,
            final TypedChannel<GTCBettermentPricesRequest> gtcBettermentRequests,
            final TypedChannel<GTCBettermentPrices> gtcBettermentResponses, final GTCWorkingOrderMaintainer gtcOrders) {

        final BulkOrderEntryPresenter bulkOrderUI =
                new BulkOrderEntryPresenter(selectIO, webLog, remoteOrderCommandToServerPublisher, ladderClickTradingIssues,
                        gtcBettermentRequests, gtcOrders);

        gtcBettermentResponses.subscribe(fiber, bulkOrderUI::bulkOrderResponse);

        webApp.alias("/bulkOrderEntry", "/bulkOrderEntry.html");
        final TypedChannel<WebSocketControlMessage> webSocketChannel = TypedChannels.create(WebSocketControlMessage.class);
        webApp.createWebSocket("/bulkOrderEntry/ws/", webSocketChannel, fiber);
        webSocketChannel.subscribe(fiber, bulkOrderUI::webControl);
        supportedSymbols.subscribe(fiber, bulkOrderUI::setGTCSupportedSymbol);

        new OPXLBulkOrderPriceLimits(selectIO, monitor, logDir, opxlClient, bulkOrderUI);
    }

}
