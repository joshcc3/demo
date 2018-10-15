package com.drwtrading.london.reddal;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;

import java.util.LinkedHashMap;
import java.util.Map;

public class RemoteOrderServerResolver {

    private final LinkedHashMap<String, RemoteOrderMatcher> matchers;

    RemoteOrderServerResolver(final LinkedHashMap<String, RemoteOrderMatcher> matchers) {
        this.matchers = matchers;
    }

    public String resolveToServerName(final String symbol, final RemoteOrderType orderType, final String tag, final String mic) {

        for (final Map.Entry<String, RemoteOrderMatcher> entry : matchers.entrySet()) {
            if (entry.getValue().matches(symbol, orderType, tag, mic)) {
                return entry.getKey();
            }
        }
        return null;
    }
}

