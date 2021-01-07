package com.drwtrading.london.reddal.pks;

import com.drwtrading.london.eeif.position.transport.cache.IPositionCmdListener;
import com.drwtrading.london.eeif.position.transport.data.ConstituentExposure;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
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

public class PKSPositionClient {

    private final Publisher<PKSExposures> positionPublisher;

    private final Map<String, CopyOnWriteArraySet<String>> isinToSymbols;
    private final Map<String, ConstituentExposure> dryIsinExposures;
    private final Map<String, ConstituentExposure> dripIsinExposures;

    private final Map<String, UltimateParentMapping> ultimateParents;
    private final Map<String, HashSet<UltimateParentMapping>> ultimateParentChildren;

    private final Map<String, PKSExposure> receivedExposures;

    private final DryPksClient dryClient;
    private final DripPksClient dripClient;

    public PKSPositionClient(final Publisher<PKSExposures> positionPublisher) {

        this.positionPublisher = positionPublisher;

        this.isinToSymbols = new HashMap<>();
        this.dryIsinExposures = new HashMap<>();
        this.dripIsinExposures = new HashMap<>();

        this.ultimateParents = new HashMap<>();
        this.ultimateParentChildren = new HashMap<>();

        this.receivedExposures = new HashMap<>();

        this.dryClient = new DryPksClient();
        this.dripClient = new DripPksClient();
    }

    public DripPksClient getDripClient() {
        return dripClient;
    }

    public DryPksClient getDryClient() {
        return dryClient;
    }

    public void setSearchResult(final SearchResult searchResult) {

        final Set<String> symbols = MapUtils.getMappedItem(isinToSymbols, searchResult.instID.isin, CopyOnWriteArraySet::new);
        symbols.add(searchResult.symbol);

        updateDryAndDripExposure(searchResult.instID);
    }

    public void setUltimateParent(final UltimateParentMapping ultimateParent) {

        if (!ultimateParent.childISIN.equals(ultimateParent.parentID.isin)) {

            final UltimateParentMapping prevMapping = ultimateParents.put(ultimateParent.childISIN, ultimateParent);
            if (null != prevMapping) {
                final Set<UltimateParentMapping> parentChildren = ultimateParentChildren.get(prevMapping.parentID.isin);
                parentChildren.remove(prevMapping);
            }

            final Set<UltimateParentMapping> children = MapUtils.getMappedSet(ultimateParentChildren, ultimateParent.parentID.isin);
            children.add(ultimateParent);

            updateDryAndDripExposure(ultimateParent.parentID);
        } else {

            final UltimateParentMapping prevMapping = ultimateParents.remove(ultimateParent.childISIN);
            if (null != prevMapping) {
                final Set<UltimateParentMapping> children = ultimateParentChildren.get(prevMapping.parentID.isin);
                children.remove(ultimateParent);
                updateDryAndDripExposure(ultimateParent.parentID);
            }
        }
    }

    private void updateDryAndDripExposure(final InstrumentID parentID) {
        final ConstituentExposure dryParentPosition = dryIsinExposures.get(parentID.isin);
        if (null != dryParentPosition) {
            updateExposure(dryParentPosition, dryIsinExposures, this::updateDryExposure);
        }

        final ConstituentExposure dripParentPosition = dripIsinExposures.get(parentID.isin);
        if (null != dripParentPosition) {
            updateExposure(dripParentPosition, dripIsinExposures, this::updateDripExposure);
        }
    }

