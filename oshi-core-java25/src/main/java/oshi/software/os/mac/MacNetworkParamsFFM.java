/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.mac.MacSystem.ADDRINFO;
import static oshi.ffm.mac.MacSystem.ADDRINFO_CANONNAME;
import static oshi.ffm.mac.MacSystem.AI_CANONNAME;
import static oshi.ffm.mac.MacSystem.AI_FLAGS;
import static oshi.ffm.mac.MacSystem.HOST_NAME_MAX;
import static oshi.ffm.mac.MacSystemFunctions.freeaddrinfo;
import static oshi.ffm.mac.MacSystemFunctions.gai_strerror;
import static oshi.ffm.mac.MacSystemFunctions.getaddrinfo;
import static oshi.ffm.mac.MacSystemFunctions.gethostname;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * MacNetworkParams implementation using FFM (no JNA dependency).
 */
@ThreadSafe
final class MacNetworkParamsFFM extends AbstractNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkParamsFFM.class);

    private static final String IPV6_ROUTE_HEADER = "Internet6:";
    private static final String DEFAULT_GATEWAY = "default";

    @Override
    public String getDomainName() {
        String hostname = "";
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
        try (Arena arena = Arena.ofConfined()) {
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
            try {
                // resPtr holds a pointer to the first addrinfo struct
                MemorySegment infoPtr = resPtr.get(ADDRESS, 0).reinterpret(ADDRINFO.byteSize(), arena, null);
                MemorySegment canonPtr = infoPtr.get(ADDRESS, ADDRINFO.byteOffset(ADDRINFO_CANONNAME));
                if (canonPtr.equals(MemorySegment.NULL)) {
                    return "";
                }
                return canonPtr.reinterpret(256, arena, null).getString(0).trim();
            } finally {
                freeaddrinfo(resPtr.get(ADDRESS, 0));
            }
        } catch (Throwable e) {
            LOG.debug("Failed getDomainName(): {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String getHostName() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(HOST_NAME_MAX + 1);
            if (gethostname(buf, HOST_NAME_MAX + 1) == 0) {
                return buf.getString(0);
            }
        } catch (Throwable e) {
            LOG.debug("Failed gethostname(): {}", e.getMessage());
        }
        return super.getHostName();
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        List<String> lines = ExecutingCommand.runNative("netstat -nr");
        boolean v6Table = false;
        for (String line : lines) {
            if (v6Table && line.startsWith(DEFAULT_GATEWAY)) {
                String[] fields = ParseUtil.whitespaces.split(line);
                if (fields.length > 2 && fields[2].contains("G")) {
                    return fields[1].split("%")[0];
                }
            } else if (line.startsWith(IPV6_ROUTE_HEADER)) {
                v6Table = true;
            }
        }
        return "";
    }
}
