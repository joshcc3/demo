package com.drwtrading.london.reddal.blotter;

public interface IMsgBlotterView {

    public void setNibblerConnected(final String source, final boolean isConnected);

    public void addRow(final int id, final String timestamp, final String source, final String text, final boolean isLowPriority);

    public void removeRow(final int id);
}
