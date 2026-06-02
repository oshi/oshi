/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

public abstract class FreeBsdNetworkParams extends AbstractNetworkParams {

    @Override
    public String getDomainName() {
        return queryDomainName();
    }

    @Override
    public String getHostName() {
        String hn = queryHostName();
        return hn != null ? hn : super.getHostName();
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -4 get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -6 get default"));
    }

    /**
     * Resolves the domain name via the subclass's getaddrinfo binding. Subclasses return an empty string on failure
     * (matching the prior JNA behavior).
     */
    protected abstract String queryDomainName();

    /**
     * Returns the hostname via the subclass's gethostname binding, or {@code null} to fall back to the InetAddress
     * lookup inherited from {@link AbstractNetworkParams#getHostName()}.
     */
    protected abstract String queryHostName();
}
