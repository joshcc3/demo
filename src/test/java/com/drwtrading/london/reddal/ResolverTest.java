package com.drwtrading.london.reddal;

import com.drwtrading.london.config.Config;
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


    @Test
    public void test_prod_equities_resolver() throws Exception {

        Environment environment =new Environment(Config.fromFile("etc/prod-equities.properties"));
        Environment.RemoteOrderServerResolver resolver = environment.getServerResolver();

        assertEquals("nibbler-chix", resolver.resolveToServerName("FOO IX", RemoteOrderType.HAWK));
        assertEquals("nibbler-chix", resolver.resolveToServerName("FOO GY IX", RemoteOrderType.HAWK));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.HAWK));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.MANUAL));
        assertEquals("nibbler-baml", resolver.resolveToServerName("FOO FP", RemoteOrderType.MKT_CLOSE));
        assertEquals("nibbler-xetra", resolver.resolveToServerName("FOO GY", RemoteOrderType.MKT_CLOSE));
        assertEquals("nibbler-baml", resolver.resolveToServerName("SF:FOO GY", RemoteOrderType.MKT_CLOSE));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("UNA NA", RemoteOrderType.MANUAL));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("FP FP", RemoteOrderType.MANUAL));
        assertEquals("nibbler-baml", resolver.resolveToServerName("SF:SAN SQ", RemoteOrderType.MANUAL));



    }


    @Test
    public void test_prod_futures_resolver() throws Exception {

        Environment environment =new Environment(Config.fromFile("etc/prod-futures.properties"));
        Environment.RemoteOrderServerResolver resolver = environment.getServerResolver();

        assertEquals("nibbler-ice", resolver.resolveToServerName("ERZ4", RemoteOrderType.MANUAL));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ERZ4", RemoteOrderType.GTC));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ZZ4", RemoteOrderType.MANUAL));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ZZ4", RemoteOrderType.GTC));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ZZ4-ZH5", RemoteOrderType.GTC));

        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FESBU4", RemoteOrderType.MANUAL));
        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FSTBU4", RemoteOrderType.HAWK));
        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FXXPU4", RemoteOrderType.TAKER));
        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FSTXU4", RemoteOrderType.MANUAL));

        assertEquals("nibbler-eurex-2", resolver.resolveToServerName("FSTCU4", RemoteOrderType.MANUAL));
        assertEquals("nibbler-eurex-2", resolver.resolveToServerName("FSTCU4", RemoteOrderType.HAWK));
        assertEquals("nibbler-eurex-2", resolver.resolveToServerName("FSTCU4", RemoteOrderType.TAKER));

        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.MANUAL));

        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FSTBZ4-FSTBH5", RemoteOrderType.MANUAL));
        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.HAWK));
        assertEquals("nibbler-eurex-fast", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.TAKER));

        assertEquals("LITTERBOX1GTC", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.GTC));
        assertEquals("LITTERBOX1GTC", resolver.resolveToServerName("FSTBU4-FSTBZ4", RemoteOrderType.GTC));
        assertEquals("LITTERBOX1GTC", resolver.resolveToServerName("FSTBU4-FSTBZ4", RemoteOrderType.TICKTAKER));


        assertEquals("LITTERBOX1DIV", resolver.resolveToServerName("FEXDZ4", RemoteOrderType.MANUAL));
        assertEquals("LITTERBOX1GTC", resolver.resolveToServerName("FEXDZ4", RemoteOrderType.GTC));

        assertEquals("LITTERBOX1DIV", resolver.resolveToServerName("FEXDZ4-FEXDZ0", RemoteOrderType.MANUAL));
        assertEquals("LITTERBOX1GTC", resolver.resolveToServerName("FEXDZ4-FEXDZ0", RemoteOrderType.GTC));
    }




}


