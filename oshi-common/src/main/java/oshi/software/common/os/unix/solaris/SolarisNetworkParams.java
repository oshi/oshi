/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

/**
 * Abstract base for Solaris NetworkParams. The default-gateway lookups are shared; the {@code gethostname} read is
 * native and implemented by the JNA and FFM subclasses.
 */
@ThreadSafe
public abstract class SolarisNetworkParams extends AbstractNetworkParams {

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route get -inet default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route get -inet6 default"));
    }
}
