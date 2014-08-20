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
import java.util.*;
import java.util.regex.Pattern;

public class Environment {

    public static final String MARKET_DATA = "marketData";
    public static final String WORKING_ORDERS = "workingOrders";
    public static final String REMOTE_COMMANDS = "remoteCommands";
    public static final String METADATA = "metadata";

    public boolean opxlDeskPositionEnabled() {
        return config.getBooleanOrDefault("opxl.deskposition.enabled", false);
    }

    public boolean opxlLadderTextEnabled() {
        return config.getBooleanOrDefault("opxl.laddertext.enabled", false);
    }

    public HostAndNic getMrPhilHostAndNic() throws SocketException {
        return getHostAndNic("mr-phil");
    }

    public File getSettingsFile() throws IOException {
        File file = new File(config.get("settings.file"));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public boolean indyEnabled() {
        return config.getBooleanOrDefault("indy.enabled", false);
    }

    public HostAndNic getIndyHostAndNic() throws SocketException {
        return getHostAndNic("indy");
    }

    public int getCommandsPort() {
        return config.getInt("commands.port");
    }

    public Collection<String> getOpxlLadderTextKeys() {
        return config.getList("opxl.laddertext.keys");
    }

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


    public List<String> getList(final String prefix) {
        return ImmutableList.copyOf(config.getListOrEmpty(prefix));
    }

    public LadderOptions ladderOptions() {
        return new LadderOptions(
                getList("trading.orderTypesLeft"),
                getList("trading.orderTypesRight"),
                getList("trading.traders"),
                config.get("trading.theoLaserLine"),
                config.get("trading.tag"),
                getServerResolver(),
                config.getDouble("trading.randomReloadFraction"));
    }


    public HostAndNic getHostAndNic(String prefix, String server) throws SocketException {
        prefix = prefix + "." + server;
        final String address = config.get(prefix + ".address");
        final String nic = config.get(prefix + ".nic");
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])), NetworkInterfaces.find(nic));
    }
    public int getPort(String prefix, String server) {
        prefix = prefix + "." + server;
        return config.getInt(prefix + ".port");
    }
    public HostAndNic getHostAndNic(String prefix) throws SocketException {
        final String address = config.get(prefix + ".address");
        final String nic = config.get(prefix + ".nic");
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])), NetworkInterfaces.find(nic));
    }

    public StatsPublisher getStatsPublisher() {
        return new StatsPublisher("", new NullTransport());
    }

    /**
     * Remote commands server are matched in order.
     * If the symbol matches the regex AND (no order types are specified OR the order is among the types specified) THEN it matches
     * @return
     */
    public RemoteOrderServerResolver getServerResolver() {

        System.out.println("Loading server resolver:");
        final LinkedHashMap<String, RemoteOrderMatcher> matchers = new LinkedHashMap<String, RemoteOrderMatcher>();

        for (String remoteServer : getList(REMOTE_COMMANDS)) {
            String symbolRegex = config.getOrDefault(REMOTE_COMMANDS +"."+remoteServer + ".symbolRegex", ".*");
            Pattern pattern = Pattern.compile(symbolRegex);
            Set<String> orderTypes = ImmutableSet.copyOf(config.getListOrDefault(REMOTE_COMMANDS +"."+remoteServer + ".orderTypes", ImmutableList.of("*")));
            System.out.println("\t"+remoteServer+": regex '"+symbolRegex+"', order types: " + orderTypes);

            if (orderTypes.size() == 1 && orderTypes.contains("*")) {
                orderTypes = null;
            }
            matchers.put(remoteServer, new RemoteOrderMatcher(pattern, orderTypes));
        }

        return getRemoteOrderServerResolver(matchers);

    }

    public static RemoteOrderServerResolver getRemoteOrderServerResolver(final LinkedHashMap<String, RemoteOrderMatcher> matchers) {
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

}
