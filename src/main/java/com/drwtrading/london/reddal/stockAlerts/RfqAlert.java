package com.drwtrading.london.reddal.stockAlerts;

import com.drwtrading.london.eeif.utils.staticData.CCY;

public class RfqAlert {

    private static final long NULL_VALUE = Long.MIN_VALUE;

    public final long milliSinceMidnight;

    public final String symbol;
    public final long price;
    public final long qty;
    public final CCY ccy;
    public final boolean isETF;

    public RfqAlert(final long milliSinceMidnight, final String symbol, final long price, final long qty, final CCY ccy,
            final boolean isETF) {
        this.milliSinceMidnight = milliSinceMidnight;
        this.symbol = symbol;
        this.price = price;
        this.qty = qty;
        this.ccy = ccy;
        this.isETF = isETF;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RfqAlert rfqAlert = (RfqAlert) o;

        return milliSinceMidnight == rfqAlert.milliSinceMidnight && qty == rfqAlert.qty && symbol.equals(rfqAlert.symbol) &&
                ccy == rfqAlert.ccy && isETF == rfqAlert.isETF;
    }

    public boolean isValidQty() {
        return qty != NULL_VALUE;
    }

    public boolean isValidPrice() {
        return price != NULL_VALUE;
    }

    @Override
    public int hashCode() {
        return (int) (milliSinceMidnight ^ (milliSinceMidnight >>> 32));
    }

    @Override
    public String toString() {
        return "RFQ Alert " + milliSinceMidnight + ", " + symbol + ": " + price + ", " + qty;
    }
}
