/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Extends JNA's IP Helper API mapping with the routing table calls it does not provide.
 * <p>
 * This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public interface IPHlpAPI extends com.sun.jna.platform.win32.IPHlpAPI {

    IPHlpAPI INSTANCE = Native.load("IPHlpAPI", IPHlpAPI.class, W32APIOptions.DEFAULT_OPTIONS);

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
     * @param Family the inherited {@link #AF_INET}, {@link #AF_INET6}, or {@link #AF_UNSPEC} for both, narrowed to the
     *               {@code ADDRESS_FAMILY} this call declares
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

    /** {@code GetAdaptersAddresses} flag to leave out unicast addresses. */
    int GAA_FLAG_SKIP_UNICAST = 0x1;
    /** {@code GetAdaptersAddresses} flag to leave out anycast addresses. */
    int GAA_FLAG_SKIP_ANYCAST = 0x2;
    /** {@code GetAdaptersAddresses} flag to leave out multicast addresses. */
    int GAA_FLAG_SKIP_MULTICAST = 0x4;
    /** {@code GetAdaptersAddresses} flag to leave out the adapter's friendly name. */
    int GAA_FLAG_SKIP_FRIENDLY_NAME = 0x20;

    /**
     * The leading fields of {@code IP_ADAPTER_ADDRESSES_LH}, through {@code OperStatus}. The structure is read in place
     * from the buffer {@link #GetAdaptersAddresses} fills, so the fields after these need not be mapped.
     */
    @FieldOrder({ "Length", "IfIndex", "Next", "AdapterName", "FirstUnicastAddress", "FirstAnycastAddress",
            "FirstMulticastAddress", "FirstDnsServerAddress", "DnsSuffix", "Description", "FriendlyName",
            "PhysicalAddress", "PhysicalAddressLength", "Flags", "Mtu", "IfType", "OperStatus" })
    class IP_ADAPTER_ADDRESSES extends Structure {
        public int Length;
        public int IfIndex;
        public Pointer Next;
        public Pointer AdapterName;
        public Pointer FirstUnicastAddress;
        public Pointer FirstAnycastAddress;
        public Pointer FirstMulticastAddress;
        public Pointer FirstDnsServerAddress;
        public Pointer DnsSuffix;
        public Pointer Description;
        public Pointer FriendlyName;
        public byte[] PhysicalAddress = new byte[8];
        public int PhysicalAddressLength;
        public int Flags;
        public int Mtu;
        public int IfType;
        public int OperStatus;

        public IP_ADAPTER_ADDRESSES(Pointer p) {
            super(p);
            read();
        }
    }

    /** One DNS server address of an adapter, in a linked list. */
    @FieldOrder({ "Length", "Reserved", "Next", "lpSockaddr", "iSockaddrLength" })
    class IP_ADAPTER_DNS_SERVER_ADDRESS extends Structure {
        public int Length;
        public int Reserved;
        public Pointer Next;
        public Pointer lpSockaddr;
        public int iSockaddrLength;

        public IP_ADAPTER_DNS_SERVER_ADDRESS(Pointer p) {
            super(p);
            read();
        }
    }

    /**
     * Retrieves the addresses associated with the adapters on the local computer.
     *
     * @param Family           {@link #AF_INET}, {@link #AF_INET6}, or {@link #AF_UNSPEC} for both
     * @param Flags            the {@code GAA_FLAG_*} flags
     * @param Reserved         must be null
     * @param AdapterAddresses the buffer to fill with a linked list of {@link IP_ADAPTER_ADDRESSES}
     * @param SizePointer      the buffer size, updated to the size required on {@code ERROR_BUFFER_OVERFLOW}
     * @return {@code ERROR_SUCCESS} on success
     * @see <a href=
     *      "https://learn.microsoft.com/en-us/windows/win32/api/iphlpapi/nf-iphlpapi-getadaptersaddresses">GetAdaptersAddresses</a>
     */
    int GetAdaptersAddresses(int Family, int Flags, Pointer Reserved, Pointer AdapterAddresses,
            IntByReference SizePointer);
}
