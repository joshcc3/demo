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
    private final Map<String, PositionValue> isinExposures;

    public PKSPositionClient(final Publisher<PKSExposure> positionPublisher) {

        this.positionPublisher = positionPublisher;

        this.isinToSymbols = new HashMap<>();
        this.isinExposures = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        final HashSet<String> symbols = MapUtils.getMappedSet(isinToSymbols, searchResult.instID.isin);
        symbols.add(searchResult.symbol);

        final PositionValue pksPosition = isinExposures.get(searchResult.instID.isin);
        if (null != pksPosition) {
            publisherExposure(searchResult.symbol, pksPosition);
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
    public boolean initialValue(final int transportID, final PositionValue item) {

        isinExposures.put(item.getKey(), item);
        return updateValue(transportID, item);
    }

    @Override
    public boolean updateValue(final int transportID, final PositionValue pksPosition) {

        final Set<String> symbols = isinToSymbols.get(pksPosition.getKey());
        if (null != symbols) {
            for (final String symbol : symbols) {
                publisherExposure(symbol, pksPosition);
            }
        }
        return true;
    }

    private void publisherExposure(final String symbol, final PositionValue pksPosition) {
        final PKSExposure pks = new PKSExposure(symbol, pksPosition.exposure, pksPosition.position);
        positionPublisher.publish(pks);
    }

    @Override
    public void batchComplete() {

    }
}
