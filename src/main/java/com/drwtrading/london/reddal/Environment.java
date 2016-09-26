package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.fastui.html.CSSClass;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderType;
import com.drwtrading.london.reddal.ladders.LadderOptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Environment {

    public static final String WORKING_ORDERS = "workingOrders";
    public static final String REMOTE_COMMANDS = "remoteCommands";
    public static final String EEIF_OE = "eeifoe";
    public static final String METADATA = "metadata";

    private final ConfigGroup config;

    public Environment(final ConfigGroup config) {
        this.config = config;
    }

    public HostAndNic getMrPhilHostAndNic() throws SocketException, ConfigException {
        return getHostAndNic("mr-phil");
    }

    public Path getSettingsFile() throws IOException, ConfigException {

        final Path settingsFile = config.getGroup("settings").getPath("file");

        Files.createDirectories(settingsFile.getParent());
        if (Files.notExists(settingsFile)) {
            Files.createFile(settingsFile);
        }
        return settingsFile;
    }

    public int getCommandsPort() throws ConfigException {
        return config.getGroup("commands").getInt("port");
    }

    public static class HostAndNic {

        public final InetSocketAddress host;
        public final String nic;

        public HostAndNic(final InetSocketAddress host, final String nic) {
            this.host = host;
            this.nic = nic;
        }
    }

    public String getStatsName() throws ConfigException {
        return config.getGroup("stats").getString("name");
    }

    public String getStatsNns() throws ConfigException {
        return config.getGroup("stats").getString("nns");
    }

    public String getStatsInterface() throws SocketException, ConfigException {
        final String nic = config.getGroup("stats").getString("interface");
        return NetworkInterfaces.find(nic);
    }

    public int getWebPort() throws ConfigException {
        return config.getGroup("web").getInt("port");
    }

    public Collection<String> getList(final String prefix) throws ConfigException {
        if (config.paramExists(prefix)) {
            return config.getParam(prefix).getSet(Pattern.compile(","));
        } else {
            return Collections.emptyList();
        }
    }

    public LadderOptions ladderOptions() throws ConfigException {

        final ConfigGroup tradingGroup = config.getGroup("trading");
        final Collection<CSSClass> leftClickOrderTypes = getClickOrderTypes(tradingGroup, "orderTypesLeft");
        final Collection<CSSClass> rightClickOrderTypes = getClickOrderTypes(tradingGroup, "orderTypesRight");
        final Collection<String> traders = tradingGroup.getParam("traders").getSet(Pattern.compile(","));
        final Collection<String> fxTraders = tradingGroup.paramExists("fxTraders") ?
                tradingGroup.getParam("fxTraders").getSet(Pattern.compile(","))
                : Collections.emptyList();
        final String theoLaserLine = tradingGroup.getString("theoLaserLine");
        final double reloadFraction = tradingGroup.getDouble("randomReloadFraction");
        final String basketURL;
        if (tradingGroup.paramExists("basketUrl")) {
            basketURL = tradingGroup.getString("basketUrl");
        } else {
            basketURL = null;
        }
        return new LadderOptions(leftClickOrderTypes, rightClickOrderTypes, traders, fxTraders, theoLaserLine, getServerResolver(), reloadFraction,
                basketURL);
    }

    private static Collection<CSSClass> getClickOrderTypes(final ConfigGroup config, final String groupName) throws ConfigException {

        final EnumSet<CSSClass> result = EnumSet.noneOf(CSSClass.class);

        final Collection<String> clickOrderTypes = config.getParam(groupName).getSet(Pattern.compile(","));
        for (final String orderType : clickOrderTypes) {
            try {
                final CSSClass cssClass = CSSClass.valueOf(orderType);
                result.add(cssClass);
            } catch (final Exception ignore) {
                throw new IllegalArgumentException("No CSS Class has been assigned for given order type [" + orderType + "].");
            }
        }
        return result;
    }

    public HostAndNic getHostAndNic(final String prefix, final String server) throws SocketException, ConfigException {

        final ConfigGroup serverConfig = config.getGroup(prefix).getGroup(server);
        final String address = serverConfig.getString("address");
        final String nic;
        if (serverConfig.paramExists("nic")) {
            nic = serverConfig.getString("nic");
        } else {
            nic = "0.0.0.0";
        }
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])),
                NetworkInterfaces.find(nic));
    }

    public HostAndNic getHostAndNic(final String prefix) throws SocketException, ConfigException {

        final ConfigGroup serverConfig = config.getGroup(prefix);
        final String address = serverConfig.getString("address");
        final String nic;
        if (serverConfig.paramExists("nic")) {
            nic = serverConfig.getString("nic");
        } else {
            nic = "0.0.0.0";
        }
        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])),
                NetworkInterfaces.find(nic));
    }

    public String getEntity() throws ConfigException {
        if (config.paramExists("entity")) {
            return config.getString("entity");
        } else {
            return "eeif";
        }
    }

    /**
     * Remote commands server are matched in order.
     * If the symbol matches the regex AND (no order types are specified OR the order is among the types specified) THEN it matches
     */
    public RemoteOrderServerResolver getServerResolver() throws ConfigException {

        System.out.println("Loading server resolver:");
        final LinkedHashMap<String, RemoteOrderMatcher> matchers = new LinkedHashMap<>();

        final ConfigGroup remoteCommands = config.getGroup(REMOTE_COMMANDS);
        final String[] orderedServers = config.getString(REMOTE_COMMANDS).split(",");
        for (final String orderedServer : orderedServers) {
            final String remoteServer = orderedServer.trim();
            final ConfigGroup remoteServerCMDs = remoteCommands.getGroup(remoteServer);

            final String symbolRegex;
            if (remoteServerCMDs.paramExists("symbolRegex")) {
                symbolRegex = remoteServerCMDs.getString("symbolRegex");
            } else {
                symbolRegex = ".*";
            }

            final Pattern pattern = Pattern.compile(symbolRegex);

            final Collection<String> orderTypesRaw;
            if (remoteServerCMDs.paramExists("orderTypes")) {
                orderTypesRaw = remoteServerCMDs.getParam("orderTypes").getSet(Pattern.compile(","));
            } else {
                orderTypesRaw = null;
            }

            final Collection<String> tagsRaw;
            if (remoteServerCMDs.paramExists("tags")) {
                tagsRaw = remoteServerCMDs.getParam("tags").getSet(Pattern.compile(","));
            } else {
                tagsRaw = null;
            }

            System.out.println('\t' + remoteServer + ": regex '" + symbolRegex + "', order types: " + orderTypesRaw + ", tags: " + tagsRaw);
            matchers.put(remoteServer, new RemoteOrderMatcher(pattern, orderTypesRaw, tagsRaw));
        }
        return getRemoteOrderServerResolver(matchers);
    }

    public static RemoteOrderServerResolver getRemoteOrderServerResolver(final LinkedHashMap<String, RemoteOrderMatcher> matchers) {
        return (symbol, orderType, tag) -> {
            for (Map.Entry<String, RemoteOrderMatcher> entry : matchers.entrySet()) {
                if (entry.getValue().matches(symbol, orderType, tag)) {
                    return entry.getKey();
                }
            }
            return null;
        };
    }

    public static interface RemoteOrderServerResolver {
        public String resolveToServerName(final String symbol, final String orderType, final String tag);

        default String resolveToServerName(final String symbol, final RemoteOrderType remoteOrderType, final String tag) {
            return resolveToServerName(symbol, remoteOrderType.name(), tag);
        }
    }

    public static class RemoteOrderMatcher {

        public final Pattern symbolPattern;
        public final Collection<String> orderTypes;
        private final Collection<String> tags;

        public RemoteOrderMatcher(final Pattern symbolPattern, final Collection<String> orderTypes, final Collection<String> tags) {
            this.symbolPattern = symbolPattern;
            this.orderTypes = orderTypes;
            this.tags = tags;
        }

        public boolean matches(final String symbol, final String orderType, final String tag) {
            return symbolPattern.matcher(symbol).find() && (orderTypes == null || orderTypes.contains(orderType) && (tags == null || tags.contains(tag)));
        }
    }

}
