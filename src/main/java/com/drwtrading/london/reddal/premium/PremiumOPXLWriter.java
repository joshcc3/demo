package com.drwtrading.london.reddal.premium;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.OpxlData;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.OPXLComponents;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PremiumOPXLWriter {

    private static final String TOPIC = ".eeif.spreadnought.premia.";
    private static final String TOPIC_PREFIX_PARAM = "prefix";

    private static final Object[] HEADERS = {"Symbol", "Premium", "LastTradePremium"};

    private static final int SYMBOL_COL = 0;
    private static final int PREMIUM_COL = 1;
    private static final int LAST_TRADE_PREMIUM_COL = 2;

    private static final long PUBLISH_INTERVAL = 5000;

    private final OpxlClient<OPXLComponents> writer;
    private final String topic;

    private final Map<String, Object[]> rows;

    private Object[][] writeTable;

    public PremiumOPXLWriter(final SelectIO selectIO, final ConfigGroup config, final IFuseBox<OPXLComponents> monitor)
            throws ConfigException {

        this.writer = new OpxlClient<>(selectIO, config, monitor, OPXLComponents.OPXL_SPREAD_PREMIUM_WRITER);
        this.writer.start();

        final String topicPrefix = config.getString(TOPIC_PREFIX_PARAM);
        final String topicSuffix = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT).format(new Date());

        this.topic = topicPrefix + TOPIC + topicSuffix;

        this.rows = new HashMap<>();
        this.writeTable = new Object[1][];
        this.writeTable[0] = HEADERS;
    }

    public void onPremium(final Premium premium) {
        final String symbol = premium.symbol;

        final Object[] row = rows.get(symbol);
        if (null == row) {

            final Object[] newRow = new Object[HEADERS.length];
            rows.put(symbol, newRow);
            newRow[SYMBOL_COL] = symbol;
            newRow[PREMIUM_COL] = premium.midMarketPremium;
            newRow[LAST_TRADE_PREMIUM_COL] = premium.lastTradPremium;

            final Object[][] oldData = writeTable;
            writeTable = new Object[oldData.length + 1][];
            System.arraycopy(oldData, 0, writeTable, 0, oldData.length);

            writeTable[writeTable.length - 1] = newRow;
        } else {

            row[PREMIUM_COL] = premium.midMarketPremium;
            row[LAST_TRADE_PREMIUM_COL] = premium.lastTradPremium;
        }
    }

    public long flush() {

        final OpxlData data = new OpxlData(topic, writeTable);

        writer.publish(data);
        return PUBLISH_INTERVAL;
    }
}
