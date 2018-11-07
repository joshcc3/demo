package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

public class AutoPullerSafeStartAll implements IAutoPullerCmd {

    private final String username;

    public AutoPullerSafeStartAll(final String username) {
        this.username = username;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.safeStartAll(username);
    }
}
