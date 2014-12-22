package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;

import java.util.ArrayList;
import java.util.List;

public class SpreadContractSet extends Struct {
    public final String front;
    public final String back;
    public final String spread;
    private final List<String> contractList = new ArrayList<>();

    public SpreadContractSet(final String front, final String back, final String spread) {
        this.front = front;
        this.back = back;
        this.spread = spread;
        contractList.add(front);
        if (back != null) {
            contractList.add(back);
        }
        if (spread != null) {
            contractList.add(spread);
        }
    }

    public String next(String from) {
        return offset(from, 1);
    }

    public String prev(String from) {
        return offset(from, -1);
    }

    private String offset(final String from, final int offset) {
        int index = contractList.indexOf(from);
        if (index < 0) {
            return from;
        } else {
            return contractList.get(remainder(index + offset, contractList.size()));
        }
    }

    private int remainder(final int a, final int n) {
        return ((a % n) + n) % n;
    }
}
