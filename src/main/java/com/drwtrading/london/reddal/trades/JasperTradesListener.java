package com.drwtrading.london.reddal.trades;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import drw.eeif.trades.transport.outbound.ITrade;
import drw.eeif.trades.transport.outbound.io.ITradesListener;

import java.util.HashSet;
import java.util.Set;

public class JasperTradesListener implements ITradesListener {

    private final Set<String> seenTradeIds = new HashSet<>();

    private final TypedChannel<MrChillTrade> trades;

    public JasperTradesListener(final TypedChannel<MrChillTrade> trades) {
        this.trades = trades;
    }

    @Override
    public void newTrade(final ITrade trade) {

        if (seenTradeIds.add(trade.getTradeId())) {
            final String tag = trade.getTag().toLowerCase();
            if (tag.startsWith("jasper") || tag.startsWith("tow")) {

                final MrChillTrade mrChillTrade = new MrChillTrade(trade.getSymbol(), trade.getSide(), trade.getPrice());

                trades.publish(mrChillTrade);
            }
        }
    }

    @Override
    public void tradeUpdate(final ITrade trade) {
        // NO-OP
    }

    @Override
    public void disconnected() {
        // NO-OP
    }

    @Override
    public void connected() {
        // NO-OP
    }

    @Override
    public void batchComplete() {
        // NO-OP
    }
}
