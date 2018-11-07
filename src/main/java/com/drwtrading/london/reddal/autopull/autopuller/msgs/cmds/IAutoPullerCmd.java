package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

public interface IAutoPullerCmd {

    public void executeOn(final IAutoPullerCmdHandler handler);
}
