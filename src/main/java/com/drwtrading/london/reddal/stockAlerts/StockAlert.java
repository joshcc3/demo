package com.drwtrading.london.reddal.stockAlerts;

public class StockAlert {

    public final String timestamp;
    public final String type;
    public final String symbol;

    public StockAlert(final String timestamp, final String type, final String symbol) {

        this.timestamp = timestamp;
        this.type = type;
        this.symbol = symbol;
    }
}
