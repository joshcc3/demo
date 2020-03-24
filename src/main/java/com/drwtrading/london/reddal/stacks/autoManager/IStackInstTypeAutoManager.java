package com.drwtrading.london.reddal.stacks.autoManager;

import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.reddal.symbols.SymbolReferencePrice;

interface IStackInstTypeAutoManager {

    public void setConfigClient(final String nibblerName, final StackClientHandler cache);

    public void configUpdated(final String nibbler, final StackConfigGroup configGroup);

    public void serverConnectionLost(final String nibblerName);

    public void addRefPrice(final SymbolReferencePrice refPrice);

    public void setModLevels(final int bpsEquivalent);
}
