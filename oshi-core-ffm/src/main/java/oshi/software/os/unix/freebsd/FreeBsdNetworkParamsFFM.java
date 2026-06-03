/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.unix.freebsd.FreeBsdLibcFunctions.ADDRINFO_LAYOUT;
import static oshi.ffm.unix.freebsd.FreeBsdLibcFunctions.AI_CANONNAME;
import static oshi.ffm.unix.freebsd.FreeBsdLibcFunctions.HOST_NAME_MAX;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.unix.freebsd.FreeBsdLibcFunctions;
import oshi.software.common.os.unix.freebsd.FreeBsdNetworkParams;

/**
 * FFM-backed FreeBSD network params. Command-line gateway lookup and the {@code getHostName} fallback live on
 * {@link FreeBsdNetworkParams}; only the libc bindings ({@code getaddrinfo}, {@code gethostname}) are FFM-specific.
 */
@ThreadSafe
public class FreeBsdNetworkParamsFFM extends FreeBsdNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdNetworkParamsFFM.class);

    @Override
    protected String queryDomainName() {
        String hostname = getHostName();
        if (hostname == null || hostname.isEmpty()) {
            return "";
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment hint = arena.allocate(ADDRINFO_LAYOUT);
            hint.set(JAVA_INT, 0, AI_CANONNAME);
            MemorySegment node = arena.allocateFrom(hostname);
            MemorySegment resPtr = arena.allocate(ADDRESS);
            int rc = FreeBsdLibcFunctions.getaddrinfo(node, MemorySegment.NULL, hint, resPtr);
            if (rc != 0) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed getaddrinfo(): {}", FreeBsdLibcFunctions.gaiStrerror(rc, arena));
                }
                return "";
            }
            MemorySegment res = resPtr.get(ADDRESS, 0);
            try {
                String canonname = FreeBsdLibcFunctions.addrinfoCanonname(res, arena);
                return canonname == null ? "" : canonname.trim();
            } finally {
                FreeBsdLibcFunctions.freeaddrinfo(res);
            }
        }, LOG, Level.WARN, "Failed to query domain name", "");
    }

    @Override
    protected String queryHostName() {
        return callInArenaOrDefault(arena -> {
            MemorySegment buf = arena.allocate(HOST_NAME_MAX + 1);
            if (0 != FreeBsdLibcFunctions.gethostname(buf, HOST_NAME_MAX + 1L)) {
                return null;
            }
            return buf.getString(0);
        }, LOG, Level.WARN, "Failed to get hostname", null);
    }
}
