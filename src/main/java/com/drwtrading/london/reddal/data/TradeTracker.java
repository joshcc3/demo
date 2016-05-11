package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;

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

    TradeTracker() {

        this.totalTradedVolumeByPrice = new LongMap<>();

        this.minTradedPrice = Long.MAX_VALUE;
        this.maxTradedPrice = Long.MIN_VALUE;
    }

    public void addTrade(final TradeUpdate tradeUpdate) {

        if (0 < qtyRunAtLastPrice) {
            if (lastPrice == tradeUpdate.getPrice()) {
                qtyRunAtLastPrice += tradeUpdate.getQuantityTraded();
                isLastTradeSameLevel = true;
            } else {
                qtyRunAtLastPrice = tradeUpdate.getQuantityTraded();
                isLastTradeSameLevel = false;
                isLastTickUp = lastPrice < tradeUpdate.getPrice();
                isLastTickDown = !isLastTickUp;
            }
        } else {
            qtyRunAtLastPrice = tradeUpdate.getQuantityTraded();
        }

        lastPrice = tradeUpdate.getPrice();
        minTradedPrice = Math.min(minTradedPrice, lastPrice);
        maxTradedPrice = Math.max(maxTradedPrice, lastPrice);

    }

    public void setTotalTraded(final TotalTradedVolumeByPrice totalTradedUpdate) {

        final long price = totalTradedUpdate.getPrice();
        final long qty = totalTradedUpdate.getQuantityTraded();
        final Long prevQty = totalTradedVolumeByPrice.put(price, qty);
        if (null == prevQty) {
            totalQtyTraded += qty;
        } else {
            totalQtyTraded += qty - prevQty;
        }
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
