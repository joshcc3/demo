package com.drwtrading.london.reddal;

import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class ResolverTest {

    @Test
    public void test_resolver() throws Exception {
        LinkedHashMap<String, Environment.RemoteOrderMatcher> matchers = new LinkedHashMap<String, Environment.RemoteOrderMatcher>();
        matchers.put("chix", new Environment.RemoteOrderMatcher(Pattern.compile("^[^:]* IX$"), ImmutableSet.of("HAWK", "MANUAL")));
        matchers.put("xetra", new Environment.RemoteOrderMatcher(Pattern.compile("^[^:]* GY$"), ImmutableSet.of("HAWK", "MANUAL", "MKT_CLOSE")));
        matchers.put("euronext", new Environment.RemoteOrderMatcher(Pattern.compile("^[^:]* (FP|BB|PL|NA)$"), ImmutableSet.of("HAWK", "MANUAL")));
        matchers.put("eurex-fast", new Environment.RemoteOrderMatcher(Pattern.compile("^(FESB|FSTB|FXXP|FSTX)(H|M|U|Z)(1|2|3|4|5|6|7|8|9|0)$"), ImmutableSet.of("HAWK", "MANUAL")));
        matchers.put("baml", new Environment.RemoteOrderMatcher(Pattern.compile(".*"), ImmutableSet.of("HAWK", "MANUAL", "MKT_CLOSE")));

        Environment.RemoteOrderServerResolver resolver = Environment.getRemoteOrderServerResolver(matchers);

        assertEquals("chix", resolver.resolveToServerName("FOO IX", RemoteOrderType.HAWK));
        assertEquals("chix", resolver.resolveToServerName("FOO GY IX", RemoteOrderType.HAWK));
        assertEquals("euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.HAWK));
        assertEquals("euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.MANUAL));
        assertEquals("baml", resolver.resolveToServerName("FOO FP", RemoteOrderType.MKT_CLOSE));
        assertEquals("baml", resolver.resolveToServerName("SF:FOO IX", RemoteOrderType.HAWK));
        assertEquals("xetra", resolver.resolveToServerName("FOO GY", RemoteOrderType.MKT_CLOSE));
        assertEquals("baml", resolver.resolveToServerName("SF:FOO GY", RemoteOrderType.MKT_CLOSE));
        assertEquals("euronext", resolver.resolveToServerName("UNA NA", RemoteOrderType.MANUAL));
        assertEquals("euronext", resolver.resolveToServerName("FP FP", RemoteOrderType.MANUAL));
        assertEquals("eurex-fast", resolver.resolveToServerName("FESBU4", RemoteOrderType.MANUAL));
    }
}
