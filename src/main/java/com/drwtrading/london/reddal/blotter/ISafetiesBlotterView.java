package com.drwtrading.london.reddal.blotter;

public interface ISafetiesBlotterView {

    public void setNibblerConnected(final String source, final boolean isConnected);

    public void setRow(final int id, final String source, final String safetyName, final long limit, final long warning,
            final String current, final String lastSymbol, final boolean isWarning, final boolean isError);

    public void removeRow(final int id);

    public void removeAllRows(final String source);
}
