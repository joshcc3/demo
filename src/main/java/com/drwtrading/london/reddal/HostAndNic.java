package com.drwtrading.london.reddal;

import java.net.InetSocketAddress;

public class HostAndNic {

    public final InetSocketAddress host;
    public final String nic;

    HostAndNic(final InetSocketAddress host, final String nic) {
        this.host = host;
        this.nic = nic;
    }
}
