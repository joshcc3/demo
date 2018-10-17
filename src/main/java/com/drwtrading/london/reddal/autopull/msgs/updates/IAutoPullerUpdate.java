package com.drwtrading.london.reddal.autopull.msgs.updates;

public interface IAutoPullerUpdate {

    public void executeOn(final IAutoPullerUpdateHandler handler);
}
