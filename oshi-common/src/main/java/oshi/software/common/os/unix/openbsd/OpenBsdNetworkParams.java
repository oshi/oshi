/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

/**
 * Abstract base for the OpenBSD NetworkParams. Holds the command-line gateway lookup and the host name fallback; the
 * JNA and FFM subclasses supply the {@code gethostname} binding.
 */
@ThreadSafe
public abstract class OpenBsdNetworkParams extends AbstractNetworkParams {

    @Override
    public String getHostName() {
        String hn = queryHostName();
        return hn != null ? hn : super.getHostName();
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get -inet6 default"));
    }

    /**
     * Reads the host name from libc, avoiding the name resolution that the {@link AbstractNetworkParams} fallback
     * performs.
     *
     * @return the native host name, or {@code null} to fall back to the InetAddress lookup
     */
    protected abstract @Nullable String queryHostName();
}
