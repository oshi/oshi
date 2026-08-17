/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.driver.unix.NetstatRoute;

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

    @Override
    public List<NetworkParams.IPRoute> getRoutes() {
        // The verbose form is required: netstat -rn -f inet publishes no netmask at all, so the prefix length would
        // be permanently unknown. -rnv adds a Mask column and still runs unprivileged.
        return NetstatRoute.querySolarisRoutes("netstat -rnv -f inet", "netstat -rnv -f inet6",
                queryInterfaceIndexByName());
    }
}
