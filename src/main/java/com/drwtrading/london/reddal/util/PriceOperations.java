package com.drwtrading.london.reddal.util;


import com.drwtrading.london.protocols.photon.marketdata.Side;

import java.math.BigDecimal;

public interface PriceOperations {
    long tickIncrement(long nearbyPrice, Side side);

    long tradeablePrice(BigDecimal price, Side side);

    long tradeablePrice(String price, Side side);

    long tradeablePrice(long price, Side side);

    long nTicksAway(long price, int n, PriceUtils.Direction direction);
}
