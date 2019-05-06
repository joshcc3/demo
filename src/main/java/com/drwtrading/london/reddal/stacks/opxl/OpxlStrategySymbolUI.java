package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.OpxlData;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.OPXLComponents;

import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpxlStrategySymbolUI {

    private static final String TOPIC = ".eeif.strategy.symbols.";
    private static final String TOPIC_PREFIX_PARAM = "env";

    private static final Object[] HEADERS = {"Symbol", "StrategyType"};

    private static final long PUBLISH_INTERVAL = 60_000;

    private final OpxlClient<OPXLComponents> writer;

    private final Map<InstType, Set<String>> symbolsPerTopics;

    private final String topic;

    private boolean isUpdated;
    private Object[][] table;

    public OpxlStrategySymbolUI(final SelectIO selectIO, final ConfigGroup config, final IResourceMonitor<OPXLComponents> monitor)
            throws ConfigException {

        this.writer = new OpxlClient<>(selectIO, monitor, OPXLComponents.OPXL_BEST_WORKING_ORDER_WRITER);
        this.writer.start();

        final String topicPrefix = config.getString(TOPIC_PREFIX_PARAM);
        final String topicSuffix = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT).format(new Date());

        this.topic = topicPrefix + TOPIC + topicSuffix;
        this.symbolsPerTopics = new EnumMap<>(InstType.class);

        for (final InstType instType : InstType.values()) {

            final HashSet<String> symbols = new HashSet<>();
            symbolsPerTopics.put(instType, symbols);
        }

        this.table = new Object[1][];
        table[0] = HEADERS;

        this.isUpdated = false;
    }

    public void addStrategySymbol(final InstType strategyInstType, final String symbol) {

        final Set<String> seenSymbols = symbolsPerTopics.get(strategyInstType);
        if (seenSymbols.add(symbol)) {

            final Object[][] newTable = new Object[table.length + 1][];
            System.arraycopy(table, 0, newTable, 0, table.length);

            final Object[] newRow = {symbol, strategyInstType};
            newTable[table.length] = newRow;
            this.table = newTable;

            isUpdated = true;
        }
    }

    public long flush() {

        if (isUpdated) {

            final OpxlData data = new OpxlData(topic, table);
            writer.publish(data);
        }
        return PUBLISH_INTERVAL;
    }
}
