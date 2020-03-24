package com.drwtrading.london.reddal.workingOrders.obligations.rfq;

import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.reddal.OPXLComponents;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RFQObligationOPXL extends AOpxlLoggingReader<OPXLComponents, RFQObligationSet> {

    private static final String TOPIC = "eeif(etf_rfq_obligation)";
    public static final RFQObligationSet EMPTY_OBLIGATIONS = new RFQObligationSet(Collections.emptyList(), Collections.emptyMap());

    private final Consumer<RFQObligationSet> updates;

    public RFQObligationOPXL(final SelectIO opxlSelectIO, final IFuseBox<OPXLComponents> monitor, final OPXLComponents component,
            final Path path, final Consumer<RFQObligationSet> updates) {

        super(opxlSelectIO, opxlSelectIO, monitor, component, TOPIC, path);
        this.updates = updates;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected RFQObligationSet parseTable(final Object[][] opxlTable) {

        final Map<String, RFQObligation> obligationHashMap = new HashMap<>();
        final Object[] headerRow = opxlTable[0];

        if (!"symbol".equals(headerRow[0].toString().toLowerCase())) {
            return EMPTY_OBLIGATIONS;
        }

        final List<Double> notionals = Arrays.stream(headerRow).skip(1).filter(o -> !o.toString().trim().isEmpty()).map(
                o -> o.toString().toLowerCase().replaceAll("m", "000000")).map(Double::valueOf).collect(Collectors.toList());

        Arrays.stream(opxlTable).skip(1).filter(objects -> objects.length == headerRow.length && !objects[0].toString().isEmpty()).map(
                Arrays::asList).forEach(objects -> {
            final String symbol = objects.get(0).toString();
            final List<RFQObligationValue> obligations = new ArrayList<>();
            for (int i = 0; i < notionals.size() && 1 + i < objects.size(); i++) {
                final Object bpsString = objects.get(1 + i);
                if (bpsString.toString().trim().isEmpty()) {
                    continue;
                }
                final double bps = Double.valueOf(bpsString.toString());
                final double notional = notionals.get(i);
                obligations.add(new RFQObligationValue(notional, bps));
            }
            final RFQObligation obligation = new RFQObligation(symbol, obligations);
            obligationHashMap.put(symbol, obligation);
        });

        return new RFQObligationSet(notionals, obligationHashMap);
    }

    @Override
    protected void handleUpdate(final RFQObligationSet prevValue, final RFQObligationSet values) {
        updates.accept(values);
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg) {

    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg, final Throwable t) {

    }
}