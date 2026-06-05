/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.unix.solaris.SolarisLibcFunctions;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

@ThreadSafe
final class SolarisNetworkParamsFFM extends AbstractNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisNetworkParamsFFM.class);

    private static final int HOST_NAME_MAX = 255;

    @Override
    public String getHostName() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(HOST_NAME_MAX + 1);
            int rc = SolarisLibcFunctions.gethostname(buf, HOST_NAME_MAX + 1);
            if (rc == 0) {
                return buf.getString(0);
            }
        } catch (Throwable t) {
            LOG.warn("gethostname failed; falling back to InetAddress", t);
        }
        return super.getHostName();
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route get -inet default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route get -inet6 default"));
    }
}
