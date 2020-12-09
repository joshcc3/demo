package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.monitoring.IFuse;

public enum ReddalComponents implements IFuse {

    SELECT_IO,
    MONITOR,

    OLD_ERRORS,

    INDY,

    UI_SELECT_IO,
    STACK_SELECT_IO,
    AUTO_PULLER_SELECT_IO,

    MD_L3_HANDLER,
    MD_L2_HANDLER,

    MD_TRANSPORT,

    FEES_CALC,
    LADDER_ROUTER,
    LADDER_PRESENTER,

    SHREDDER_ROUTER,
    ORDER_PRESENTER,

    STACK_GROUP_CLIENT,
    STACK_MANAGER,

    PKS,
    YODA,
    BLOTTER_CONNECTION,
    BLOTTER_CONNECTION_LOG,
    TRADING_DATA,
    META_DATA_LOG,

    SAFETY_WORKING_ORDER_VIEWER,

    OPXL_READERS,

    FX,

    MR_CHILL_TRADES,
    ICE_PIE;

    @Override
    public String getMnemonic() {
        return name();
    }

    @Override
    public String getToolTip() {
        return name();
    }
}
