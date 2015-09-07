package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.protocols.photon.marketdata.FutureLegStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureStrategyStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureStrategyType;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.util.Struct;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FuturesContractSetGenerator {

    final Set<String> excludedMarkets = ImmutableSet.of("FEXD");

    Map<String, TreeMap<Long, InstrumentDefinitionEvent>> marketToOutrightsByExpiry = new MapMaker().makeComputingMap(new Function<String, TreeMap<Long, InstrumentDefinitionEvent>>() {
        @Override
        public TreeMap<Long, InstrumentDefinitionEvent> apply(final String from) {
            return new TreeMap<>();
        }
    });

    Map<String, HashMap<SpreadExpiries, InstrumentDefinitionEvent>> marketToSpreads = new MapMaker().makeComputingMap(new Function<String, HashMap<SpreadExpiries, InstrumentDefinitionEvent>>() {
        @Override
        public HashMap<SpreadExpiries, InstrumentDefinitionEvent> apply(final String from) {
            return new HashMap<>();
        }
    });


    Map<String, SpreadContractSet> setByFrontMonth = new HashMap<>();

    final Publisher<SpreadContractSet> publisher;

    public FuturesContractSetGenerator(final Publisher<SpreadContractSet> publisher) {
        this.publisher = publisher;
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureOutrightStructure) {
            long timestamp = ((FutureOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getExpiry().getTimestamp();
            marketToOutrightsByExpiry.get(instrumentDefinitionEvent.getMarket()).put(timestamp, instrumentDefinitionEvent);
            publishContractSet(instrumentDefinitionEvent.getMarket());
        } else if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureStrategyStructure) {
            FutureStrategyStructure structure = (FutureStrategyStructure) instrumentDefinitionEvent.getInstrumentStructure();
            if (structure.getType() == FutureStrategyType.SPREAD) {
                FutureLegStructure frontLeg = structure.getLegs().get(0);
                FutureLegStructure backLeg = structure.getLegs().get(1);
                SpreadExpiries spreadExpiries = new SpreadExpiries(frontLeg.getExpiry().getTimestamp(), backLeg.getExpiry().getTimestamp());
                marketToSpreads.get(instrumentDefinitionEvent.getMarket()).put(spreadExpiries, instrumentDefinitionEvent);
                publishContractSet(instrumentDefinitionEvent.getMarket());
            }
        }
    }

    private void publishContractSet(final String market) {
        if(!excludedMarkets.contains(market)) {
            SpreadContractSet spreadContractSet = updateMarket(market);
            if (spreadContractSet != null && !spreadContractSet.equals(setByFrontMonth.put(spreadContractSet.front, spreadContractSet))) {
                publisher.publish(spreadContractSet);
            }
        }
    }

    private SpreadContractSet updateMarket(final String market) {
        TreeMap<Long, InstrumentDefinitionEvent> outrights = marketToOutrightsByExpiry.get(market);
        if (outrights.size() == 0) {
            return null;
        } else if (outrights.size() == 1) {
            return new SpreadContractSet(outrights.firstEntry().getValue().getSymbol(), null, null);
        } else if (outrights.size() > 1) {
            ArrayList<Map.Entry<Long, InstrumentDefinitionEvent>> values = new ArrayList<>(outrights.entrySet());
            Map.Entry<Long, InstrumentDefinitionEvent> front = values.get(0);
            Map.Entry<Long, InstrumentDefinitionEvent> back = values.get(1);
            HashMap<SpreadExpiries, InstrumentDefinitionEvent> spreads = marketToSpreads.get(market);
            InstrumentDefinitionEvent spread = spreads.get(new SpreadExpiries(front.getKey(), back.getKey()));
            if (spread != null) {
                return new SpreadContractSet(front.getValue().getSymbol(), back.getValue().getSymbol(), spread.getSymbol());
            } else {
                return new SpreadContractSet(front.getValue().getSymbol(), back.getValue().getSymbol(), null);
            }
        }
        return null;
    }


    public static class SpreadExpiries extends Struct {
        public final Long frontExpiry;
        public final Long backExpiry;

        public SpreadExpiries(final Long frontExpiry, final Long backExpiry) {
            this.frontExpiry = frontExpiry;
            this.backExpiry = backExpiry;
        }
    }

}
