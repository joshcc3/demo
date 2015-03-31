package com.drwtrading.london.reddal;

import com.drw.xetra.ebs.mds.XetraStream;
import com.drw.xetra.ebs.mds.XetraTypes;
import com.drwtrading.eeif.md.xetra.XetraStreamPair;
import com.drwtrading.london.config.Config;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
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


    public static enum Exchange {
        EUREX,
        XETRA,
        REMOTE
    }

    public static final String MARKET_DATA = "marketData";
    public static final String WORKING_ORDERS = "workingOrders";
    public static final String REMOTE_COMMANDS = "remoteCommands";
    public static final String METADATA = "metadata";

    public boolean opxlDeskPositionEnabled() {
        return config.getBooleanOrDefault("opxl.deskposition.enabled", false);
    }

    public Collection<String> opxlDeskPositionKeys() {
        return config.getListOrEmpty("opxl.deskposition.keys");
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
        return config.getListOrEmpty("opxl.laddertext.keys");
    }

    public Exchange getMarketDataExchange(final String mds) {
        return Exchange.valueOf(config.getOrDefault(MARKET_DATA + "." + mds + ".exchange", "REMOTE"));
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

    public File getXetraReferenceDataFile(String marketDataName) {
        File file = getFile(MARKET_DATA + "." + marketDataName + ".referenceDataFile");
        file.getParentFile().mkdirs();
        return file;
    }

    private File getFile(String key) {
        return new File(config.get(key).replace("{date}", new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
    }

    public File getLogDirectory(String configName) throws IOException {
        File baseLogDir = new File(config.get("logDir"));
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File logDir = new File(baseLogDir, today + "-" + configName).getCanonicalFile();
        logDir.mkdirs();
        return logDir;
    }


    public Collection<XetraStreamPair> getXetraReferenceDataStreams(String marketDataName) {
        marketDataName = MARKET_DATA + "." + marketDataName;
        List<String> xetraRefDataStreams = config.getList(marketDataName + ".refDataStreams");
        List<XetraStreamPair> xetraStreamPairs = new ArrayList<XetraStreamPair>();
        for (String xetraRefDataStream : xetraRefDataStreams) {
            String prefix = marketDataName + ".refData." + xetraRefDataStream;
            XetraStreamPair streamPair = new XetraStreamPair(
                    new XetraStream(XetraTypes.StreamType.ReferenceData, XetraStream.XetraStreamServiceType.A, config.get(prefix + ".addressA"), config.getInt(prefix + ".port"), null, null),
                    new XetraStream(XetraTypes.StreamType.ReferenceData, XetraStream.XetraStreamServiceType.B, config.get(prefix + ".addressB"), config.getInt(prefix + ".port"), null, null)
            );
            xetraStreamPairs.add(streamPair);
        }
        return xetraStreamPairs;
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
                getServerResolver(),
                config.getDouble("trading.randomReloadFraction"));
    }

    public HostAndNic getHostAndNic(String prefix, String server) throws SocketException {
        prefix = prefix + "." + server;
        final String address = config.get(prefix + ".address");
        final String nic = config.getOrDefault(prefix + ".nic", "0.0.0.0");
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])), NetworkInterfaces.find(nic));
    }

    public HostAndNic getHostAndNic(String prefix) throws SocketException {
        final String address = config.get(prefix + ".address");
        final String nic = config.getOrDefault(prefix + ".nic", "0.0.0.0");
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])), NetworkInterfaces.find(nic));
    }

    public Collection<String> getMarkets(final String mds) {
        return config.getList(MARKET_DATA + "." + mds + ".markets");
    }

    public Set<String> getXetraMarkets(String mds) {
        return new HashSet<String>(config.getList(MARKET_DATA + "." + mds + ".markets"));
    }


    public String getMarketDataInterface(final String mds) throws SocketException {
        return NetworkInterfaces.find(config.get(MARKET_DATA + "." + mds + ".interface"));
    }


    /**
     * Remote commands server are matched in order.
     * If the symbol matches the regex AND (no order types are specified OR the order is among the types specified) THEN it matches
     *
     * @return
     */
    public RemoteOrderServerResolver getServerResolver() {

        System.out.println("Loading server resolver:");
        final LinkedHashMap<String, RemoteOrderMatcher> matchers = new LinkedHashMap<String, RemoteOrderMatcher>();

        for (String remoteServer : getList(REMOTE_COMMANDS)) {
            String symbolRegex = config.getOrDefault(REMOTE_COMMANDS + "." + remoteServer + ".symbolRegex", ".*");
            Pattern pattern = Pattern.compile(symbolRegex);
            Set<String> orderTypes = ImmutableSet.copyOf(config.getListOrDefault(REMOTE_COMMANDS + "." + remoteServer + ".orderTypes", ImmutableList.of("*")));
            System.out.println("\t" + remoteServer + ": regex '" + symbolRegex + "', order types: " + orderTypes);
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
                return null;
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
