package com.drwtrading.london.reddal.stockAlerts;

public class StockAlert {

    public final long milliSinceMidnight;


    public final String timestamp;
    public final String type;
    public final String symbol;
    public final String msg;

    public StockAlert(final long milliSinceMidnight, final String timestamp, final String type, final String symbol, final String msg) {

        this.milliSinceMidnight = milliSinceMidnight;

        this.timestamp = timestamp;
        this.type = type;
        this.symbol = symbol;
        this.msg = msg;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final StockAlert that = (StockAlert) o;
            return timestamp.equals(that.timestamp) && type.equals(that.type) && symbol.equals(that.symbol) && msg.equals(that.msg);
        }
    }

    @Override
    public int hashCode() {
        return (int) (milliSinceMidnight ^ (milliSinceMidnight >>> 32));
    }

    @Override
    public String toString() {
        return "StockAlert [" + type + "] " + timestamp + ", " + symbol + ": " + msg;
    }
}
