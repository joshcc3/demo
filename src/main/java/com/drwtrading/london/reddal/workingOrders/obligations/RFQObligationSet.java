package com.drwtrading.london.reddal.workingOrders.obligations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RFQObligationSet {
    public final List<Double> notionals;
    public final Map<String, RFQObligation> obligationMap;

    RFQObligationSet(List<Double> notionals, Map<String, RFQObligation> obligationMap) {
        this.notionals = Collections.unmodifiableList(notionals);
        this.obligationMap = Collections.unmodifiableMap(obligationMap);
    }
}
