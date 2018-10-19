package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import com.drwtrading.london.jetlang.DefaultJetlangFactory;
import com.drwtrading.london.jetlang.FiberGroup;
import com.drwtrading.london.reddal.util.SelectIOFiber;

class ReddalFibers {

    final FiberGroup fiberGroup;
    final FiberBuilder logging;
    final FiberBuilder ui;
    final FiberBuilder metaData;
    final FiberBuilder remoteOrders;
    final FiberBuilder mrPhil;
    final FiberBuilder indy;
    final FiberBuilder settings;
    final FiberBuilder ladderRouter;

    ReddalFibers(final ReddalChannels channels, final DefaultJetlangFactory factory, final SelectIO uiSelectIO, final IErrorLogger logger) {

        fiberGroup = new FiberGroup(factory, "Fibers", channels.error);
        logging = fiberGroup.create("Logging");
        ui = fiberGroup.wrap(new SelectIOFiber(uiSelectIO, logger, "UI"), "UI");
        ladderRouter = fiberGroup.create("Ladder");
        metaData = fiberGroup.create("Metadata");
        remoteOrders = fiberGroup.create("Remote orders");
        mrPhil = fiberGroup.create("Mr Phil");
        indy = fiberGroup.create("Indy");
        settings = fiberGroup.create("Settings");
    }

    void start() {
        fiberGroup.start();
    }
}
