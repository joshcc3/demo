package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.monitoring.IMonitoredComponent;

public enum ReddalComponents implements IMonitoredComponent {

    SELECT_IO,
    MONITOR,

    INDY,

    UI_SELECT_IO,
    STACK_SELECT_IO,
    AUTO_PULLER_SELECT_IO,

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
    STACK_OPXL_OUTPUT,

    PKS,
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
