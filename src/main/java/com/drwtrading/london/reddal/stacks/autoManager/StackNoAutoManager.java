package com.drwtrading.london.reddal.stacks.autoManager;

import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.reddal.symbols.SymbolReferencePrice;

class StackNoAutoManager implements IStackInstTypeAutoManager {

    static final StackNoAutoManager INSTANCE = new StackNoAutoManager();

    private StackNoAutoManager() {
        // Singleton
    }

    @Override
    public void setConfigClient(final String nibblerName, final StackClientHandler cache) {
        // no-op
    }

    @Override
    public void configUpdated(final String nibbler, final StackConfigGroup configGroup) {
        // no-op
    }

    @Override
    public void serverConnectionLost(final String nibblerName) {
        // no-op
    }

    @Override
    public void addRefPrice(final SymbolReferencePrice refPrice) {
        // no-op
    }

    @Override
    public void setModLevels(final int bpsEquivalent) {
        // no-op
    }
}
