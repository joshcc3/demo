package com.drwtrading.london.reddal.autopull.autopuller.msgs.updates;

public interface IAutoPullerUpdate {

    public void executeOn(final IAutoPullerUpdateHandler handler);
}
