package com.drwtrading.london.reddal.blotter;

public interface ISafetiesBlotterView {

    public void setNibblerConnected(final String source, final boolean isConnected);

    public void setOMS(final int id, final String source, final long remoteOMSID, final String omsName, final boolean isEnabled,
            final String stateText);

    public void setRow(final int id, final String source, final long remoteSafetyID, final String safetyName, final String limit,
            final String warning, final String current, final String lastSymbol, final boolean isEditable, final boolean isWarning,
            final boolean isError);

    public void removeRow(final int id);

    public void removeAllRows(final String source);
}
