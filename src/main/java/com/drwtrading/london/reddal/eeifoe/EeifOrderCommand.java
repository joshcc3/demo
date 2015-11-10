package com.drwtrading.london.reddal.eeifoe;

public interface EeifOrderCommand {
    int getOrderId();
    String getServer();
    void accept(EeifOrderCommandHandler handler);
}
