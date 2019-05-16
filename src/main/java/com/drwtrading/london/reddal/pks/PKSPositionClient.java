package com.drwtrading.london.reddal.pks;

import com.drwtrading.london.eeif.position.transport.cache.IPositionCmdListener;
import com.drwtrading.london.eeif.position.transport.data.ConstituentExposure;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PKSPositionClient implements ITransportCacheListener<String, ConstituentExposure>, IPositionCmdListener {

    private final Publisher<PKSExposures> positionPublisher;

    private final Map<String, CopyOnWriteArraySet<String>> isinToSymbols;
    private final Map<String, ConstituentExposure> isinExposures;

    private final Map<String, UltimateParentMapping> ultimateParents;
    private final Map<String, HashSet<UltimateParentMapping>> ultimateParentChildren;

    private final Map<String, PKSExposure> receivedExposures;

    public PKSPositionClient(final Publisher<PKSExposures> positionPublisher) {

        this.positionPublisher = positionPublisher;

        this.isinToSymbols = new HashMap<>();
        this.isinExposures = new HashMap<>();

        this.ultimateParents = new HashMap<>();
        this.ultimateParentChildren = new HashMap<>();

        this.receivedExposures = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        final CopyOnWriteArraySet<String> symbols =
                MapUtils.getMappedItem(isinToSymbols, searchResult.instID.isin, CopyOnWriteArraySet::new);
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
            final PKSExposure pksExposure = new PKSExposure(symbols, exposure, position);
            receivedExposures.put(isin, pksExposure);
        }
    }

    @Override
    public void batchComplete() {

        if (!receivedExposures.isEmpty()) {
            final Collection<PKSExposure> exposures = new HashSet<>(receivedExposures.values());
            final PKSExposures pksExposures = new PKSExposures(exposures);
            positionPublisher.publish(pksExposures);
            receivedExposures.clear();
        }
    }
}