    private boolean updateExposure(final ConstituentExposure pksPosition, final Map<String, ConstituentExposure> exposures,
            final IExposureUpdate exposureUpdate) {

        final Set<UltimateParentMapping> children = ultimateParentChildren.get(pksPosition.getKey());
        if (null != children) {

            for (final UltimateParentMapping mapping : children) {

                final ConstituentExposure childPksPosition = exposures.get(mapping.childISIN);
                final double parentToChildRatio = mapping.parentToChildRatio;
                if (null == childPksPosition) {
                    exposureUpdate.updateExposure(mapping.childISIN, parentToChildRatio * pksPosition.exposure, 0d);
                } else {
                    exposureUpdate.updateExposure(mapping.childISIN, parentToChildRatio * pksPosition.exposure, childPksPosition.position);
                }
            }
        }

        final UltimateParentMapping ultimateParent = ultimateParents.get(pksPosition.isin);
        if (null == ultimateParent) {
            exposureUpdate.updateExposure(pksPosition.getKey(), pksPosition.exposure, pksPosition.position);
        } else {
            final ConstituentExposure parentPksPosition = exposures.get(ultimateParent.parentID.isin);
            if (null == parentPksPosition) {
                exposureUpdate.updateExposure(pksPosition.getKey(), 0d, pksPosition.position);
            } else {
                final double parentToChildRatio = ultimateParent.parentToChildRatio;
                exposureUpdate.updateExposure(pksPosition.getKey(), parentToChildRatio * parentPksPosition.exposure, pksPosition.position);
            }
        }

        return true;
    }

    private void updateDryExposure(final String isin, final double dryExposure, final double dryPosition) {

        final Set<String> symbols = isinToSymbols.get(isin);

        if (null != symbols) {
            final ConstituentExposure dripIsinExposure = dripIsinExposures.get(isin);

            final double dripExposure = dripIsinExposure != null ? dripIsinExposure.exposure : 0;
            final double dripPosition = dripIsinExposure != null ? dripIsinExposure.position : 0;

            final PKSExposure pksExposure = new PKSExposure(symbols, dryExposure, dryPosition, dripExposure, dripPosition);
            receivedExposures.put(isin, pksExposure);
        }
    }

    private void updateDripExposure(final String isin, final double dripExposure, final double dripPosition) {

        final Set<String> symbols = isinToSymbols.get(isin);

        if (null != symbols) {
            final ConstituentExposure dryIsinExposure = dryIsinExposures.get(isin);

            final double dryExposure = dryIsinExposure != null ? dryIsinExposure.exposure : 0;
            final double dryPosition = dryIsinExposure != null ? dryIsinExposure.position : 0;

            final PKSExposure pksExposure = new PKSExposure(symbols, dryExposure, dryPosition, dripExposure, dripPosition);
            receivedExposures.put(isin, pksExposure);
        }
    }

    private void batchCompleted() {

        if (!receivedExposures.isEmpty()) {
            final Collection<PKSExposure> exposures = new HashSet<>(receivedExposures.values());
            final PKSExposures pksExposures = new PKSExposures(exposures);
            positionPublisher.publish(pksExposures);
            receivedExposures.clear();
        }
    }

    public class DryPksClient implements ITransportCacheListener<String, ConstituentExposure>, IPositionCmdListener {

        @Override
        public boolean setHedgingEnabled(final boolean isHedgingEnabled) {
            return true;
        }

        @Override
        public boolean initialValue(final int transportID, final ConstituentExposure item) {
            dryIsinExposures.put(item.getKey(), item);
            return updateExposure(item, dryIsinExposures, PKSPositionClient.this::updateDryExposure);
        }

        @Override
        public boolean updateValue(final int transportID, final ConstituentExposure item) {
            return updateExposure(item, dryIsinExposures, PKSPositionClient.this::updateDryExposure);
        }

        @Override
        public void batchComplete() {
            batchCompleted();
        }
    }

    public class DripPksClient implements ITransportCacheListener<String, ConstituentExposure>, IPositionCmdListener {

        @Override
        public boolean setHedgingEnabled(final boolean isHedgingEnabled) {
            return true;
        }

        @Override
        public boolean initialValue(final int transportID, final ConstituentExposure item) {
            dripIsinExposures.put(item.getKey(), item);
            return updateExposure(item, dripIsinExposures, PKSPositionClient.this::updateDripExposure);
        }

        @Override
        public boolean updateValue(final int transportID, final ConstituentExposure item) {
            return updateExposure(item, dripIsinExposures, PKSPositionClient.this::updateDripExposure);
        }

        @Override
        public void batchComplete() {
            batchCompleted();
        }
    }
}
