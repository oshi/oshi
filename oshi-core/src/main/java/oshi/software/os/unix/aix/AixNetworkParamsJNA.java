/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.AixLibc;
import oshi.software.common.os.unix.aix.AixNetworkParams;

/**
 * JNA-backed AIX NetworkParams. Calls {@code AixLibc.gethostname}; the {@code netstat} parsing for default gateways
 * lives on the {@link AixNetworkParams} base.
 */
@ThreadSafe
final class AixNetworkParamsJNA extends AixNetworkParams {

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_BUF_SIZE];
        if (0 != AixLibc.INSTANCE.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }
}
