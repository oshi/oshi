/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import org.jspecify.annotations.Nullable;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.common.os.unix.openbsd.OpenBsdNetworkParams;

/**
 * JNA-backed OpenBSD network params. The gateway lookup and the {@code getHostName} fallback live on
 * {@link OpenBsdNetworkParams}; only the {@code gethostname} binding is JNA-specific.
 */
@ThreadSafe
public class OpenBsdNetworkParamsJNA extends OpenBsdNetworkParams {

    private static final OpenBsdLibc LIBC = OpenBsdLibc.INSTANCE;

    @Override
    protected @Nullable String queryHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return null;
        }
        return Native.toString(hostnameBuffer);
    }
}
