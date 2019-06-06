package com.drwtrading.london.reddal.workingOrders.bestPrices;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.OpxlData;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.Environment;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.OPXLComponents;

import java.util.Date;
import java.util.TreeMap;

public class OPXLBestWorkingOrdersPresenter {

    private static final String TOPIC = ".bestWorkingOrders.";
    private static final long PUBLISH_PERIOD_MILLIS = 5000;
    private static final String[] HEADERS = {"symbol", "bestBid", "bestAsk", "bestBidQty", "bestAskQty"};

    private final SelectIO selectIO;

    private final OpxlClient<OPXLComponents> writer;
    private final String topic;

    private final TreeMap<String, BestWorkingPriceForSymbol> symbolPrices;

    private Object[][] table;

    public OPXLBestWorkingOrdersPresenter(final SelectIO selectIO, final IResourceMonitor<OPXLComponents> monitor, final Environment env) {

        this.selectIO = selectIO;

        this.writer = new OpxlClient<>(selectIO, monitor, OPXLComponents.OPXL_BEST_WORKING_ORDER_WRITER);
        this.writer.start();

        final String topicSuffix = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT).format(new Date());
        this.topic = env + TOPIC + topicSuffix;

        this.symbolPrices = new TreeMap<>();

        this.table = new String[][]{HEADERS};
    }

    public void setTopOfBigWorkingOrders(final BestWorkingPriceForSymbol bestWorkingPrice) {

        selectIO.execute(() -> {

            final BestWorkingPriceForSymbol prev = symbolPrices.put(bestWorkingPrice.symbol, bestWorkingPrice);
            if (null == prev) {
                table = new Object[symbolPrices.size() + 1][HEADERS.length];
                table[0] = HEADERS;
            }
        });
    }

    public long flush() {

        int i = 0;

        for (final BestWorkingPriceForSymbol prices : symbolPrices.values()) {

            final Object[] row = table[++i];

            row[0] = prices.symbol;

            if (null != prices.bidPrice) {
                row[1] = prices.bidPrice / (double) Constants.NORMALISING_FACTOR;
                row[3] = prices.bidQty;
            }

            if (null != prices.askPrice) {
                row[2] = prices.askPrice / (double) Constants.NORMALISING_FACTOR;
                row[4] = prices.askQty;
            }

        }

        final OpxlData data = new OpxlData(topic, table);
        writer.publish(data);

        return PUBLISH_PERIOD_MILLIS;
    }
}
