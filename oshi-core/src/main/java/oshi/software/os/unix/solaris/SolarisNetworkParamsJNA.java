/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.os.unix.solaris.SolarisNetworkParams;

/**
 * JNA-backed Solaris NetworkParams.
 */
@ThreadSafe
final class SolarisNetworkParamsJNA extends SolarisNetworkParams {

    private static final SolarisLibc LIBC = SolarisLibc.INSTANCE;

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }
}
