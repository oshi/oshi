/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.slf4j.event.Level.DEBUG;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.mac.MacSystem.ADDRINFO;
import static oshi.ffm.platform.mac.MacSystem.ADDRINFO_CANONNAME;
import static oshi.ffm.platform.mac.MacSystem.AI_CANONNAME;
import static oshi.ffm.platform.mac.MacSystem.AI_FLAGS;
import static oshi.ffm.platform.mac.MacSystem.HOST_NAME_MAX;
import static oshi.ffm.platform.mac.MacSystemFunctions.gai_strerror;
import static oshi.ffm.platform.mac.MacSystemFunctions.getaddrinfo;
import static oshi.ffm.platform.mac.MacSystemFunctions.gethostname;

import java.lang.foreign.MemorySegment;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.mac.MacSystemFunctions;
import oshi.software.common.os.mac.MacNetworkParams;

/**
 * MacNetworkParams implementation using FFM (no JNA dependency).
 */
@ThreadSafe
final class MacNetworkParamsFFM extends MacNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkParamsFFM.class);

    @Override
    public String getDomainName() {
        final String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            if (hostname == null || hostname.isEmpty()) {
                LOG.debug("Could not determine hostname");
                return "";
            }
        } catch (UnknownHostException e) {
            LOG.debug("Unknown host exception when getting address of local host: {}", e.getMessage());
            return "";
        }
        return callInArenaOrDefault(arena -> {
            // Allocate hint struct with AI_CANONNAME flag
            MemorySegment hint = arena.allocate(ADDRINFO);
            hint.set(JAVA_INT, ADDRINFO.byteOffset(AI_FLAGS), AI_CANONNAME);

            // Allocate pointer-to-pointer for result
            MemorySegment resPtr = arena.allocate(ADDRESS);

            MemorySegment nodeStr = arena.allocateFrom(hostname);
            int res = getaddrinfo(nodeStr, MemorySegment.NULL, hint, resPtr);
            if (res != 0) {
                LOG.debug("Failed getaddrinfo(): {}", gai_strerror(res));
                return "";
            }
            try (NativeHandle addrInfo = NativeHandle.of(resPtr.get(ADDRESS, 0), MacSystemFunctions::freeaddrinfo)) {
                // resPtr holds a pointer to the first addrinfo struct
                MemorySegment infoPtr = addrInfo.get().reinterpret(ADDRINFO.byteSize(), arena, null);
                MemorySegment canonPtr = infoPtr.get(ADDRESS, ADDRINFO.byteOffset(ADDRINFO_CANONNAME));
                if (canonPtr.equals(MemorySegment.NULL)) {
                    return "";
                }
                return canonPtr.reinterpret(256, arena, null).getString(0).trim();
            }
        }, LOG, DEBUG, "Failed getDomainName()", "");
    }

    @Override
    public String getHostName() {
        String hostname = callInArenaOrDefault(arena -> {
            MemorySegment buf = arena.allocate(HOST_NAME_MAX + 1L);
            if (gethostname(buf, HOST_NAME_MAX + 1L) == 0) {
                return buf.getString(0);
            }
            return null;
        }, LOG, DEBUG, "Failed gethostname()", null);
        return hostname == null ? super.getHostName() : hostname;
    }
}
