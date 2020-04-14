package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.indy.transport.cache.IIndyCacheListener;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.indy.transport.data.IndexDef;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import org.jetlang.channels.Publisher;

public class IndyClient implements IIndyCacheListener {

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
