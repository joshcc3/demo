package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

public class AutoPullerStopAll implements IAutoPullerCmd {

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.stopAll();
    }
}
