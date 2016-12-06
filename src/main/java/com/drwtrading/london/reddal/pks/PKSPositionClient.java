package com.drwtrading.london.reddal.pks;

import com.drwtrading.london.eeif.position.transport.cache.IPositionCmdListener;
import com.drwtrading.london.eeif.position.transport.data.PositionValue;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PKSPositionClient implements ITransportCacheListener<String, PositionValue>, IPositionCmdListener {

    private final Publisher<PKSExposure> positionPublisher;

    private final Map<String, HashSet<String>> isinToSymbols;
    private final Map<String, Double> isinExposures;

    public PKSPositionClient(final Publisher<PKSExposure> positionPublisher) {

        this.positionPublisher = positionPublisher;

        this.isinToSymbols = new HashMap<>();
        this.isinExposures = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        final HashSet<String> symbols = MapUtils.getMappedSet(isinToSymbols, searchResult.instID.isin);
        symbols.add(searchResult.symbol);

        final Double exposure = isinExposures.get(searchResult.instID.isin);
        if (null != exposure) {
            publisherExposure(searchResult.symbol, exposure);
        }
    }

    @Override
    public boolean setMasterDelta(final double masterDelta) {
        return true;
    }

    @Override
    public boolean setHedgingEnabled(final boolean isHedgingEnabled) {
        return true;
    }

    @Override
    public boolean setKey(final int localID, final String isin) {
        return true;
    }

    @Override
    public boolean setValue(final int localID, final PositionValue item) {

        final Set<String> symbols = isinToSymbols.get(item.getKey());
        if (null != symbols) {
            for (final String symbol : symbols) {
                publisherExposure(symbol, item.position);
            }
        }
        return true;
    }

    private void publisherExposure(final String symbol, final double exposure) {
        final PKSExposure position = new PKSExposure(symbol, exposure);
        positionPublisher.publish(position);
    }

    @Override
    public void batchComplete() {

    }
}
