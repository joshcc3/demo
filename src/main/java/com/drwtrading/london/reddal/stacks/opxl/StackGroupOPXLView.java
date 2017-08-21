package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LaserLine;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
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

    private final Map<String, Long> whiteLaserLines;
    private final Map<String, Long> bidLaserLines;
    private final Map<String, Long> askLaserLines;
    private final Set<String> updatedSymbols;

    private final NavigableMap<String, Object[]> rows;

    public StackGroupOPXLView(final IResourceMonitor<ReddalComponents> monitor, final String opxlTopic) {

        this.monitor = monitor;

        final SimpleDateFormat simpleDF = DateTimeUtil.getDateFormatter('_' + DateTimeUtil.DATE_FILE_FORMAT);
        final String date = simpleDF.format(new Date());
        this.opxlTopic = opxlTopic + date;

        this.client = new OpxlClient(OPXL_SERVER, OPXL_PORT);

        this.whiteLaserLines = new HashMap<>();
        this.bidLaserLines = new HashMap<>();
        this.askLaserLines = new HashMap<>();

        this.updatedSymbols = new HashSet<>();

        this.rows = new TreeMap<>();
    }

    public void setLaserLines(final List<LadderMetadata> laserLines) {

        for (final LadderMetadata laserLine : laserLines) {
            if (LadderMetadata.Type.LASER_LINE == laserLine.typeEnum()) {
                final LaserLine laserLine1 = (LaserLine) laserLine;
                final String symbol = laserLine1.getSymbol();
                final String line = laserLine1.getId();
                final long price = laserLine1.getPrice();
                switch (line) {
                    case "white": {
                        if (0 < price) {
                            whiteLaserLines.put(symbol, price);
                        } else {
                            whiteLaserLines.remove(symbol);
                        }
                        break;
                    }
                    case "bid": {
                        if (0 < price) {
                            bidLaserLines.put(symbol, price);
                        } else {
                            bidLaserLines.remove(symbol);
                        }
                        break;
                    }
                    case "ask": {
                        if (0 < price) {
                            askLaserLines.put(symbol, price);
                        } else {
                            askLaserLines.remove(symbol);
                        }
                        break;
                    }
                }

                updatedSymbols.add(symbol);
            }
        }
    }

    public long update() {

        updateOffsets();

        final int rowCount = rows.size() + 1;
        final Object[][] opxlTable = new Object[rowCount][];

        opxlTable[0] = HEADER_ROW;
        int rowIndex = 0;

        for (final Object[] row : rows.values()) {

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

    private void updateOffsets() {

        for (final String symbol : updatedSymbols) {

            final Long theoLine = whiteLaserLines.get(symbol);
            final Long bidLine = bidLaserLines.get(symbol);
            final Long askLine = askLaserLines.get(symbol);

            final Object[] row = getRow(symbol);

            if (null != theoLine && null != bidLine && null != askLine) {

                final double mid = (bidLine + askLine) / 2d;
                final double result = (mid - theoLine) / Constants.NORMALISING_FACTOR;

                row[BEST_BID_COL] = result;
                row[BEST_ASK_COL] = result;
            }
        }
        updatedSymbols.clear();
    }

    private Object[] getRow(final String symbol) {

        final Object[] row = rows.get(symbol);
        if (null == row) {
            final Object[] result = {symbol, null, null};
            rows.put(symbol, result);
            return result;
        } else {
            return row;
        }
    }
}
