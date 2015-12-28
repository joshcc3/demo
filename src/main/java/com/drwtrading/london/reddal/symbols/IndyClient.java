package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.indy.transport.cache.IIndyCacheListener;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.indy.transport.data.IndexDef;
import com.drwtrading.london.indy.transport.data.InstrumentDef;

public class IndyClient implements IIndyCacheListener {

    private final TypedChannel<InstrumentDef> instDefs;

    public IndyClient(final TypedChannel<InstrumentDef> instDefs) {

        this.instDefs = instDefs;
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
        return true;
    }
}
