package com.drwtrading.london.reddal;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

public class RemoteOrderMatcher {

    private final Pattern symbolPattern;
    private final Collection<RemoteOrderType> orderTypes;
    private final Collection<String> tags;
    private final Collection<String> mics;

    RemoteOrderMatcher(final Pattern symbolPattern, final Set<RemoteOrderType> orderTypes, final Collection<String> tags,
            final Collection<String> mics) {

        this.symbolPattern = symbolPattern;
        this.orderTypes = orderTypes;
        this.tags = tags;
        this.mics = mics;
    }

    public boolean matches(final String symbol, final RemoteOrderType orderType, final String tag, final String mic) {
        return symbolPattern.matcher(symbol).find() && (null == orderTypes || orderTypes.contains(orderType)) &&
                (null == tags || tags.contains(tag)) && (null == mics || null == mic || mics.contains(mic));
    }
}
