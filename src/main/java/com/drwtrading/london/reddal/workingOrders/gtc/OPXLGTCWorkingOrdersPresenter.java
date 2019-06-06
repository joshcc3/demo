package com.drwtrading.london.reddal.workingOrders.gtc;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.OpxlData;
import com.drwtrading.london.eeif.utils.application.Environment;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.OPXLComponents;

import java.util.Date;
import java.util.TreeMap;

public class OPXLGTCWorkingOrdersPresenter {

    private static final String TOPIC = ".gtcWorkingOrders.";
    private static final long PUBLISH_PERIOD_MILLIS = 5000;
    private static final String[] HEADERS = {"symbol", "count"};

    private final SelectIO selectIO;

    private final OpxlClient<OPXLComponents> writer;
    private final String topic;

    private final TreeMap<String, GTCWorkingOrderCount> symbolCounts;

    private Object[][] table;

    public OPXLGTCWorkingOrdersPresenter(final SelectIO selectIO, final IResourceMonitor<OPXLComponents> monitor, final Environment env) {

        this.selectIO = selectIO;

        this.writer = new OpxlClient<>(selectIO, monitor, OPXLComponents.OPXL_BEST_WORKING_ORDER_WRITER);
        this.writer.start();

        final String topicSuffix = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT).format(new Date());
        this.topic = env + TOPIC + topicSuffix;

        this.symbolCounts = new TreeMap<>();

        this.table = new String[][]{HEADERS};
    }

    public void setTopOfBigWorkingOrders(final GTCWorkingOrderCount gtcCount) {

        selectIO.execute(() -> {

            final GTCWorkingOrderCount prev = symbolCounts.put(gtcCount.symbol, gtcCount);
            if (null == prev) {
                table = new Object[symbolCounts.size() + 1][HEADERS.length];
                table[0] = HEADERS;
            }
        });
    }

    public long flush() {

        int i = 0;

        for (final GTCWorkingOrderCount prices : symbolCounts.values()) {

            final Object[] row = table[++i];

            row[0] = prices.symbol;
            row[1] = prices.count;
        }

        final OpxlData data = new OpxlData(topic, table);
        writer.publish(data);

        return PUBLISH_PERIOD_MILLIS;
    }
}
