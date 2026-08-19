/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.HOST_NAME_MAX;

import java.lang.foreign.MemorySegment;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.bsd.RouteDumpFFM;
import oshi.ffm.platform.unix.PosixLibcFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.software.common.os.unix.openbsd.OpenBsdNetworkParams;
import oshi.util.LogLevel;

/**
 * FFM-backed OpenBSD network params. The gateway lookup and the {@code getHostName} fallback live on
 * {@link OpenBsdNetworkParams}; only the {@code gethostname} binding is FFM-specific.
 */
@ThreadSafe
public class OpenBsdNetworkParamsFFM extends OpenBsdNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdNetworkParamsFFM.class);

    @Override
    protected @Nullable String queryHostName() {
        return callInArenaOrDefault(arena -> {
            MemorySegment buf = arena.allocate(HOST_NAME_MAX + 1L);
            if (0 != PosixLibcFunctions.gethostname(buf, HOST_NAME_MAX + 1L)) {
                return null;
            }
            return buf.getString(0);
        }, LOG, LogLevel.WARN, "Failed to get hostname", null);
    }

    @Override
    protected byte[] queryRouteDump() {
        return RouteDumpFFM.queryRouteDump((state, name, namelen, oldp, oldlenp) -> OpenBsdLibcFunctions.sysctl(state,
                name, namelen, oldp, oldlenp, MemorySegment.NULL, 0));
    }
}
