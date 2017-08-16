package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.ladders.LadderOptions;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderType;

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
        final String theoLaserLine = tradingGroup.getString("theoLaserLine");
        final double reloadFraction = tradingGroup.getDouble("randomReloadFraction");
        final String basketURL;
        if (tradingGroup.paramExists("basketUrl")) {
            basketURL = tradingGroup.getString("basketUrl");
        } else {
            basketURL = null;
        }
        return new LadderOptions(leftClickOrderTypes, rightClickOrderTypes, traders, theoLaserLine, getServerResolver(), reloadFraction,
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
        return getHostAndNic(serverConfig);
    }

    public HostAndNic getHostAndNic(final String prefix) throws SocketException, ConfigException {

        final ConfigGroup serverConfig = config.getGroup(prefix);
        return getHostAndNic(serverConfig);
    }

    public static HostAndNic getHostAndNic(final ConfigGroup serverConfig) throws ConfigException, SocketException {

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

    /**
     * Remote commands server are matched in order.
     * If the symbol matches the regex AND (no order types are specified OR the order is among the types specified) THEN it matches
     */
    public IRemoteOrderServerResolver getServerResolver() throws ConfigException {

        System.out.println("Loading server resolver:");
        final LinkedHashMap<String, RemoteOrderMatcher> matchers = new LinkedHashMap<>();

        final ConfigGroup remoteCommands = config.getGroup(REMOTE_COMMANDS);
        final String[] orderedServers = config.getString(REMOTE_COMMANDS).trim().split(",");
        for (final String orderedServer : orderedServers) {
            final String remoteServer = orderedServer.trim();
            if (!remoteServer.isEmpty()) {

                final ConfigGroup remoteServerCMDs = remoteCommands.getEnabledGroup(remoteServer);

                final String symbolRegex;
                if (null != remoteServerCMDs && remoteServerCMDs.paramExists("symbolRegex")) {
                    symbolRegex = remoteServerCMDs.getString("symbolRegex");
                } else {
                    symbolRegex = ".*";
                }

                final Pattern pattern = Pattern.compile(symbolRegex);

                final Collection<String> orderTypesRaw;
                if (null != remoteServerCMDs && remoteServerCMDs.paramExists("orderTypes")) {
                    orderTypesRaw = remoteServerCMDs.getParam("orderTypes").getSet(Pattern.compile(","));
                } else {
                    orderTypesRaw = null;
                }

                final Collection<String> tagsRaw;
                if (null != remoteServerCMDs && remoteServerCMDs.paramExists("tags")) {
                    tagsRaw = remoteServerCMDs.getParam("tags").getSet(Pattern.compile(","));
                } else {
                    tagsRaw = null;
                }

                final Collection<String> micsRaw;
                if (null != remoteServerCMDs && remoteServerCMDs.paramExists("mics")) {
                    micsRaw = remoteServerCMDs.getParam("mics").getSet(Pattern.compile(","));
                } else {
                    micsRaw = null;
                }

                System.out.println(
                        '\t' + remoteServer + ": regex '" + symbolRegex + "', order types: " + orderTypesRaw + ", tags: " + tagsRaw);
                matchers.put(remoteServer, new RemoteOrderMatcher(pattern, orderTypesRaw, tagsRaw, micsRaw));
            }
        }
        return getRemoteOrderServerResolver(matchers);
    }

    public static IRemoteOrderServerResolver getRemoteOrderServerResolver(final LinkedHashMap<String, RemoteOrderMatcher> matchers) {
        return (symbol, orderType, tag, mic) -> {
            for (Map.Entry<String, RemoteOrderMatcher> entry : matchers.entrySet()) {
                if (entry.getValue().matches(symbol, orderType, tag, mic)) {
                    return entry.getKey();
                }
            }
            return null;
        };
    }

    public static interface IRemoteOrderServerResolver {

        public String resolveToServerName(final String symbol, final String orderType, final String tag, final String mic);

        default String resolveToServerName(final String symbol, final RemoteOrderType remoteOrderType, final String tag, final String mic) {
            return resolveToServerName(symbol, remoteOrderType.name(), tag, mic);
        }
    }

}
