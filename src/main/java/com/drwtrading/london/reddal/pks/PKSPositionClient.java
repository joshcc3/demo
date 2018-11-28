package com.drwtrading.london.reddal.pks;

import com.drwtrading.london.eeif.position.transport.cache.IPositionCmdListener;
import com.drwtrading.london.eeif.position.transport.data.ConstituentExposure;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PKSPositionClient implements ITransportCacheListener<String, ConstituentExposure>, IPositionCmdListener {

    private final Publisher<PKSExposure> positionPublisher;

    private final Map<String, HashSet<String>> isinToSymbols;
    private final Map<String, ConstituentExposure> isinExposures;

    private final Map<String, UltimateParentMapping> ultimateParents;
    private final Map<String, HashSet<UltimateParentMapping>> ultimateParentChildren;

    public PKSPositionClient(final Publisher<PKSExposure> positionPublisher) {

        this.positionPublisher = positionPublisher;

        this.isinToSymbols = new HashMap<>();
        this.isinExposures = new HashMap<>();

        this.ultimateParents = new HashMap<>();
        this.ultimateParentChildren = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        final HashSet<String> symbols = MapUtils.getMappedSet(isinToSymbols, searchResult.instID.isin);
        symbols.add(searchResult.symbol);

        final ConstituentExposure pksPosition = isinExposures.get(searchResult.instID.isin);
        if (null != pksPosition) {
            updateValue(pksPosition);
        }
    }

    public void setUltimateParent(final UltimateParentMapping ultimateParent) {

        if (!ultimateParent.childISIN.equals(ultimateParent.parentID.isin)) {

            final UltimateParentMapping prevMapping = ultimateParents.put(ultimateParent.childISIN, ultimateParent);
            if (null != prevMapping) {
                final HashSet<UltimateParentMapping> parentChildren = ultimateParentChildren.get(prevMapping.parentID.isin);
                parentChildren.remove(prevMapping);
            }

            final Set<UltimateParentMapping> children = MapUtils.getMappedSet(ultimateParentChildren, ultimateParent.parentID.isin);
            children.add(ultimateParent);

            final ConstituentExposure parentPosition = isinExposures.get(ultimateParent.parentID.isin);
            if (null != parentPosition) {
                updateValue(parentPosition);
            }
        }
    }

    @Override
    public boolean setHedgingEnabled(final boolean isHedgingEnabled) {
        return true;
    }

    @Override
    public boolean initialValue(final int transportID, final ConstituentExposure pksPosition) {

        isinExposures.put(pksPosition.getKey(), pksPosition);
        return updateValue(pksPosition);
    }

    @Override
    public boolean updateValue(final int transportID, final ConstituentExposure pksPosition) {

        return updateValue(pksPosition);
    }

    private boolean updateValue(final ConstituentExposure pksPosition) {

        final Set<UltimateParentMapping> children = ultimateParentChildren.get(pksPosition.getKey());
        if (null != children) {

            for (final UltimateParentMapping mapping : children) {

                final ConstituentExposure childPksPosition = isinExposures.get(mapping.childISIN);
                if (null == childPksPosition) {
                    updateExposure(mapping.childISIN, mapping.parentToChildRatio * pksPosition.exposure, 0d);
                } else {
                    updateExposure(mapping.childISIN, mapping.parentToChildRatio * pksPosition.exposure, childPksPosition.position);
                }
            }
        }

        final UltimateParentMapping ultimateParent = ultimateParents.get(pksPosition.isin);
        if (null == ultimateParent) {
            updateExposure(pksPosition.getKey(), pksPosition.exposure, pksPosition.position);
        } else {
            final ConstituentExposure parentPksPosition = isinExposures.get(ultimateParent.parentID.isin);
            if (null == parentPksPosition) {
                updateExposure(pksPosition.getKey(), 0d, pksPosition.position);
            } else {
                updateExposure(pksPosition.getKey(), ultimateParent.parentToChildRatio * parentPksPosition.exposure, pksPosition.position);
            }
        }
        return true;
    }

    private void updateExposure(final String isin, final double exposure, final double position) {

        final Set<String> symbols = isinToSymbols.get(isin);
        if (null != symbols) {
            for (final String symbol : symbols) {
                publisherExposure(symbol, exposure, position);
            }
        }
    }

    private void publisherExposure(final String symbol, final double exposure, final double position) {

        final PKSExposure pks = new PKSExposure(symbol, exposure, position);
        positionPublisher.publish(pks);
    }

    @Override
    public void batchComplete() {

    }
}
