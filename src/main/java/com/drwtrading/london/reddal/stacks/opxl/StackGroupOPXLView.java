package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LaserLine;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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

    private final Map<String, Long> greenLaserLines;
    private final Map<String, Long> bidLaserLines;
    private final Map<String, Long> askLaserLines;
    private final Set<String> updatedSymbols;

    private final NavigableMap<String, Object[]> rows;

    public StackGroupOPXLView(final IResourceMonitor<ReddalComponents> monitor, final String opxlTopic) {

        this.monitor = monitor;

        this.opxlTopic = opxlTopic;

        this.client = new OpxlClient(OPXL_SERVER, OPXL_PORT);

        this.greenLaserLines = new HashMap<>();
        this.bidLaserLines = new HashMap<>();
        this.askLaserLines = new HashMap<>();

        this.updatedSymbols = new HashSet<>();

        this.rows = new TreeMap<>();
    }

    public void setLaserLine(final LadderMetadata metadata) {

        if (LadderMetadata.Type.LASER_LINE == metadata.typeEnum()) {

            final LaserLine laserLine = (LaserLine) metadata;

            final String symbol = laserLine.getSymbol();
            final String line = laserLine.getId();
            final long price = laserLine.getPrice();
            switch (line) {
                case "green": {
                    if (0 < price) {
                        greenLaserLines.put(symbol, price);
                    } else {
                        greenLaserLines.remove(symbol);
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

            final Long greenLine = greenLaserLines.get(symbol);
            final Long bidLine = bidLaserLines.get(symbol);
            final Long askLine = askLaserLines.get(symbol);

            final Object[] row = getRow(symbol);

            if (null != greenLine && null != bidLine && null != askLine) {

                final double mid = (bidLine + askLine) / 2d;
                final double result = (mid - greenLine) / Constants.NORMALISING_FACTOR;

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
