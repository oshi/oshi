/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Extends JNA's IP Helper API mapping with the routing table calls it does not provide.
 * <p>
 * This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public interface IPHlpAPI extends com.sun.jna.platform.win32.IPHlpAPI {

    IPHlpAPI INSTANCE = Native.load("IPHlpAPI", IPHlpAPI.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Return both IPv4 and IPv6 routes from a single call. */
    short AF_UNSPEC = 0;

    /** {@code SOCKADDR_INET} address family for IPv4. */
    short AF_INET = 2;

    /** {@code SOCKADDR_INET} address family for IPv6. Note this is not the same value as on UNIX. */
    short AF_INET6 = 23;

    /**
     * A union of {@code SOCKADDR_IN}, {@code SOCKADDR_IN6} and a bare {@code ADDRESS_FAMILY}, mapped as its largest arm
     * so the layout is 28 bytes with 4-byte alignment.
     * <p>
     * The IPv4 arm's {@code sin_addr} occupies the four bytes at offset 4, which is where the IPv6 arm's
     * {@code sin6_flowinfo} sits, so one field serves both and {@link #si_family} selects between them.
     */
    @FieldOrder({ "si_family", "port", "ipv4AddrOrFlowInfo", "ipv6Addr", "scopeId" })
    class SOCKADDR_INET extends Structure {
        public short si_family;
        public short port;
        public int ipv4AddrOrFlowInfo;
        public byte[] ipv6Addr = new byte[16];
        public int scopeId;
    }

    /** An address paired with the number of leading bits that are significant. */
    @FieldOrder({ "Prefix", "PrefixLength" })
    class IP_ADDRESS_PREFIX extends Structure {
        public SOCKADDR_INET Prefix = new SOCKADDR_INET();
        public byte PrefixLength;
    }

    /**
     * A single routing table row.
     * <p>
     * {@code NET_LUID} is a union of a {@code ULONG64} and a bitfield over the same 64 bits. Since OSHI never reads it,
     * mapping it as a plain {@code long} is layout-identical and avoids a nested structure.
     */
    @FieldOrder({ "InterfaceLuid", "InterfaceIndex", "DestinationPrefix", "NextHop", "SitePrefixLength",
            "ValidLifetime", "PreferredLifetime", "Metric", "Protocol", "Loopback", "AutoconfigureAddress", "Publish",
            "Immortal", "Age", "Origin" })
    class MIB_IPFORWARD_ROW2 extends Structure {
        public long InterfaceLuid;
        public int InterfaceIndex;
        public IP_ADDRESS_PREFIX DestinationPrefix = new IP_ADDRESS_PREFIX();
        public SOCKADDR_INET NextHop = new SOCKADDR_INET();
        public byte SitePrefixLength;
        public int ValidLifetime;
        public int PreferredLifetime;
        public int Metric;
        public int Protocol;
        public byte Loopback;
        public byte AutoconfigureAddress;
        public byte Publish;
        public byte Immortal;
        public int Age;
        public int Origin;

        public MIB_IPFORWARD_ROW2() {
            super();
        }

        public MIB_IPFORWARD_ROW2(Pointer p) {
            super(p);
        }
    }

    /**
     * Retrieves the IP route entries on the local computer. The table is allocated by the system and must be released
     * with {@link #FreeMibTable(Pointer)}.
     *
     * @param Family {@link #AF_INET}, {@link #AF_INET6}, or {@link #AF_UNSPEC} for both
     * @param Table  receives a pointer to a {@code MIB_IPFORWARD_TABLE2}, whose {@code ULONG NumEntries} is followed by
     *               the row array. The rows are 8-byte aligned, so they begin at offset 8 rather than 4.
     * @return {@code NO_ERROR} on success
     * @see <a href=
     *      "https://learn.microsoft.com/en-us/windows/win32/api/netioapi/nf-netioapi-getipforwardtable2">GetIpForwardTable2</a>
     */
    int GetIpForwardTable2(short Family, PointerByReference Table);

    /**
     * Frees a table allocated by the IP Helper API.
     *
     * @param Memory the table to free
     * @see <a href=
     *      "https://learn.microsoft.com/en-us/windows/win32/api/netioapi/nf-netioapi-freemibtable">FreeMibTable</a>
     */
    void FreeMibTable(Pointer Memory);
}
