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

    MD_TRANSPORT,

    LADDER_PRESENTER,

    STACK_GROUP_CLIENT,
    STACK_OPXL_OUTPUT,
    STACK_MANAGER,

    PKS,
    YODA,
    BLOTTER,

    SAFETY_WORKING_ORDER_VIEWER,

    OPXL_ULTIMATE_PARENT;

    @Override
    public String getMnemonic() {
        return name();
    }

    @Override
    public String getToolTip() {
        return name();
    }
}
