/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        Map<String, Integer> ifIndexByName = queryInterfaceIndexByName();
        List<NetworkParams.IPRoute> routes = new ArrayList<>(
                NetstatRoute.querySolarisRoutes("netstat -rnv -f inet", false, ifIndexByName));
        routes.addAll(NetstatRoute.querySolarisRoutes("netstat -rnv -f inet6", true, ifIndexByName));
        return routes;
    }
}
