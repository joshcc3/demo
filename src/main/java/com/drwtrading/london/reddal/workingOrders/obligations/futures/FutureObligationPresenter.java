package com.drwtrading.london.reddal.workingOrders.obligations.futures;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.opxl.QuotingObligation;
import com.drwtrading.london.reddal.opxl.QuotingObligationType;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.PriceQtyPair;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByBestPrice;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class FutureObligationPresenter extends AOpxlLoggingReader<OPXLComponents, Map<String, QuotingObligation>>
        implements IWorkingOrdersCallback {

    public static final String SYMBOL = "Symbol";
    public static final String QUANTITY = "Quantity";
    public static final String WIDTH = "Width";
    public static final String WIDTH_TYPE = "WidthType";
    private static final long OBLIGATION_CALC_PERIOD_MILLIS = 1000;
    private static final String TODAY = DateTimeFormatter.ofPattern(DateTimeUtil.DATE_FILE_FORMAT).format(LocalDate.now());
    public static final double INDEX_POINTS_TO_BPS_RATIO = 10000d;

    private final Map<String, FutureObligationPerformance> performanceMap;
    private final Map<String, WorkingOrdersByBestPrice> workingOrders;

    private final WebSocketViews<IFutureObligationView> views;
    private final DecimalFormat oneDP;
    private final SelectIO selectIO;

    private Map<String, QuotingObligation> obligations;

    public FutureObligationPresenter(final OpxlClient<OPXLComponents> opxlClient, final SelectIO callbackSelectIO,
            final IFuseBox<OPXLComponents> monitor, final OPXLComponents component, final String topicPrefix, final Path logDir) {

        super(opxlClient, callbackSelectIO, monitor, component, topicPrefix.replace("{DATE}", TODAY), logDir);
        this.selectIO = callbackSelectIO;
        this.performanceMap = new HashMap<>();
        this.workingOrders = new HashMap<>();
        this.obligations = new HashMap<>();

        this.views = new WebSocketViews<>(IFutureObligationView.class, this);

        this.oneDP = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 1);
    }

    @Override
    public void start() {
        selectIO.addDelayedAction(OBLIGATION_CALC_PERIOD_MILLIS, this::calcObligations);
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByBestPrice orderedWorkingOrders =
                workingOrders.computeIfAbsent(workingOrder.order.getSymbol(), WorkingOrdersByBestPrice::new);
        orderedWorkingOrders.setWorkingOrder(workingOrder);
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.get(workingOrder.order.getSymbol());
        orderedWorkingOrders.removeWorkingOrder(workingOrder);
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        for (final WorkingOrdersByBestPrice orderedWorkingOrders : workingOrders.values()) {
            orderedWorkingOrders.connectionLost(source);
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);
        }
    }

    public void onConnected(final WebSocketConnected connected) {

        final IFutureObligationView view = views.register(connected);
        for (final FutureObligationPerformance obligation : performanceMap.values()) {
            update(view, obligation);
        }
    }

    public void onDisconnected(final WebSocketDisconnected disconnected) {

        views.unregister(disconnected);
    }

    private long calcObligations() {

        for (final QuotingObligation obligation : obligations.values()) {

            final WorkingOrdersByBestPrice workingOrders = this.workingOrders.get(obligation.getSymbol());
            final FutureObligationPerformance obligationPerformance = getObligationPerformance(obligation, workingOrders);

            performanceMap.put(obligation.getSymbol(), obligationPerformance);

            update(views.all(), obligationPerformance);
        }
        return OBLIGATION_CALC_PERIOD_MILLIS;
    }

    public void update(final IFutureObligationView view, final FutureObligationPerformance performance) {

        final QuotingObligation obligation = performance.getObligation();

        final String quotingWidthObligation = Double.toString(obligation.getQuotingWidth());
        final String qtyObligation = Integer.toString(obligation.getQuantity());

        final boolean isObligationMet = performance.isObligationMet();

        final String widthShowing = oneDP.format(performance.getBpsWide());
        final String qtyShowing = Long.toString(performance.getQtyShowing());

        view.setObligation(obligation.getSymbol(), obligation.getType(), quotingWidthObligation, qtyObligation, isObligationMet,
                widthShowing, qtyShowing);
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Map<String, QuotingObligation> parseTable(final Object[][] opxlTable) {

        final Map<String, QuotingObligation> obligationMap = new HashMap<>();

        if (0 < opxlTable.length) {
            final Object[] headerRow = opxlTable[0];
            final int symbolCol = findColumn(headerRow, SYMBOL);
            final int quantityCol = findColumn(headerRow, QUANTITY);
            final int widthCol = findColumn(headerRow, WIDTH);
            final int widthType = findColumn(headerRow, WIDTH_TYPE);

            for (int i = 1; i < opxlTable.length; ++i) {
                final Object[] row = opxlTable[i];
                if (testColsPresent(row, symbolCol, quantityCol, widthCol, widthType)) {

                    final int quantity = Integer.parseInt(row[quantityCol].toString());
                    final double width = Double.parseDouble(row[widthCol].toString());
                    final QuotingObligationType type = QuotingObligationType.valueOf(row[widthType].toString());
                    final QuotingObligation obligation = new QuotingObligation(row[symbolCol].toString(), quantity, width, type);
                    obligationMap.put(obligation.getSymbol(), obligation);
                }
            }
        }

        return obligationMap;
    }

    @Override
    protected void handleUpdate(final Map<String, QuotingObligation> previous, final Map<String, QuotingObligation> current) {
        this.obligations = current;
    }

    private static FutureObligationPerformance getObligationPerformance(final QuotingObligation obligation,
            final WorkingOrdersByBestPrice workingOrders) {

        if (null == workingOrders) {

            return new FutureObligationPerformance(obligation, false, Double.POSITIVE_INFINITY, 0);
        } else {

            final PriceQtyPair bidShowing = workingOrders.getPriceToQty(BookSide.BID, obligation.getQuantity());
            final PriceQtyPair askShowing = workingOrders.getPriceToQty(BookSide.ASK, obligation.getQuantity());

            final double indexPointsWide;

            if (0 < bidShowing.qty && 0 < askShowing.qty) {
                indexPointsWide = askShowing.price - bidShowing.price;
            } else {
                indexPointsWide = Double.POSITIVE_INFINITY;
            }

            final long qtyShowing = Math.min(bidShowing.qty, askShowing.qty);

            if (obligation.getType() == QuotingObligationType.BPS) {

                final double bpsWide = (indexPointsWide / bidShowing.price) * INDEX_POINTS_TO_BPS_RATIO;
                final boolean isObligationMet = bpsWide <= obligation.getQuotingWidth() && obligation.getQuantity() <= qtyShowing;
                return new FutureObligationPerformance(obligation, isObligationMet, bpsWide, qtyShowing);
            } else if (obligation.getType() == QuotingObligationType.INDEX_POINTS) {

                final double denormalisedIndexPointsWide = indexPointsWide / Constants.NORMALISING_FACTOR;
                final boolean isObligationMet =
                        denormalisedIndexPointsWide <= obligation.getQuotingWidth() && obligation.getQuantity() <= qtyShowing;
                return new FutureObligationPerformance(obligation, isObligationMet, denormalisedIndexPointsWide, qtyShowing);
            } else {
                throw new IllegalStateException("Unsupported future obligation quoting type [" + obligation.getType() + "].");
            }
        }
    }

}
