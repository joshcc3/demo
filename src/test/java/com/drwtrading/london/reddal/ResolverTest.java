package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.config.Config;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;
import com.google.common.collect.ImmutableSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class ResolverTest {

    @Test
    public void resolverTest() throws Exception {

        final LinkedHashMap<String, RemoteOrderMatcher> matchers = new LinkedHashMap<>();
        matchers.put("chix", new RemoteOrderMatcher(Pattern.compile(" IX$"), ImmutableSet.of("HAWK", "MANUAL"), null, null));
        matchers.put("xetra", new RemoteOrderMatcher(Pattern.compile(" GY$"), ImmutableSet.of("HAWK", "MANUAL", "MKT_CLOSE"), null, null));
        matchers.put("euronext", new RemoteOrderMatcher(Pattern.compile(" (FP|BB|PL|NA)$"), ImmutableSet.of("HAWK", "MANUAL"), null, null));
        matchers.put("eurex-fast", new RemoteOrderMatcher(Pattern.compile("^(FESB|FSTB|FXXP|FSTX)(H|M|U|Z)(1|2|3|4|5|6|7|8|9|0)$"),
                ImmutableSet.of("HAWK", "MANUAL"), null, null));
        matchers.put("baml", new RemoteOrderMatcher(Pattern.compile(".*"), ImmutableSet.of("HAWK", "MANUAL", "MKT_CLOSE"), null, null));

        final Environment.IRemoteOrderServerResolver resolver = Environment.getRemoteOrderServerResolver(matchers);

        Assert.assertEquals(resolver.resolveToServerName("FOO IX", RemoteOrderType.HAWK, "CHAD", null), "chix", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO GY IX", RemoteOrderType.HAWK, "CHAD", null), "chix", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO FP", RemoteOrderType.HAWK, "CHAD", null), "euronext", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO FP", RemoteOrderType.MANUAL, "CHAD", null), "euronext", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO GY", RemoteOrderType.MKT_CLOSE, "CHAD", null), "xetra", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("UNA NA", RemoteOrderType.MANUAL, "CHAD", null), "euronext", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FP FP", RemoteOrderType.MANUAL, "CHAD", null), "euronext", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FESBU4", RemoteOrderType.MANUAL, "CHAD", null), "eurex-fast", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO AV", RemoteOrderType.MKT_CLOSE, "CHAD", null), "baml", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO GA", RemoteOrderType.HAWK, "CHAD", null), "baml", "Resolved server.");
    }

    @Test
    public void localEquitiesResolverTest() throws IOException, ConfigException {
        final Environment environment = new Environment(new Config(Paths.get("etc/local.properties")).getRoot());
        environment.getServerResolver();
    }

    @Test
    public void prodEquitiesResolverTest() throws Exception {

        final Environment environment = new Environment(new Config(Paths.get("etc/prod-equities.properties")).getRoot());
        final Environment.IRemoteOrderServerResolver resolver = environment.getServerResolver();

        Assert.assertEquals(resolver.resolveToServerName("FOO IX", RemoteOrderType.HAWK, "CHAD", null), "nibbler-chix", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO GY IX", RemoteOrderType.HAWK, "CHAD", null), "nibbler-chix",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO FP", RemoteOrderType.HAWK, "CHAD", null), "nibbler-euronext",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO FP", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-euronext",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO FP", RemoteOrderType.MKT_CLOSE, "CHAD", null), "nibbler-euronext",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FOO GY", RemoteOrderType.MKT_CLOSE, "CHAD", null), "nibbler-xetra",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("UNA NA", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-euronext",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FP FP", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-euronext",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("SAN SQ", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-baml",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("SAN SQ", RemoteOrderType.HAWK, "CHAD", null), "nibbler-baml", "Resolved server.");
    }

    @Test
    public void prodFuturesResolverTest() throws Exception {

        final Environment environment = new Environment(new Config(Paths.get("etc/prod-futures.properties")).getRoot());
        final Environment.IRemoteOrderServerResolver resolver = environment.getServerResolver();

        Assert.assertEquals(resolver.resolveToServerName("ERZ4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-ice", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("ERZ4", RemoteOrderType.GTC, "CHAD", null), "nibbler-ice", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("ZZ4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-ice", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("ZZ4", RemoteOrderType.GTC, "CHAD", null), "nibbler-ice", "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("ZZ4-ZH5", RemoteOrderType.GTC, "CHAD", null), "nibbler-ice", "Resolved server.");

        Assert.assertEquals(resolver.resolveToServerName("FESBU4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-1",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTXU4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-1",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTCU4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTCU4", RemoteOrderType.HAWK, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTCU4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");

        Assert.assertEquals(resolver.resolveToServerName("FXXPU4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-1",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTBU4", RemoteOrderType.HAWK, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("F2MXU4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-2",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FDAXU4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-1",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSMMU4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSMIU4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-1",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSLIU4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTBZ4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");

        Assert.assertEquals(resolver.resolveToServerName("FSTBZ4-FSTBH5", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTBZ4", RemoteOrderType.HAWK, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTBZ4", RemoteOrderType.TAKER, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");

        Assert.assertEquals(resolver.resolveToServerName("FSTBZ4", RemoteOrderType.GTC, "CHAD", null), "nibbler-eurex-gtc",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FSTBU4-FSTBZ4", RemoteOrderType.GTC, "CHAD", null), "nibbler-eurex-gtc",
                "Resolved server.");

        Assert.assertEquals(resolver.resolveToServerName("FEXDZ4", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FEXDZ4", RemoteOrderType.GTC, "CHAD", null), "nibbler-eurex-gtc",
                "Resolved server.");

        Assert.assertEquals(resolver.resolveToServerName("FEXDZ4-FEXDZ0", RemoteOrderType.MANUAL, "CHAD", null), "nibbler-eurex-3",
                "Resolved server.");
        Assert.assertEquals(resolver.resolveToServerName("FEXDZ4-FEXDZ0", RemoteOrderType.GTC, "CHAD", null), "nibbler-eurex-gtc",
                "Resolved server.");
    }
}
