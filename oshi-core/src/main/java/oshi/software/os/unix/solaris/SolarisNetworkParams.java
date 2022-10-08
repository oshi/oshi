/*
 * Copyright 2017-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

/**
 * SolarisNetworkParams class.
 */
@ThreadSafe
final class SolarisNetworkParams extends AbstractNetworkParams {

    private static final SolarisLibc LIBC = SolarisLibc.INSTANCE;

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
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
