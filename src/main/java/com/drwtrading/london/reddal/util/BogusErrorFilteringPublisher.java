package com.drwtrading.london.reddal.util;

import org.jetlang.channels.Publisher;

public class BogusErrorFilteringPublisher implements Publisher<Throwable> {

    public final Publisher<Throwable> publisher;

    public BogusErrorFilteringPublisher(final Publisher<Throwable> publisher) {

        this.publisher = publisher;
    }

    @Override
    public void publish(final Throwable msg) {

        if (!msg.toString().contains("Connection reset by peer") && !msg.toString().contains("Connection timed out") &&
                !msg.toString().contains("Message delayed")) {

            publisher.publish(msg);
        }
    }
}
