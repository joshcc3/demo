package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.network.NetworkInterfaces;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.ladders.LadderOptions;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;

public class Environment {

    public static final String METADATA = "metadata";

    private final ConfigGroup config;

    Environment(final ConfigGroup config) {
        this.config = config;
    }

    public Collection<String> getList(final String prefix) throws ConfigException {
        if (config.paramExists(prefix)) {
            return config.getSet(prefix);
        } else {
            return Collections.emptyList();
        }
    }

    public LadderOptions ladderOptions() throws ConfigException {

        final ConfigGroup tradingGroup = config.getGroup("trading");

        final Collection<CSSClass> leftClickOrderTypes = tradingGroup.getEnumSet("orderTypesLeft", CSSClass.class);
        final Collection<CSSClass> rightClickOrderTypes = tradingGroup.getEnumSet("orderTypesRight", CSSClass.class);

        final Collection<User> traders = tradingGroup.getEnumSet("traders", User.class);

        final String basketURL;
        if (tradingGroup.paramExists("basketUrl")) {
            basketURL = tradingGroup.getString("basketUrl");
        } else {
            basketURL = null;
        }
        return new LadderOptions(leftClickOrderTypes, rightClickOrderTypes, traders, basketURL);
    }

    public HostAndNic getHostAndNic(final String prefix, final String server) throws SocketException, ConfigException {

        if (config.groupExists(prefix) && config.getGroup(prefix).groupExists(server)) {

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
        } else {
            return null;
        }
    }
}
