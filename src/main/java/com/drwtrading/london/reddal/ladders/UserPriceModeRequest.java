package com.drwtrading.london.reddal.ladders;

public class UserPriceModeRequest {

    public final String username;
    public final PricingMode mode;

    UserPriceModeRequest(final String username, final PricingMode mode) {
        this.username = username;
        this.mode = mode;
    }
}
