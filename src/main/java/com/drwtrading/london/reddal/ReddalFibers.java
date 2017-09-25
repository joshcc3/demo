package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.jetlang.FiberGroup;
import com.drwtrading.london.jetlang.JetlangFactory;
import com.drwtrading.london.jetlang.stats.MonitoredJetlangFactory;
import org.jetlang.fibers.Fiber;

class ReddalFibers {

    final JetlangFactory jetlangFactory;
    final FiberGroup fiberGroup;
    final Fiber starter;
    final FiberBuilder logging;
    final FiberBuilder ui;
    final FiberBuilder stats;
    final FiberBuilder marketData;
    final FiberBuilder metaData;
    final FiberBuilder workingOrders;
    final FiberBuilder selecta;
    final FiberBuilder remoteOrders;
    final FiberBuilder opxlPosition;
    final FiberBuilder opxlText;
    final FiberBuilder mrPhil;
    final FiberBuilder indy;
    final FiberBuilder watchdog;
    final FiberBuilder settings;
    final FiberBuilder ladderRouter;
    final FiberBuilder shredderRouter;
    final FiberBuilder contracts;

    ReddalFibers(final ReddalChannels channels, final MonitoredJetlangFactory factory) {
        jetlangFactory = factory;
        fiberGroup = new FiberGroup(jetlangFactory, "Fibers", channels.error);
        starter = jetlangFactory.createFiber("Starter");
        fiberGroup.wrap(starter, "Starter");
        logging = fiberGroup.create("Logging");
        ui = fiberGroup.create("UI");
        ladderRouter = fiberGroup.create("Ladder");
        shredderRouter = fiberGroup.create("Shredder");
        stats = fiberGroup.create("Stats");
        marketData = fiberGroup.create("Market data");
        metaData = fiberGroup.create("Metadata");
        workingOrders = fiberGroup.create("Working orders");
        selecta = fiberGroup.create("Selecta");
        remoteOrders = fiberGroup.create("Remote orders");
        opxlPosition = fiberGroup.create("OPXL Position");
        opxlText = fiberGroup.create("OPXL Text");
        mrPhil = fiberGroup.create("Mr Phil");
        indy = fiberGroup.create("Indy");
        watchdog = fiberGroup.create("Watchdog");
        settings = fiberGroup.create("Settings");
        contracts = fiberGroup.create("Contracts");
    }

    void onStart(final Runnable runnable) {
        starter.execute(runnable);
    }

    void start() {
        fiberGroup.start();
    }
}
