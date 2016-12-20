package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class StackGroupOPXLView {

    private static final String OPXL_SERVER = "ldnoptech";
    private static final int OPXL_PORT = 30002;

    private static final long REFRESH_RATE_MILLIS = 5000;
    private static final String[] HEADER_ROW = {"symbol", "bestBid", "bestAsk"};

    private static final int BEST_BID_COL = 1;
    private static final int BEST_ASK_COL = 2;

    private final IResourceMonitor<ReddalComponents> monitor;

    private final String opxlTopic;

    private final OpxlClient client;

    private final Map<BookSide, Map<String, StackGroup>> stackGroups;
    private final Map<String, StackRefPriceDetail> refPriceDetails;

    private final NavigableMap<String, String[]> rows;

    private final DecimalFormat priceDF;

    public StackGroupOPXLView(final IResourceMonitor<ReddalComponents> monitor, final String opxlTopic) {

        this.monitor = monitor;

        this.opxlTopic = opxlTopic;

        this.client = new OpxlClient(OPXL_SERVER, OPXL_PORT);

        this.stackGroups = new EnumMap<>(BookSide.class);
        for (final BookSide side : BookSide.values()) {
            this.stackGroups.put(side, new HashMap<>());
        }
        this.refPriceDetails = new HashMap<>();

        this.rows = new TreeMap<>();

        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2, 10);
    }

    public void setStackGroup(final StackGroup stackGroup) {

        this.stackGroups.get(stackGroup.getSide()).put(stackGroup.getSymbol(), stackGroup);
        final StackRefPriceDetail refPriceDetail = refPriceDetails.get(stackGroup.getSymbol());

        updateStack(stackGroup, refPriceDetail);
    }

    public void setStackRefPrice(final StackRefPriceDetail refPriceDetail) {

        this.refPriceDetails.put(refPriceDetail.symbol, refPriceDetail);

        for (final Map<String, StackGroup> stacks : stackGroups.values()) {

            final StackGroup stackGroup = stacks.get(refPriceDetail.symbol);
            updateStack(stackGroup, refPriceDetail);
        }
    }

    private void updateStack(final StackGroup stackGroup, final StackRefPriceDetail refPriceDetail) {

        if (null != stackGroup && null != refPriceDetail) {

            final String symbol = stackGroup.getSymbol();

            final String[] row = getRow(symbol);
            final long priceOffset = stackGroup.getPriceOffset();
            final Integer pullBackTicks = getPullBackTicks(stackGroup);

            final String printValue;
            if (null == pullBackTicks) {
                printValue = null;
            } else {

                final long refPrice = refPriceDetail.refPrice + priceOffset;

                final long calcPrice = refPriceDetail.tickTable.getTicksAway(stackGroup.getSide(), refPrice, pullBackTicks);
                final double spreadSide = (calcPrice - refPrice + priceOffset) / (double) Constants.NORMALISING_FACTOR;

                printValue = priceDF.format(spreadSide);
            }

            if (BookSide.BID == stackGroup.getSide()) {
                row[BEST_BID_COL] = printValue;
            } else {
                row[BEST_ASK_COL] = printValue;
            }
        }
    }

    private String[] getRow(final String symbol) {

        final String[] row = rows.get(symbol);
        if (null == row) {
            final String[] result = {symbol, null, null};
            rows.put(symbol, result);
            return result;
        } else {
            return row;
        }
    }

    private static Integer getPullBackTicks(final StackGroup stackGroup) {

        Integer result = null;
        for (final StackType type : StackType.values()) {

            final Stack stack = stackGroup.getStack(type);
            final StackLevel level = stack.getFirstLevel();

            if (stack.isEnabled() && null != level) {

                final int pullBackTicks = level.getPullbackTicks();
                if (null == result || pullBackTicks < result) {
                    result = pullBackTicks;
                }
            }
        }
        return result;
    }

    public long update() {

        final int rowCount = rows.size() + 1;
        final Object[][] opxlTable = new Object[rowCount][];

        opxlTable[0] = HEADER_ROW;
        int rowIndex = 0;

        for (final String[] row : rows.values()) {

            opxlTable[++rowIndex] = row;
        }

        try {
            if (client.isConnected()) {
                client.publish(new OpxlData(opxlTopic, opxlTable));
                monitor.setOK(ReddalComponents.STACK_OPXL_OUTPUT);
            } else {
                client.reconnect();
            }
        } catch (final IOException e) {
            monitor.logError(ReddalComponents.STACK_OPXL_OUTPUT, "Failed to write OPXL values.", e);
        }
        return REFRESH_RATE_MILLIS;
    }
}
