package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.config.Config;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import eeif.execution.RemoteOrderType;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class ResolverTest {

    @Test
    public void test_resolver() throws Exception {
        LinkedHashMap<String, Environment.RemoteOrderMatcher> matchers = new LinkedHashMap<String, Environment.RemoteOrderMatcher>();
        matchers.put("chix", new Environment.RemoteOrderMatcher(Pattern.compile(" IX$"), ImmutableSet.of("HAWK", "MANUAL"), null, nullsRaw));
        matchers.put("xetra", new Environment.RemoteOrderMatcher(Pattern.compile(" GY$"), ImmutableSet.of("HAWK", "MANUAL", "MKT_CLOSE"), null, nullsRaw));
        matchers.put("euronext", new Environment.RemoteOrderMatcher(Pattern.compile(" (FP|BB|PL|NA)$"), ImmutableSet.of("HAWK", "MANUAL"), null, nullsRaw));
        matchers.put("eurex-fast",
                new Environment.RemoteOrderMatcher(Pattern.compile("^(FESB|FSTB|FXXP|FSTX)(H|M|U|Z)(1|2|3|4|5|6|7|8|9|0)$"),
                        ImmutableSet.of("HAWK", "MANUAL"), null, null));
        matchers.put("baml", new Environment.RemoteOrderMatcher(Pattern.compile(".*"), ImmutableSet.of("HAWK", "MANUAL", "MKT_CLOSE"), null, nullsRaw));

        Environment.RemoteOrderServerResolver resolver = Environment.getRemoteOrderServerResolver(matchers);

        assertEquals("chix", resolver.resolveToServerName("FOO IX", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("chix", resolver.resolveToServerName("FOO GY IX", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("xetra", resolver.resolveToServerName("FOO GY", RemoteOrderType.MKT_CLOSE, "CHAD", null));
        assertEquals("euronext", resolver.resolveToServerName("UNA NA", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("euronext", resolver.resolveToServerName("FP FP", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("eurex-fast", resolver.resolveToServerName("FESBU4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("baml", resolver.resolveToServerName("FOO AV", RemoteOrderType.MKT_CLOSE, "CHAD", null));
        assertEquals("baml", resolver.resolveToServerName("FOO GA", RemoteOrderType.HAWK, "CHAD", null));
    }

    @Test
    public void test_local_equities_resolver() throws IOException, ConfigException {
        Environment environment = new Environment(new Config(Paths.get("etc/local.properties")).getRoot());
        Environment.RemoteOrderServerResolver resolver = environment.getServerResolver();

    }

    @Test
    public void test_prod_equities_resolver() throws Exception {

        Environment environment = new Environment(new Config(Paths.get("etc/prod-equities.properties")).getRoot());
        Environment.RemoteOrderServerResolver resolver = environment.getServerResolver();

        assertEquals("nibbler-chix", resolver.resolveToServerName("FOO IX", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-chix", resolver.resolveToServerName("FOO GY IX", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("FOO FP", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-baml", resolver.resolveToServerName("FOO FP", RemoteOrderType.MKT_CLOSE, "CHAD", null));
        assertEquals("nibbler-xetra", resolver.resolveToServerName("FOO GY", RemoteOrderType.MKT_CLOSE, "CHAD", null));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("UNA NA", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-euronext", resolver.resolveToServerName("FP FP", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-baml", resolver.resolveToServerName("SAN SQ", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-baml", resolver.resolveToServerName("SAN SQ", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-baml", resolver.resolveToServerName("FOO GA", RemoteOrderType.MKT_CLOSE, "CHAD", null));
    }

    @Test
    public void test_prod_futures_resolver() throws Exception {

        Environment environment = new Environment(new Config(Paths.get("etc/prod-futures.properties")).getRoot());
        Environment.RemoteOrderServerResolver resolver = environment.getServerResolver();

        assertEquals("nibbler-ice", resolver.resolveToServerName("ERZ4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ERZ4", RemoteOrderType.GTC, "CHAD", null));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ZZ4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ZZ4", RemoteOrderType.GTC, "CHAD", null));
        assertEquals("nibbler-ice", resolver.resolveToServerName("ZZ4-ZH5", RemoteOrderType.GTC, "CHAD", null));

        assertEquals("nibbler-eurex-1", resolver.resolveToServerName("FESBU4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-1", resolver.resolveToServerName("FSTXU4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTCU4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTCU4", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTCU4", RemoteOrderType.TAKER, "CHAD", null));

        assertEquals("nibbler-eurex-1", resolver.resolveToServerName("FXXPU4", RemoteOrderType.TAKER, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTBU4", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-eurex-2", resolver.resolveToServerName("F2MXU4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-2", resolver.resolveToServerName("FTDXU4", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-eurex-1", resolver.resolveToServerName("FDAXU4", RemoteOrderType.TAKER, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSMMU4", RemoteOrderType.TAKER, "CHAD", null));
        assertEquals("nibbler-eurex-1", resolver.resolveToServerName("FSMIU4", RemoteOrderType.TAKER, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSLIU4", RemoteOrderType.TAKER, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.MANUAL, "CHAD", null));

        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTBZ4-FSTBH5", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.HAWK, "CHAD", null));
        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.TAKER, "CHAD", null));

        assertEquals("nibbler-eurex-gtc", resolver.resolveToServerName("FSTBZ4", RemoteOrderType.GTC, "CHAD", null));
        assertEquals("nibbler-eurex-gtc", resolver.resolveToServerName("FSTBU4-FSTBZ4", RemoteOrderType.GTC, "CHAD", null));

        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FEXDZ4", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-gtc", resolver.resolveToServerName("FEXDZ4", RemoteOrderType.GTC, "CHAD", null));

        assertEquals("nibbler-eurex-3", resolver.resolveToServerName("FEXDZ4-FEXDZ0", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("nibbler-eurex-gtc", resolver.resolveToServerName("FEXDZ4-FEXDZ0", RemoteOrderType.GTC, "CHAD", null));
    }

    @Test
    public void test_tag_resolver() throws IOException, ConfigException {
        Environment environment = new Environment(new Config(Paths.get("etc/prod-equities.properties")).getRoot());
        Environment.RemoteOrderServerResolver resolver = environment.getServerResolver();
        assertEquals("nibbler-xetra", resolver.resolveToServerName("FOO GY", RemoteOrderType.MANUAL, "CHAD", null));
        assertEquals("xetra-spreader", resolver.resolveToServerName("FOO GY", RemoteOrderType.MANUAL, "CLICKNOUGHT", null));
    }
}


