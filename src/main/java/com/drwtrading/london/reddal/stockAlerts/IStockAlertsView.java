package com.drwtrading.london.reddal.stockAlerts;

public interface IStockAlertsView {

    public void stockAlert(final String timestamp, final String type, final String symbol);
}
