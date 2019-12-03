package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.OpxlData;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.stacks.family.StackUIData;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OpxlStrategyOffsetsUI {

    private static final String TOPIC = ".eeif.strategy.offsets.";
    private static final String TOPIC_PREFIX_PARAM = "env";

    private static final Object[] HEADERS = {"Symbol", "BID Offset", "ASK Offset"};

    private static final long PUBLISH_INTERVAL = 1_000;

    private final OpxlClient<?> writer;

    private final String topic;

    private final Map<String, String[]> symbolsRows;

    private boolean isUpdated;
    private Object[][] table;

    public OpxlStrategyOffsetsUI(final OpxlClient<?> writer, final ConfigGroup config) throws ConfigException {

        this.writer = writer;

        final String topicPrefix = config.getString(TOPIC_PREFIX_PARAM);
        final String topicSuffix = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT).format(new Date());

        this.topic = topicPrefix + TOPIC + topicSuffix;

        this.symbolsRows = new HashMap<>();

        this.table = new Object[1][];
        table[0] = HEADERS;

        this.isUpdated = true;
    }

    public void setStrategyOffsets(final StackUIData uiData) {

        final String[] symbolData = MapUtils.getMappedItem(symbolsRows, uiData.symbol, () -> {

            final Object[][] newTable = new Object[table.length + 1][];
            System.arraycopy(table, 0, newTable, 0, table.length);

            final String[] result = new String[HEADERS.length];
            newTable[table.length] = result;
            this.table = newTable;

            result[0] = uiData.symbol;
            return result;
        });

        symbolData[1] = uiData.getBidPriceOffsetBPS();
        symbolData[2] = uiData.getAskPriceOffsetBPS();

        isUpdated = true;
    }

    public long flush() {

        if (isUpdated) {

            final OpxlData data = new OpxlData(topic, table);
            writer.publish(data);

            isUpdated = false;
        }
        return PUBLISH_INTERVAL;
    }
}
