package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelThreeMonitor;
import com.drwtrading.london.eeif.utils.marketData.book.impl.levelThree.Level3Book;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.SingleBandTickTable;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.LaserLineType;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.LastTradeDataForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.TradingStatusForAll;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.model.LadderViewModel;
import com.drwtrading.london.reddal.orderManagement.NibblerTransportConnected;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryClient;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByPrice;
import drw.eeif.eeifoe.BookParameters;
import drw.eeif.eeifoe.OrderParameters;
import drw.eeif.eeifoe.OrderSide;
import drw.eeif.eeifoe.PegToPrice;
import drw.eeif.eeifoe.PredictionParameters;
import drw.eeif.eeifoe.PriceParameters;
import drw.eeif.eeifoe.QuotingParameters;
import drw.eeif.eeifoe.RemoteOrder;
import drw.eeif.eeifoe.TakingParameters;
import drw.eeif.eeifoe.Update;
import drw.eeif.fees.FeesCalc;
import drw.london.json.Jsonable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetlang.channels.Publisher;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ZoomableLadderTest {

    private static final String DEFAULT_USER = "Trader McTradeface";
    private static final String SYMBOL = "VOD LN";
    private static final InstrumentID INST_ID = new InstrumentID("GB1234567890", CCY.GBX, MIC.XLON);
    private static final ITickTable TICK_TABLE = new SingleBandTickTable(Constants.NORMALISING_FACTOR);
    private static final long DEFAULT_CENTER_PRICE = 0L;
    private static final int LEVELS = 50;

    private static final Map<LaserLineType, LaserLineValue> LAZORS = new EnumMap<>(LaserLineType.class);
    private static final String RIGHT = "right";
    private static final String LEFT = "left";

    static {
        for (final LaserLineType lazorType : LaserLineType.values()) {
            LAZORS.put(lazorType, new LaserLineValue(SYMBOL, lazorType, 0));
        }
    }

    @SuppressWarnings("unchecked")
    private final IResourceMonitor<ReddalComponents> monitor = Mockito.mock(IResourceMonitor.class);
    @SuppressWarnings("unchecked")
    private final Publisher<LadderClickTradingIssue> ladderClickTradingIssuePublisher = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<IOrderCmd> remoteOrderCommandPublisher = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<OrderEntryCommandToServer> eeifCommandToServer = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<StackIncreaseParentOffsetCmd> stackParentCmdPublisher = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<Jsonable> trace = Mockito.mock(Publisher.class);

    private final IBookLevelThreeMonitor bookViewer = Mockito.mock(IBookLevelThreeMonitor.class);
    private final ILadderUI view = Mockito.mock(ILadderUI.class);
    private final LadderPrefsForSymbolUser ladderPrefsForSymbolUser = Mockito.mock(LadderPrefsForSymbolUser.class);
    private final SymbolStackData stackData = Mockito.mock(SymbolStackData.class);
    private final UiPipeImpl uiPipe = Mockito.mock(UiPipeImpl.class);

    private final FXCalc<ReddalComponents> fxCalc = new FXCalc<>(monitor, ReddalComponents.FX_ERROR, MDSource.INTERNAL);
    private final FeesCalc feesCalc = new FeesCalc(System.out::println, fxCalc);
    private final DecimalFormat feeDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 5);
    private final TradingStatusForAll tradingStatusForAll = new TradingStatusForAll();
    private final Set<OrderType> supportedOrderTypes = EnumSet.noneOf(OrderType.class);
    private final Set<AlgoType> supportedAlgoTypes = EnumSet.noneOf(AlgoType.class);
    private final LastTradeDataForSymbol lastTradeDataForSymbol = new LastTradeDataForSymbol("");
    private final LadderMetaData metaData = new LadderMetaData("");
    private final LadderOptions ladderOptions =
            new LadderOptions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "");
    private final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap = new HashMap<>();

    @BeforeMethod
    public void setup() {

        Mockito.reset(monitor, ladderClickTradingIssuePublisher, remoteOrderCommandPublisher, eeifCommandToServer, stackParentCmdPublisher,
                increaseChildOffsetCmdPublisher, disableSiblingsCmdPublisher, trace, view, ladderPrefsForSymbolUser, stackData, uiPipe, bookViewer);

        Mockito.when(stackData.getNavLaserLine()).thenReturn(LAZORS.get(LaserLineType.NAV));
        Mockito.when(stackData.getTheoLaserLine()).thenReturn(LAZORS.get(LaserLineType.GREEN));
    }

    @Test
    public void displayLimitOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;

        book.addOrder(1, BookSide.BID, 0, qty);
        book.addOrder(2, BookSide.ASK, 1, qty);
        book.setValidity(true);

        bookView.timedRefresh();
        bookView.refresh(SYMBOL);

        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.BID + centerRow), Mockito.eq("100"));
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.OFFER + (centerRow - 1)), Mockito.eq("100"));
    }

    @Test
    public void displayZoomedOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;
        final long initialPrice = 100 * Constants.NORMALISING_FACTOR;

        book.addOrder(1, BookSide.BID, initialPrice, qty);
        book.addOrder(2, BookSide.BID, TICK_TABLE.addTicks(initialPrice, 1), qty);
        book.addOrder(3, BookSide.ASK, TICK_TABLE.addTicks(initialPrice, 1), qty);
        book.addOrder(4, BookSide.ASK, TICK_TABLE.addTicks(initialPrice, 2), qty);
        book.setValidity(true);

        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.BID + centerRow), Mockito.eq(Integer.toString(2 * qty)));
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.OFFER + (centerRow - 1)), Mockito.eq(Integer.toString(2 * qty)));
    }

    @Test
    public void displayZoomedWorkingOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 1, DEFAULT_CENTER_PRICE, BookSide.BID));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 2, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), BookSide.BID));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 3, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), BookSide.ASK));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 4, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2), BookSide.ASK));

        book.setValidity(true);
        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.ORDER + centerRow), Mockito.eq(Integer.toString(2 * qty)));
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.ORDER + (centerRow - 1)), Mockito.eq(Integer.toString(2 * qty)));
    }

    @Test
    public void displayZoomedOrderEntryOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;

        orderUpdates.onUpdate(getUpdate(qty, DEFAULT_CENTER_PRICE, OrderSide.BUY, 1));
        orderUpdates.onUpdate(getUpdate(qty, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), OrderSide.BUY, 2));
        orderUpdates.onUpdate(getUpdate(qty, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), OrderSide.SELL, 3));
        orderUpdates.onUpdate(getUpdate(qty, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2), OrderSide.SELL, 4));

        book.setValidity(true);
        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.ORDER + centerRow), Mockito.eq(Integer.toString(2 * qty)));
        Mockito.verify(uiPipe).txt(Mockito.startsWith(HTML.ORDER + (centerRow - 1)), Mockito.eq(Integer.toString(2 * qty)));
    }

    @Test
    public void cancelZoomedOutOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;

        workingOrders.setWorkingOrder(getWorkingOrder(qty, 1, DEFAULT_CENTER_PRICE, BookSide.BID));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 2, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), BookSide.BID));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 3, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), BookSide.ASK));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 4, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2), BookSide.ASK));

        book.setValidity(true);
        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);

        final Map<String, String> bidPrice = Collections.singletonMap("price", Long.toString(DEFAULT_CENTER_PRICE));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + centerRow, LEFT, bidPrice);
        final Map<String, String> askPrice = Collections.singletonMap("price", Long.toString(TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2)));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + (centerRow - 1), LEFT, askPrice);
        Mockito.verify(remoteOrderCommandPublisher, Mockito.times(4)).publish(Mockito.any());
    }

    @Test
    public void cancelZoomedOutOrderEntryOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;

        orderUpdates.onUpdate(getUpdate(qty, DEFAULT_CENTER_PRICE, OrderSide.BUY, 1));
        orderUpdates.onUpdate(getUpdate(qty, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), OrderSide.BUY, 2));
        orderUpdates.onUpdate(getUpdate(qty, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), OrderSide.SELL, 3));
        orderUpdates.onUpdate(getUpdate(qty, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2), OrderSide.SELL, 4));

        book.setValidity(true);
        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);

        final Map<String, String> bidPrice = Collections.singletonMap("price", Long.toString(DEFAULT_CENTER_PRICE));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + centerRow, LEFT, bidPrice);
        final Map<String, String> askPrice = Collections.singletonMap("price", Long.toString(TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2)));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + (centerRow - 1), LEFT, askPrice);
        Mockito.verify(eeifCommandToServer, Mockito.times(4)).publish(Mockito.any());
    }

    @Test
    public void modifyZoomedOutOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;

        workingOrders.setWorkingOrder(getWorkingOrder(qty, 1, DEFAULT_CENTER_PRICE, BookSide.BID));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 2, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), BookSide.BID));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 3, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 1), BookSide.ASK));
        workingOrders.setWorkingOrder(getWorkingOrder(qty, 4, TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2), BookSide.ASK));

        book.setValidity(true);
        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);

        tradingStatusForAll.setNibblerConnected(new NibblerTransportConnected("", true));

        final Map<String, String> bidPriceInit = Collections.singletonMap("price", Long.toString(DEFAULT_CENTER_PRICE));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + centerRow, RIGHT, bidPriceInit);
        final Map<String, String> bidPriceModify =
                Collections.singletonMap("price", Long.toString(TICK_TABLE.subtractTicks(DEFAULT_CENTER_PRICE, 2)));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + (centerRow + 1), RIGHT, bidPriceModify);
        Mockito.verify(remoteOrderCommandPublisher, Mockito.times(2)).publish(Mockito.any());

        final Map<String, String> askPriceInit =
                Collections.singletonMap("price", Long.toString(TICK_TABLE.addTicks(DEFAULT_CENTER_PRICE, 2)));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + centerRow, RIGHT, askPriceInit);
        final Map<String, String> askPriceModify = Collections.singletonMap("price", Long.toString(DEFAULT_CENTER_PRICE));
        bookView.onClick(ClientSpeedState.FINE, HTML.ORDER + (centerRow - 1), RIGHT, askPriceModify);
        Mockito.verify(remoteOrderCommandPublisher, Mockito.times(4)).publish(Mockito.any());
    }

    @Test
    public void submitZoomedOutOrdersTest() {
        final LadderViewModel ladderModel = new LadderViewModel(uiPipe);
        ladderModel.extendToLevels(LEVELS);
        final int centerRow = LEVELS / 2;
        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        final Level3Book book = new Level3Book(bookViewer, SYMBOL, 1, INST_ID, InstType.EQUITY, TICK_TABLE, MDSource.INTERNAL, 1, 1);
        mdForSymbol.setL3Book(book);
        final WorkingOrdersByPrice workingOrders = new WorkingOrdersByPrice();
        final OrderUpdatesForSymbol orderUpdates = new OrderUpdatesForSymbol(SYMBOL);
        final LadderBookView bookView = getBookView(SYMBOL, ladderModel, mdForSymbol, workingOrders, orderUpdates, orderEntryMap);
        final int qty = 100;

        book.setValidity(true);
        bookView.zoomOut();
        bookView.timedRefresh();
        bookView.refresh(SYMBOL);

        Mockito.doReturn("MANUAL").when(ladderPrefsForSymbolUser).get(Mockito.matches(HTML.ORDER_TYPE_LEFT), Mockito.any());
        Mockito.doReturn("TEST_TAG").when(ladderPrefsForSymbolUser).get(Mockito.matches(HTML.WORKING_ORDER_TAG));
        Mockito.doReturn("50").when(ladderPrefsForSymbolUser).get(Mockito.matches(HTML.INP_RELOAD), Mockito.any());
        tradingStatusForAll.setNibblerConnected(new NibblerTransportConnected("", true));
        final Map<String, String> bidPriceInit = Collections.singletonMap("price", Long.toString(DEFAULT_CENTER_PRICE));
        bookView.setTradingBoxQty(50);
        bookView.onClick(ClientSpeedState.FINE, HTML.BID + centerRow, LEFT, bidPriceInit);
        Mockito.verify(remoteOrderCommandPublisher, Mockito.times(1)).publish(Mockito.any());
    }

    private LadderBookView getBookView(final String symbol, final LadderViewModel ladderModel, final MDForSymbol mdForSymbol,
            final WorkingOrdersByPrice workingOrders, final OrderUpdatesForSymbol orderUpdatesForSymbol,
            final Map<String, OrderEntryClient.SymbolOrderChannel> orderEntryMap) {

        return new LadderBookView(monitor, DEFAULT_USER, true, symbol, ladderModel, view, ladderOptions, fxCalc, feesCalc, feeDF,
                ladderPrefsForSymbolUser, ladderClickTradingIssuePublisher, remoteOrderCommandPublisher, eeifCommandToServer,
                tradingStatusForAll, supportedOrderTypes, supportedAlgoTypes, mdForSymbol, workingOrders, lastTradeDataForSymbol,
                orderUpdatesForSymbol, LEVELS, stackData, metaData, stackParentCmdPublisher, increaseChildOffsetCmdPublisher,
                disableSiblingsCmdPublisher, trace, orderEntryMap, DEFAULT_CENTER_PRICE);
    }

    private static SourcedWorkingOrder getWorkingOrder(final int qty, final int orderId, final long price, final BookSide side) {
        return new SourcedWorkingOrder("",
                new WorkingOrder(orderId, 1, orderId, SYMBOL, "", side, AlgoType.MANUAL, OrderType.LIMIT, orderId, price, qty, 0));
    }

    private static UpdateFromServer getUpdate(final int qty, final long price, final OrderSide side, final int orderId) {
        final PriceParameters priceParameters = new PegToPrice(price);
        final BookParameters bookParameters = new BookParameters(true, true, true, false, false, false, 0);
        final TakingParameters takingParameters = new TakingParameters(true, 0, 100000, 1, true, 1000);
        final QuotingParameters quotingParameters = new QuotingParameters(true, 100, 10, 10, 10, 10, 10, 10, 10, 10, true);
        final PredictionParameters predictionParameters = new PredictionParameters(true);
        final OrderParameters orderParameters =
                new OrderParameters(priceParameters, bookParameters, takingParameters, quotingParameters, predictionParameters);
        final RemoteOrder remoteOrder = new RemoteOrder(SYMBOL, side, price, qty, DEFAULT_USER, orderParameters, new ObjectArrayList<>());
        final Update update = new Update(orderId, 0, qty, price, false, "", remoteOrder);
        return new UpdateFromServer("", update);
    }
}
