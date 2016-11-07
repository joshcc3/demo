package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.monitoring.IMonitoredComponent;

public enum ReddalComponents implements IMonitoredComponent {

    MONITOR_DUPLICATE_APPLICATION_REGISTRATION,
    MONITOR_APP_COUNT,
    MONITOR_CLIENT_PROTOCOL_ERROR,
    MONITOR_CLIENT_HEARTBEAT_TIME_OUT,
    MONITOR_CLIENT_TCP_CLOSED,
    MONITOR_CLIENT_TCP_EXCEPTION,
    MONITOR_CLIENT_UNKNOWN_MSG_TYPE,

    SELECT_IO_CLOSE,
    SELECT_IO_SELECT,
    SELECT_IO_UNHANDLED,

    INDY_HUB_AVAILABLE,
    INDY_UNDERLYING_TCP,
    INDY_TCP_PROTOCOL_FAILURE,

    MD_L3_HANDLER,
    MD_L2_HANDLER,

    MD_HUB_AVAILABLE,
    MD_COULD_NOT_ESTABLISH_CONNECTION,
    MD_UNDERLYING_TCP,
    MD_TCP_PROTOCOL_FAILURE,
    MD_MD_SERVER_UPDATE,
    MD_MD_SERVER_VIEWER,

    STACK_GROUP_CLIENT,
    STACK_CONFIG_CLIENT,

    YODA,

    SAFETY_WORKING_ORDER_VIEWER;

    @Override
    public String getMnemonic() {
        return name();
    }

    @Override
    public String getToolTip() {
        return name();
    }
}
