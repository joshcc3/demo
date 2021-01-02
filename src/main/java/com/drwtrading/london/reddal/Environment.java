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

    public static HostAndNic getHostAndNic(final ConfigGroup serverConfig) throws SocketException, ConfigException {

        final String nic = "0.0.0.0";
        final String address = serverConfig.getString("address");

        return new HostAndNic(new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])),
                NetworkInterfaces.find(nic));
    }
}
