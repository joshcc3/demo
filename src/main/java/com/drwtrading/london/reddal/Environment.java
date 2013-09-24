package com.drwtrading.london.reddal;

import com.drwtrading.london.config.Config;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.drwtrading.monitoring.stats.StatsPublisher;
import com.drwtrading.monitoring.transport.NullTransport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Environment {

    public static class HostAndNic {
        public final InetSocketAddress host;
        public final String nic;

        public HostAndNic(InetSocketAddress host, String nic) {
            this.host = host;
            this.nic = nic;
        }
    }

    private final Config config;

    public Environment(Config config) {
        this.config = config;
    }

    public String getStatsName() {
        return config.get("stats.name");
    }

    public String getStatsNns() {
        return config.get("stats.nns");
    }

    public String getStatsInterface() throws SocketException {
        return NetworkInterfaces.find(config.get("stats.interface"));
    }

    public int getWebPort() {
        return config.getInt("web.port");
    }

    public File getLogDirectory(String configName) throws IOException {
        File baseLogDir = new File(config.get("logDir"));
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File logDir = new File(baseLogDir, today + "-" + configName).getCanonicalFile();
        logDir.mkdirs();
        return logDir;
    }

    public List<String> marketDataServers() {
        return config.getListOrEmpty("mds");
    }


    public List<String> workingOrderServers() {
        return config.getListOrEmpty("workingorders");
    }


    public List<String> remoteCommandServers() {
        return config.getListOrEmpty("remotecommands");
    }


    public LadderOptions ladderOptions() {
        return new LadderOptions(
                config.getListOrEmpty("options.orderTypesLeft"),
                config.getListOrEmpty("options.orderTypesRight"),
                config.getListOrEmpty("options.traders"),
                getServerResolver());
    }


    public List<String> metadataServers() {
        return config.getListOrEmpty("metadata");
    }


    public HostAndNic getHostAndNic(String prefix) throws SocketException {
        final String address = config.get(prefix + ".address");
        final String nic = config.get(prefix + ".nic");
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])), NetworkInterfaces.find(nic));
    }

    public StatsPublisher getStatsPublisher() {
        return new StatsPublisher("", new NullTransport());
    }

    public static interface RemoteOrderServerResolver {
        public String resolveToServerName(String symbol, RemoteOrderType orderType);
    }

    public static class RemoteOrderMatcher {
        public final Pattern symbolPattern;
        public final Set<String> orderTypes;

        public RemoteOrderMatcher(Pattern symbolPattern, Set<String> orderTypes) {
            this.symbolPattern = symbolPattern;
            this.orderTypes = orderTypes;
        }

        public boolean matches(String symbol, RemoteOrderType remoteOrderType) {
            return symbolPattern.matcher(symbol).find() && (orderTypes == null || orderTypes.contains(remoteOrderType.toString()));
        }
    }

    public RemoteOrderServerResolver getServerResolver() {

        final LinkedHashMap<String, RemoteOrderMatcher> matchers = new LinkedHashMap<String, RemoteOrderMatcher>();

        for (String remoteServer : remoteCommandServers()) {
            Pattern pattern = Pattern.compile(config.getOrDefault(remoteServer + ".symbolRegex", ".*"));
            Set<String> orderTypes = ImmutableSet.copyOf(config.getListOrDefault(remoteServer + ".orderTypes", ImmutableList.of("*")));
            if (orderTypes.size() == 1 && orderTypes.contains("*")) {
                orderTypes = null;
            }
            matchers.put(remoteServer, new RemoteOrderMatcher(pattern, orderTypes));
        }

        return new RemoteOrderServerResolver() {
            @Override
            public String resolveToServerName(String symbol, RemoteOrderType orderType) {
                for (Map.Entry<String, RemoteOrderMatcher> entry : matchers.entrySet()) {
                    if (entry.getValue().matches(symbol, orderType)) {
                        return entry.getKey();
                    }
                }
                throw new IllegalArgumentException("No matching remote order server for " + symbol + " " + orderType);
            }
        };

    }

}
