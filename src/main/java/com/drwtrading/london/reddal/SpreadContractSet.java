package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;

public class SpreadContractSet extends Struct {

    public final String front;
    public final String back;
    public final String spread;

    public SpreadContractSet(final String front, final String back, final String spread) {

        this.front = front;
        this.back = back;
        this.spread = spread;
    }

    public String next(final String from) {

        if (front.equals(from)) {
            return back;
        } else if (back.equals(from)) {
            return spread;
        } else if (spread.equals(from)) {
            return front;
        } else {
            return null;
        }
    }
}
