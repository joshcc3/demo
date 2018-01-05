package com.drwtrading.london.reddal.stacks.filters;

public class FuturesFilterGenerator {

    //    final Publisher<FuturesMetadata> publisher;
    //    final Map<String, Map<String, String>> data = new HashMap<>();
    //    final Map<String, TradableFuture> listings = new HashMap<>();
    //    final Map<String, FuturesMetadata> metadataMap = new HashMap<>();
    //
    //    public FuturesFilterGenerator(final Publisher<FuturesMetadata> publisher) {
    //        this.publisher = publisher;
    //    }
    //
    //    public void on(final OpxlData opxlData) {
    //        final OpxlTable opxlTable = OpxlTable.from(opxlData);
    //        for (final Map<String, String> row : opxlTable.rows) {
    //            final String symbol = row.get("ID").toUpperCase();
    //            data.put(symbol, row);
    //            handle(symbol);
    //        }
    //    }
    //
    //    public void on(final TradableFuture future) {
    //        listings.put(future.getExecutionSymbol(), future);
    //        handle(future.getExecutionSymbol());
    //    }
    //
    //    private void handle(final String symbol) {
    //
    //        final TradableFuture future = listings.get(symbol);
    //        if (null != future) {
    //            final HashMap<String, String> subData = new HashMap<>();
    //            subData.put("Exchange", future.exchange.name());
    //            subData.put("Expiry",
    //                    future.getExecutionSymbol().substring(future.getExecutionSymbol().length() - 2, future.getExecutionSymbol().length()));
    //            subData.put("Currency", future.ccy.name());
    //            final Map<String, String> map = data.get(symbol);
    //            if (map != null) {
    //                map.forEach((key, value) -> {
    //                    if (!subData.containsKey(key) && !"ID".equals(key) && !"".equals(value.trim())) {
    //                        subData.put(key, value);
    //                    }
    //                });
    //            }
    //            final FuturesMetadata metadata = new FuturesMetadata(future.getExecutionSymbol(), subData);
    //            if (!equals(metadataMap.put(symbol, metadata))) {
    //                publisher.publish(metadata);
    //            }
    //        }
    //    }
}
