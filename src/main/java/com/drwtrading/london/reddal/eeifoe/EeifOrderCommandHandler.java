package com.drwtrading.london.reddal.eeifoe;

public interface EeifOrderCommandHandler {
    void on(SubmitEeifOrder submitEeifOrder);
    void on(CancelEeifOrder cancelEeifOrder);
}
