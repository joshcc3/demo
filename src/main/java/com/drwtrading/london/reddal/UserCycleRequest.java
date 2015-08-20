package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;

public class UserCycleRequest extends Struct {

    public final String username;

    public UserCycleRequest(final String username) {
        this.username = username;
    }
}
