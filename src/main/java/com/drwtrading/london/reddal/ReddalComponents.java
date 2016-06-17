package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.monitoring.IMonitoredComponent;

public enum ReddalComponents implements IMonitoredComponent {

    SELECT_IO_CLOSE,
    SELECT_IO_SELECT,
    SELECT_IO_UNHANDLED,

    INDY_HUB_AVAILABLE,
    INDY_UNDERLYING_TCP,
    INDY_TCP_PROTOCOL_FAILURE,

    MD_L3_HANDLER,
    MD_L2_HANDLER,

    NEW_MD_HUB_AVAILABLE,
    NEW_MD_COULD_NOT_ESTABLISH_CONNECTION,
    NEW_MD_UNDERLYING_TCP,
    NEW_MD_TCP_PROTOCOL_FAILURE,
    NEW_MD_MD_SERVER_UPDATE,
    NEW_MD_MD_SERVER_VIEWER;

    @Override
    public String getMnemonic() {
        return name();
    }

    @Override
    public String getToolTip() {
        return name();
    }
}
