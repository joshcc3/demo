package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.collections.LongMap;

public class TradeTracker {

    private final LongMap<Long> totalTradedVolumeByPrice;

    private long lastPrice;
    private long minTradedPrice;
    private long maxTradedPrice;

    private long totalQtyTraded;
    private long qtyRunAtLastPrice;
    private boolean isLastTradeSameLevel;
    private boolean isLastTickUp;
    private boolean isLastTickDown;

    public TradeTracker() {

        this.totalTradedVolumeByPrice = new LongMap<>();

        this.minTradedPrice = Long.MAX_VALUE;
        this.maxTradedPrice = Long.MIN_VALUE;
    }

    public void clear() {

        totalTradedVolumeByPrice.clear();

        lastPrice = 0;
        minTradedPrice = Long.MAX_VALUE;
        maxTradedPrice = Long.MIN_VALUE;

        totalQtyTraded = 0;
        qtyRunAtLastPrice = 0;
        isLastTradeSameLevel = false;
        isLastTickUp = false;
        isLastTickDown = false;
    }

    public void addTrade(final long price, final long qty) {

        if (0 < qtyRunAtLastPrice) {
            if (lastPrice == price) {
                qtyRunAtLastPrice += qty;
                isLastTradeSameLevel = true;
            } else {
                qtyRunAtLastPrice = qty;
                isLastTradeSameLevel = false;
                isLastTickUp = lastPrice < price;
                isLastTickDown = !isLastTickUp;
            }
        } else {
            qtyRunAtLastPrice = qty;
        }

        lastPrice = price;

        final Long prevQty = totalTradedVolumeByPrice.get(price);
        if (null == prevQty) {
            totalTradedVolumeByPrice.put(price, qty);
        } else {
            totalTradedVolumeByPrice.put(price, prevQty + qty);
        }
        totalQtyTraded += qty;
        minTradedPrice = Math.min(minTradedPrice, price);
        maxTradedPrice = Math.max(maxTradedPrice, price);
    }

    public boolean hasTrade() {
        return 0 < qtyRunAtLastPrice;
    }

    public long getLastPrice() {
        return lastPrice;
    }

    public long getMinTradedPrice() {
        return minTradedPrice;
    }

    public long getMaxTradedPrice() {
        return maxTradedPrice;
    }

    public long getTotalQtyTraded() {
        return totalQtyTraded;
    }

    public long getQtyRunAtLastPrice() {
        return qtyRunAtLastPrice;
    }

    public Long getTotalQtyTradedAtPrice(final long price) {
        return totalTradedVolumeByPrice.get(price);
    }

    public boolean isLastTradeSameLevel() {
        return isLastTradeSameLevel;
    }

    public boolean isLastTickUp() {
        return isLastTickUp;
    }

    public boolean isLastTickDown() {
        return isLastTickDown;
    }
}
