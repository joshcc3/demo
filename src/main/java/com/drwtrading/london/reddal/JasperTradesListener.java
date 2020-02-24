package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import drw.eeif.trades.transport.outbound.ITrade;

import java.util.HashSet;
import java.util.Set;

public class JasperTradesListener implements ITransportCacheListener<String, ITrade> {

    private final Set<String> seenTradeIds = new HashSet<>();

    private final TypedChannel<ITrade> trades;

    public JasperTradesListener(final TypedChannel<ITrade> trades) {
        this.trades = trades;
    }

    @Override
    public boolean initialValue(final int transportID, final ITrade trade) {
        if (seenTradeIds.add(trade.getTradeId())) {
            final String tag = trade.getTag().toLowerCase();
            if (tag.startsWith("jasper") || tag.startsWith("tow")) {
                trades.publish(trade);
            }
        }
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final ITrade item) {
        return true;
    }

    @Override
    public void batchComplete() {
        //NO-OP
    }

}
