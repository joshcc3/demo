package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.google.common.collect.ImmutableSet;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpxlExDateSubscriber {

    public static final String OPXL_KEY = "eeif(isin_going_ex_" + new SimpleDateFormat(DateTimeUtil.DATE_FILE_FORMAT).format(new Date()) + ")";

    private final Publisher<Throwable> errorPublisher;
    private final Publisher<IsinsGoingEx> publisher;

    public OpxlExDateSubscriber(final Publisher<Throwable> errorPublisher, final Publisher<IsinsGoingEx> publisher) {
        this.errorPublisher = errorPublisher;
        this.publisher = publisher;
    }

    public void onOpxlData(final OpxlData opxlData) {
        String headerString = Arrays.asList(opxlData.getData()[0]).toString();
        if (!headerString.equals("[ISIN, Ex Date]")) {
            errorPublisher.publish(new Throwable("Ex-date: bad headers " + headerString));
            return;
        }
        List<Object[]> rows = Arrays.asList(opxlData.getData()).subList(1, opxlData.getData().length);
        HashSet<String> isins = new HashSet<>();
        for (final Object[] data : rows) {
            final String isin = data[0].toString();
            final String value = data[1].toString();
            isins.add(isin);
        }
        publisher.publish(new IsinsGoingEx(isins));
    }

    public static class IsinsGoingEx {
        public final ImmutableSet<String> isins;

        public IsinsGoingEx(Set<String> isins) {
            this.isins = ImmutableSet.copyOf(isins);
        }
    }
}
