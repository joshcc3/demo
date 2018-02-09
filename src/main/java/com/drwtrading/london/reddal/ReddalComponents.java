package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.monitoring.IMonitoredComponent;

public enum ReddalComponents implements IMonitoredComponent {

    SELECT_IO,
    MONITOR,

    INDY,

    UI_SELECT_IO,
    SHREDDER_SELECT_IO,
    STACK_SELECT_IO,
    AUTO_PULLER_SELECT_IO,

    MD_L3_HANDLER,
    MD_L2_HANDLER,

    MD_TRANSPORT,

    LADDER_ROUTER,
    LADDER_PRESENTER,

    SHREDDER_ROUTER,
    SHREDDER_PRESENTER,

    STACK_GROUP_CLIENT,
    STACK_OPXL_OUTPUT,
    STACK_MANAGER,

    PKS,
    YODA,
    BLOTTER_CONNECTION,
    BLOTTER_CONNECTION_LOG,
    TRADING_DATA,
    META_DATA_LOG,

    SAFETY_WORKING_ORDER_VIEWER,

    OPXL_ULTIMATE_PARENT,
    OPXL_FX_CALC,
    OPXL_ETF_STACK_MANAGER_FILTERS;

    @Override
    public String getMnemonic() {
        return name();
    }

    @Override
    public String getToolTip() {
        return name();
    }
}
