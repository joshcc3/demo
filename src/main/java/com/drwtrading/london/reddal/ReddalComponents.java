package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.monitoring.IMonitoredComponent;

public enum ReddalComponents implements IMonitoredComponent {

    SELECT_IO_CLOSE,
    SELECT_IO_SELECT,
    SELECT_IO_UNHANDLED,

    INDY_HUB_AVAILABLE,
    INDY_UNDERLYING_TCP,
    INDY_TCP_PROTOCOL_FAILURE,

    MD_TRANSPORT;

    @Override
    public String getMnemonic() {
        return name();
    }

    @Override
    public String getToolTip() {
        return name();
    }
}
