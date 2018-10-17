package com.drwtrading.london.reddal.autopull.msgs.cmds;

public class AutoPullerPriceRefreshRequest implements IAutoPullerCmd {

    public AutoPullerPriceRefreshRequest() {
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.priceRefreshRequest();
    }
}
