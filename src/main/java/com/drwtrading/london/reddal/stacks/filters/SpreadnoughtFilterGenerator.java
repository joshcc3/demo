package com.drwtrading.london.reddal.stacks.filters;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SpreadnoughtFilterGenerator {

    final Publisher<StackMetadata> metaDataPublisher;

//    final Map<String, StackListing> stacks;
    final Map<String, Map<String, String>> dataBySymbol;
    final Map<String, StackMetadata> publishedMetaData;

    public SpreadnoughtFilterGenerator(final Publisher<StackMetadata> metaDataPublisher) {
        this.metaDataPublisher = metaDataPublisher;

//        this.stacks = new HashMap<>();
        this.dataBySymbol = new HashMap<>();
        this.publishedMetaData = new HashMap<>();
    }

//    public void on(final StackListing stack) {
//        stacks.put(stack.getExecutionSymbol(), stack);
//        process(stack.getExecutionSymbol(), stack.quoteMDSource, stack.quoteInstID, stack.leanInstID);
//    }
//
//    public void onOpxlData(final OpxlData opxlData) {
//        final OpxlTable table = OpxlTable.from(opxlData);
//        for (final Map<String, String> row : table.rows) {
//            if (row.containsKey("Symbol")) {
//                final String symbol = row.get("Symbol").trim();
//                if (!symbol.trim().isEmpty()) {
//
//                    dataBySymbol.put(symbol, row);
//
//                    final StackListing stack = stacks.get(symbol);
//                    if (null != stack) {
//                        process(stack.getExecutionSymbol(), stack.quoteMDSource, stack.quoteInstID, stack.leanInstID);
//                    }
//                }
//            }
//        }
//    }

    private void process(final String quoteSymbol, final MDSource quoteSource, final InstrumentID quoteInstID,
            final InstrumentID leanInstID) {

        final StackMetadata metadata = new StackMetadata(quoteSymbol, new TreeMap<>());
        metadata.data.put("Quote Venue", quoteSource.name());
        metadata.data.put("Quote Exch", quoteInstID.mic.exchange.name());
        metadata.data.put("Quote Ccy", quoteInstID.ccy.name());
        metadata.data.put("Lean Exch", leanInstID.mic.exchange.name());
        metadata.data.put("Lean Ccy", leanInstID.ccy.name());
        final Map<String, String> data = dataBySymbol.get(quoteSymbol);
        if (null != data) {
            data.forEach((key, value) -> {
                if (!metadata.data.containsKey(key) && !"Symbol".equals(key) && !"".equals(value.trim())) {
                    metadata.data.put(key, value);
                }
            });
        }
        if (!metadata.equals(publishedMetaData.put(metadata.symbol, metadata))) {
            metaDataPublisher.publish(metadata);
        }
    }
}
