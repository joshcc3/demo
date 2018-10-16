package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.indy.transport.cache.IIndyCacheListener;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.indy.transport.data.IndexDef;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import org.jetlang.channels.Publisher;

public class IndyClient implements IIndyCacheListener {


    private final Publisher<InstrumentDef> instDefs;
    private final Publisher<SymbolDescription> symbolDescriptions;

    public IndyClient(final Publisher<InstrumentDef> instDefs, Publisher<SymbolDescription> symbolDescriptions) {
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
        for (InstrumentDef instDef : etfDef.instDefs) {
            symbolDescriptions.publish(new SymbolDescription(instDef.instID, instDef.bbgCode, etfDef.indexDef.name));
        }
        return true;
    }
}
