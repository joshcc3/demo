package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.util.Struct;

public class HeartbeatRoundtrip extends Struct {

    public final String userName;
    public final String symbol;
    public final long sentTimeMillis;
    public final long returnTimeMillis;
    public final long roundtripMillis;

    public HeartbeatRoundtrip(final String userName, final String symbol, final long sentTimeMillis, final long returnTimeMillis,
            final long roundtripMillis) {

        this.userName = userName;
        this.symbol = symbol;
        this.sentTimeMillis = sentTimeMillis;
        this.returnTimeMillis = returnTimeMillis;
        this.roundtripMillis = roundtripMillis;
    }
}
