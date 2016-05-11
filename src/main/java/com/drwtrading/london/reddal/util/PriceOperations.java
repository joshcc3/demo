package com.drwtrading.london.reddal.util;


import com.drwtrading.london.protocols.photon.marketdata.Side;

public interface PriceOperations {

    long tradablePrice(long price, Side side);

    long nTicksAway(long price, int n, PriceUtils.Direction direction);
}
