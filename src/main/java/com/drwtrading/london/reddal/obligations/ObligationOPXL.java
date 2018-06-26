package com.drwtrading.london.reddal.obligations;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.opxl.AOpxlReader;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ObligationOPXL extends AOpxlReader<Map<String, RFQObligation>> {


    final static String TOPIC = "eeif(etf_rfq_obligation)";

    final Consumer<Map<String, RFQObligation>> updates;

    public ObligationOPXL(SelectIO selectIO, IResourceMonitor<ReddalComponents> monitor, ReddalComponents component, Path path, Consumer<Map<String, RFQObligation>> updates) {
        super(selectIO, monitor, component, TOPIC, path);
        this.updates = updates;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Map<String, RFQObligation> parseTable(Object[][] opxlTable) {

        Map<String, RFQObligation> obligationHashMap = new HashMap<>();
        Object[] headerRow = opxlTable[0];

        if (!"symbol".equals(headerRow[0].toString().toLowerCase())) {
            return obligationHashMap;
        }

        List<Double> notionals = Arrays.stream(headerRow)
                .skip(1)
                .filter(o -> !o.toString().trim().isEmpty())
                .map(o -> o.toString().toLowerCase().replaceAll("m", "000000"))
                .map(Double::valueOf)
                .collect(Collectors.toList());

        Arrays.stream(opxlTable)
                .skip(1)
                .filter(objects -> objects.length == headerRow.length && !objects[0].toString().isEmpty())
                .map(Arrays::asList).forEach(objects -> {
            String symbol = objects.get(0).toString();
            List<Obligation> obligations = new ArrayList<>();
            for (int i = 0; i < notionals.size() && 1 + i < objects.size(); i++) {
                Object bpsString = objects.get(1 + i);
                if (bpsString.toString().trim().isEmpty()) {
                    continue;
                }
                double bps = Double.valueOf(bpsString.toString());
                double notional = notionals.get(i);
                obligations.add(new Obligation(notional, bps));
            }
            RFQObligation obligation = new RFQObligation(symbol, obligations);
            obligationHashMap.put(symbol, obligation);
        });

        return obligationHashMap;
    }

    @Override
    protected void handleUpdate(Map<String, RFQObligation> values) {
        updates.accept(values);
    }

    @Override
    protected void handleError(ReddalComponents component, String msg) {

    }

    @Override
    protected void handleError(ReddalComponents component, String msg, Throwable t) {

    }

}