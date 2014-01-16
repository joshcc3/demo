package com.drwtrading.london.reddal;

import com.drwtrading.london.photons.reddal.selecta.Direction;
import com.drwtrading.london.photons.reddal.selecta.Side;
import com.drwtrading.london.util.Struct;

public class OffsetUpdate extends Struct {
    public final String symbol;
    public final String equityId;
    public final Side side;
    public final Direction direction;

    public OffsetUpdate(String symbol, String equityId, Side side, Direction direction) {
        this.symbol = symbol;
        this.equityId = equityId;
        this.side = side;
        this.direction = direction;
    }
}
