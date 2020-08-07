package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

import com.drwtrading.london.eeif.utils.application.User;

public class AutoPullerSafeStartAll implements IAutoPullerCmd {

    private final User user;

    public AutoPullerSafeStartAll(final User user) {
        this.user = user;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {

        handler.safeStartAll(user);
    }
}
