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

import static com.google.common.collect.Sets.newHashSet;

public class OpxlExDateSubscriber {

    public static final String OPXL_KEY = "eeif(names_going_ex_" + new SimpleDateFormat(DateTimeUtil.DATE_FILE_FORMAT).format(new Date()) + ")";

    private final Publisher<Throwable> errorPublisher;
    private final Publisher<SymbolsGoingEx> publisher;

    public OpxlExDateSubscriber(final Publisher<Throwable> errorPublisher, final Publisher<SymbolsGoingEx> publisher) {
        this.errorPublisher = errorPublisher;
        this.publisher = publisher;
    }

    public void onOpxlData(final OpxlData opxlData) {
        String headerString = Arrays.asList(opxlData.getData()[0]).toString();
        if (!headerString.equals("[Ticker, Ex Date]")) {
            errorPublisher.publish(new Throwable("Ex-date: bad headers " + headerString));
            return;
        }
        List<Object[]> rows = Arrays.asList(opxlData.getData()).subList(1, opxlData.getData().length);
        HashSet<String> symbols = new HashSet<>();
        for (final Object[] data : rows) {
            final String symbol = data[0].toString();
            final String value = data[1].toString();
            symbols.add(symbol);
        }
        publisher.publish(new SymbolsGoingEx(symbols));
    }

    public static class SymbolsGoingEx {
        public final ImmutableSet<String> symbols;

        public SymbolsGoingEx(Set<String> symbols) {
            this.symbols = ImmutableSet.copyOf(symbols);
        }
    }
}
