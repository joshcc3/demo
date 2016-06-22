package com.drwtrading.london.reddal.util;

import drw.opxl.OpxlCallbacks;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;
import org.jetlang.fibers.Fiber;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ReconnectingOPXLClient extends OpxlCallbacks {

    final String opxlHost;
    final int opxlPort;
    final Consumer<OpxlData> dataConsumer;
    final Set<String> keys;
    final Fiber fiber;
    final Publisher<Throwable> errors;

    public ReconnectingOPXLClient(final String opxlHost, final int opxlPort, final Consumer<OpxlData> dataConsumer, final Set<String> keys,
            final Fiber fiber, final Publisher<Throwable> errors) {

        this.opxlHost = opxlHost;
        this.opxlPort = opxlPort;
        this.dataConsumer = dataConsumer;
        this.keys = keys;
        this.fiber = fiber;
        this.errors = errors;
        fiber.execute(this::connect);
    }

    public void connect() {
        final OpxlClient client = new OpxlClient(opxlHost, opxlPort, keys, this);
        try {
            client.connect();
        } catch (final IOException e) {
            errors.publish(e);
        }
    }

    @Override
    public void on(final OpxlData data) {
        dataConsumer.accept(data);
    }

    @Override
    public void onDisconnect() {
        errors.publish(new Exception("OPXL Disconnected: " + keys + " (" + opxlHost + ":" + opxlPort + ")"));
        fiber.schedule(this::connect, 3, TimeUnit.SECONDS);
    }

}
