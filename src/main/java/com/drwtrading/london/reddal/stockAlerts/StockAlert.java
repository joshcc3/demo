package com.drwtrading.london.reddal.stockAlerts;

public class StockAlert {

    public final String timestamp;
    public final String type;
    public final String symbol;
    public final String msg;

    public StockAlert(final String timestamp, final String type, final String symbol, final String msg) {

        this.timestamp = timestamp;
        this.type = type;
        this.symbol = symbol;
        this.msg = msg;
    }
}
