package com.drwtrading.london.reddal;

import com.drwtrading.london.reddal.ladders.Contract;
import com.drwtrading.london.util.Struct;

public class UserCycleRequest extends Struct {

    public final String username;
    public final Contract contract;

    public UserCycleRequest(final String username) {
        this.username = username;
        this.contract = null;
    }

    public UserCycleRequest(final String username, final Contract contract) {
        this.username = username;
        this.contract = contract;
    }
}
