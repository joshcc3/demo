package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.OpxlData;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;

import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class OpxlStrategySymbolUI {

    private static final String TOPIC = ".eeif.strategy.symbols.";
    private static final String TOPIC_PREFIX_PARAM = "env";

    private static final Object[] HEADERS = {"Symbol", "StrategyType", "Lean", "Quote"};

    private static final long PUBLISH_INTERVAL = 60_000;

    private final OpxlClient<?> writer;

    private final Map<InstType, HashMap<String, OpxlStrategyData>> symbolsPerTopics;

    private final String topic;

    private boolean isUpdated;
    private Object[][] table;

    public OpxlStrategySymbolUI(final OpxlClient<?> opxlClient, final ConfigGroup config) throws ConfigException {

        this.writer = opxlClient;

        final String topicPrefix = config.getString(TOPIC_PREFIX_PARAM);
        final String topicSuffix = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT).format(new Date());

        this.topic = topicPrefix + TOPIC + topicSuffix;
        this.symbolsPerTopics = new EnumMap<>(InstType.class);

        this.table = new Object[1][];
        table[0] = HEADERS;

        this.isUpdated = true;
    }

    public void addStrategySymbol(final InstType strategyInstType, final String symbol, final boolean isLean) {

        final Map<String, OpxlStrategyData> seenSymbols = MapUtils.getMappedMap(symbolsPerTopics, strategyInstType);
        OpxlStrategyData data = seenSymbols.get(symbol);

        if (data == null) {
            final Object[][] newTable = new Object[table.length + 1][];
            System.arraycopy(table, 0, newTable, 0, table.length);

            final Object[] newRow = {symbol, strategyInstType, false, false};
            newTable[table.length] = newRow;
            this.table = newTable;

            data = new OpxlStrategyData(strategyInstType, symbol, newRow);
            seenSymbols.put(symbol, data);
        }

        final boolean updated = isLean ? data.setLean() : data.setQuote();
        isUpdated |= updated;
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
