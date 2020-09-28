package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.IndexConstant;
import com.drwtrading.london.indy.transport.cache.IIndyCacheListener;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.indy.transport.data.IndexDef;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class IndyClient implements IIndyCacheListener {

    private static final Map<IndexConstant, EnumSet<FutureConstant>> FUTURES_FOR_INDEX = new EnumMap<>(IndexConstant.class);

    static {
        for (final FutureConstant future : FutureConstant.values()) {
            final IndexConstant index = future.index;

            if (index != null) {
                MapUtils.getMappedEnumSet(FUTURES_FOR_INDEX, index, FutureConstant.class).add(future);
            }
        }
    }

    private final FutureExpiryCalc futureExpiryCalc = new FutureExpiryCalc();

    private final Publisher<InstrumentDef> instDefs;
    private final Publisher<SymbolIndyData> symbolDescriptions;

    public IndyClient(final Publisher<InstrumentDef> instDefs, final Publisher<SymbolIndyData> symbolDescriptions) {
        this.instDefs = instDefs;
        this.symbolDescriptions = symbolDescriptions;
    }

    @Override
    public boolean setInstDef(final InstrumentDef instDef) {
        instDefs.publish(instDef);
        return true;
    }

    @Override
    public boolean setIndexDef(final IndexDef indexDef) {

        final IndexConstant indexConstant = IndexConstant.getIndex(indexDef.name);
        final EnumSet<FutureConstant> futuresForIndex = FUTURES_FOR_INDEX.get(indexConstant);

        if (futuresForIndex != null) {
            for (final FutureConstant future : futuresForIndex) {

                for (int i = 0; i < 3; i++) {
                    final String symbol = futureExpiryCalc.getFutureCode(future, i);
                    final InstrumentID instId = futureExpiryCalc.getInstID(future, i);

                    final SymbolIndyData data = new SymbolIndyData(instId, symbol, indexDef.name, indexDef.source);

                    symbolDescriptions.publish(data);
                }

            }
        }

        return true;
    }

    @Override
    public boolean setETFDef(final ETFDef etfDef) {
        for (final InstrumentDef instDef : etfDef.instDefs) {
            final SymbolIndyData data = new SymbolIndyData(instDef.instID, instDef.bbgCode, etfDef.indexDef.name, etfDef.indexDef.source);
            symbolDescriptions.publish(data);
        }

        return true;
    }
}
