package com.drwtrading.london.reddal.autopull.msgs.cmds;

public interface IAutoPullerCmd {

    public void executeOn(final IAutoPullerCmdHandler handler);
}
