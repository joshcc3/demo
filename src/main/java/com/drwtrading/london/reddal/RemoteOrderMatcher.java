package com.drwtrading.london.reddal;

import java.util.Collection;
import java.util.regex.Pattern;

public class RemoteOrderMatcher {

    public final Pattern symbolPattern;
    public final Collection<String> orderTypes;
    private final Collection<String> tags;
    private final Collection<String> mics;

    public RemoteOrderMatcher(final Pattern symbolPattern, final Collection<String> orderTypes, final Collection<String> tags,
            final Collection<String> mics) {

        this.symbolPattern = symbolPattern;
        this.orderTypes = orderTypes;
        this.tags = tags;
        this.mics = mics;
    }

    public boolean matches(final String symbol, final String orderType, final String tag, final String mic) {
        return symbolPattern.matcher(symbol).find() && (null == orderTypes || orderTypes.contains(orderType)) &&
                (null == tags || tags.contains(tag)) && (null == mics || null == mic || mics.contains(mic));
    }
}
