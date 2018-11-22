package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.ladders.LadderOptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.regex.Pattern;

public class Environment {

    public static final String EEIF_OE = "eeifoe";
    public static final String METADATA = "metadata";

    private final ConfigGroup config;

    Environment(final ConfigGroup config) {
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

    public String getStatsName() throws ConfigException {
        return config.getGroup("stats").getString("name");
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
        final double reloadFraction = tradingGroup.getDouble("randomReloadFraction");
        final String basketURL;
        if (tradingGroup.paramExists("basketUrl")) {
            basketURL = tradingGroup.getString("basketUrl");
        } else {
            basketURL = null;
        }
        return new LadderOptions(leftClickOrderTypes, rightClickOrderTypes, traders, reloadFraction, basketURL);
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

        if (!config.groupExists(prefix) || !config.getGroup(prefix).groupExists(server)) {
            return null;
        }

        final ConfigGroup serverConfig = config.getGroup(prefix).getGroup(server);
        return getHostAndNic(serverConfig);
    }

    private HostAndNic getHostAndNic(final String prefix) throws SocketException, ConfigException {

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
}
