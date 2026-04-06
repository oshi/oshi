/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.linux.LinuxLibcFunctions;

/**
 * FFM-based Linux network parameters. Overrides {@code gethostname} and {@code getaddrinfo} calls to use FFM.
 */
final class LinuxNetworkParamsFFM extends LinuxNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkParamsFFM.class);

    private static final int HOST_NAME_MAX = 255;

    @Override
    public String getHostName() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(HOST_NAME_MAX + 1);
            if (0 == LinuxLibcFunctions.gethostname(buf, HOST_NAME_MAX + 1L)) {
                return buf.getString(0);
            }
        } catch (Throwable e) {
            LOG.warn("FFM gethostname failed: {}", e.toString());
        }
        return super.getHostName();
    }

    @Override
    public String getDomainName() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.warn("Unknown host exception when getting address of local host: {}", e.getMessage());
            return "";
        }
        try (Arena arena = Arena.ofConfined()) {
            // Build hints: ai_flags = AI_CANONNAME, rest zero
            MemorySegment hints = arena.allocate(LinuxLibcFunctions.ADDRINFO_LAYOUT);
            hints.set(ValueLayout.JAVA_INT, 0, LinuxLibcFunctions.AI_CANONNAME);

            // Output pointer-to-pointer: one ADDRESS slot
            MemorySegment resPP = arena.allocate(ValueLayout.ADDRESS);

            MemorySegment nodeSeg = arena.allocateFrom(hostname);
            int rc = LinuxLibcFunctions.getaddrinfo(nodeSeg, MemorySegment.NULL, hints, resPP);
            if (rc != 0) {
                LOG.error("Failed getaddrinfo(): {}", LinuxLibcFunctions.gaiStrerror(rc, arena));
                return "";
            }
            // resPP holds the pointer to the first addrinfo struct
            MemorySegment resPtr = resPP.get(ValueLayout.ADDRESS, 0);
            try {
                String canonname = LinuxLibcFunctions.addrinfoCanoname(resPtr, arena);
                return canonname == null ? hostname : canonname.trim();
            } finally {
                LinuxLibcFunctions.freeaddrinfo(resPtr);
            }
        } catch (Throwable e) {
            LOG.warn("FFM getaddrinfo failed: {}", e.toString());
        }
        return super.getDomainName();
    }
}
