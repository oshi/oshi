/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.windows;

import static java.lang.foreign.MemoryLayout.paddingLayout;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public class IPHlpAPIFFM extends WindowsForeignFunctions {

    public static final int AF_INET = 2;
    public static final int AF_INET6 = 23;
    public static final int TCP_TABLE_OWNER_PID_ALL = 5;
    public static final int UDP_TABLE_OWNER_PID = 1;

    private static final SymbolLookup IPHlpAPI = lib("IPHlpAPI");

    private static final MethodHandle GetExtendedTcpTable = downcall(IPHlpAPI, "GetExtendedTcpTable", JAVA_INT, ADDRESS,
            ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);

    public static final StructLayout MIB_TCPROW_OWNER_PID_LAYOUT = structLayout(JAVA_INT.withName("dwState"),
            JAVA_INT.withName("dwLocalAddr"), JAVA_INT.withName("dwLocalPort"), JAVA_INT.withName("dwRemoteAddr"),
            JAVA_INT.withName("dwRemotePort"), JAVA_INT.withName("dwOwningPid"));

    public static final StructLayout MIB_TCP6ROW_OWNER_PID_LAYOUT = structLayout(
            sequenceLayout(16, JAVA_BYTE).withName("ucLocalAddr"), JAVA_INT.withName("dwLocalScopeId"),
            JAVA_INT.withName("dwLocalPort"), sequenceLayout(16, JAVA_BYTE).withName("ucRemoteAddr"),
            JAVA_INT.withName("dwRemoteScopeId"), JAVA_INT.withName("dwRemotePort"), JAVA_INT.withName("dwState"),
            JAVA_INT.withName("dwOwningPid"));

    public static int GetExtendedTcpTable(MemorySegment tcpTable, MemorySegment tableSize, int bOrder, int ulAf,
            int tableClass, int reserved) throws Throwable {
        return (int) GetExtendedTcpTable.invokeExact(tcpTable, tableSize, bOrder, ulAf, tableClass, reserved);
    }

    private static final MethodHandle GetExtendedUdpTable = downcall(IPHlpAPI, "GetExtendedUdpTable", JAVA_INT, ADDRESS,
            ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);

    public static final StructLayout MIB_UDPROW_OWNER_PID_LAYOUT = structLayout(JAVA_INT.withName("dwLocalAddr"),
            JAVA_INT.withName("dwLocalPort"), JAVA_INT.withName("dwOwningPid"));

    public static final StructLayout MIB_UDP6ROW_OWNER_PID_LAYOUT = structLayout(
            sequenceLayout(16, JAVA_BYTE).withName("ucLocalAddr"), JAVA_INT.withName("dwLocalScopeId"),
            JAVA_INT.withName("dwLocalPort"), JAVA_INT.withName("dwOwningPid"));

    public static int GetExtendedUdpTable(MemorySegment udpTable, MemorySegment tableSize, int bOrder, int ulAf,
            int tableClass, int reserved) throws Throwable {
        return (int) GetExtendedUdpTable.invokeExact(udpTable, tableSize, bOrder, ulAf, tableClass, reserved);
    }

    private static final MethodHandle GetNetworkParams = downcall(IPHlpAPI, "GetNetworkParams", JAVA_INT, ADDRESS,
            ADDRESS);

    public static final StructLayout IP_ADDRESS_STRING_LAYOUT = structLayout(
            sequenceLayout(16, JAVA_BYTE).withName("String"));

    public static final StructLayout IP_ADDR_STRING_LAYOUT = structLayout(ADDRESS.withName("Next"),
            IP_ADDRESS_STRING_LAYOUT.withName("IpAddress"), IP_ADDRESS_STRING_LAYOUT.withName("IpMask"),
            JAVA_INT.withName("Context"), paddingLayout(4));

    public static final StructLayout FIXED_INFO_LAYOUT = structLayout(
            sequenceLayout(132, JAVA_BYTE).withName("HostName"), sequenceLayout(132, JAVA_BYTE).withName("DomainName"),
            ADDRESS.withName("CurrentDnsServer"), IP_ADDR_STRING_LAYOUT.withName("DnsServerList"),
            JAVA_INT.withName("NodeType"), sequenceLayout(260, JAVA_BYTE).withName("ScopeId"),
            JAVA_INT.withName("EnableRouting"), JAVA_INT.withName("EnableProxy"), JAVA_INT.withName("EnableDns"),
            paddingLayout(4));

    public static int GetNetworkParams(MemorySegment fixedInfo, MemorySegment bufferSize) throws Throwable {
        return (int) GetNetworkParams.invokeExact(fixedInfo, bufferSize);
    }

    private static final MethodHandle GetTcpStatisticsEx = downcall(IPHlpAPI, "GetTcpStatisticsEx", JAVA_INT, ADDRESS,
            JAVA_INT);

    public static final StructLayout MIB_TCPSTATS_LAYOUT = structLayout(JAVA_INT.withName("dwRtoAlgorithm"),
            JAVA_INT.withName("dwRtoMin"), JAVA_INT.withName("dwRtoMax"), JAVA_INT.withName("dwMaxConn"),
            JAVA_INT.withName("dwActiveOpens"), JAVA_INT.withName("dwPassiveOpens"),
            JAVA_INT.withName("dwAttemptFails"), JAVA_INT.withName("dwEstabResets"), JAVA_INT.withName("dwCurrEstab"),
            JAVA_INT.withName("dwInSegs"), JAVA_INT.withName("dwOutSegs"), JAVA_INT.withName("dwRetransSegs"),
            JAVA_INT.withName("dwInErrs"), JAVA_INT.withName("dwOutRsts"), JAVA_INT.withName("dwNumConns"));

    public static int GetTcpStatisticsEx(MemorySegment stats, int family) throws Throwable {
        return (int) GetTcpStatisticsEx.invokeExact(stats, family);
    }

    private static final MethodHandle GetUdpStatisticsEx = downcall(IPHlpAPI, "GetUdpStatisticsEx", JAVA_INT, ADDRESS,
            JAVA_INT);

    public static final StructLayout MIB_UDPSTATS_LAYOUT = structLayout(JAVA_INT.withName("dwInDatagrams"),
            JAVA_INT.withName("dwNoPorts"), JAVA_INT.withName("dwInErrors"), JAVA_INT.withName("dwOutDatagrams"),
            JAVA_INT.withName("dwNumAddrs"));

    public static int GetUdpStatisticsEx(MemorySegment stats, int family) throws Throwable {
        return (int) GetUdpStatisticsEx.invokeExact(stats, family);
    }

    // GetIfEntry2 for network interface statistics
    // ULONG GetAdaptersAddresses(ULONG Family, ULONG Flags, PVOID Reserved, PIP_ADAPTER_ADDRESSES AdapterAddresses,
    // PULONG SizePointer)
    private static final MethodHandle GetAdaptersAddresses = downcall(IPHlpAPI, "GetAdaptersAddresses", JAVA_INT,
            JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    public static final int GAA_FLAG_SKIP_UNICAST = 0x1;
    public static final int GAA_FLAG_SKIP_ANYCAST = 0x2;
    public static final int GAA_FLAG_SKIP_MULTICAST = 0x4;
    public static final int GAA_FLAG_SKIP_FRIENDLY_NAME = 0x20;

    // IP_ADAPTER_ADDRESSES_LH on 64-bit Windows, naming only the fields read: the first union's Length and IfIndex,
    // Next, FirstDnsServerAddress and OperStatus. The padding spans AdapterName through the multicast list, DnsSuffix
    // through IfType, and Ipv6IfIndex through FirstDnsSuffix.
    public static final StructLayout IP_ADAPTER_ADDRESSES_LAYOUT = structLayout(JAVA_INT.withName("Length"),
            JAVA_INT.withName("IfIndex"), ADDRESS.withName("Next"), paddingLayout(32),
            ADDRESS.withName("FirstDnsServerAddress"), paddingLayout(48), JAVA_INT.withName("OperStatus"),
            paddingLayout(340));
    public static final long OFFSET_ADAPTER_NEXT = IP_ADAPTER_ADDRESSES_LAYOUT
            .byteOffset(PathElement.groupElement("Next"));
    public static final long OFFSET_ADAPTER_FIRST_DNS_SERVER = IP_ADAPTER_ADDRESSES_LAYOUT
            .byteOffset(PathElement.groupElement("FirstDnsServerAddress"));
    public static final long OFFSET_ADAPTER_OPER_STATUS = IP_ADAPTER_ADDRESSES_LAYOUT
            .byteOffset(PathElement.groupElement("OperStatus"));

    public static final StructLayout SOCKET_ADDRESS_LAYOUT = structLayout(ADDRESS.withName("lpSockaddr"),
            JAVA_INT.withName("iSockaddrLength"), paddingLayout(4));

    public static final StructLayout IP_ADAPTER_DNS_SERVER_ADDRESS_LAYOUT = structLayout(JAVA_INT.withName("Length"),
            JAVA_INT.withName("Reserved"), ADDRESS.withName("Next"), SOCKET_ADDRESS_LAYOUT.withName("Address"));
    public static final long OFFSET_DNS_SERVER_NEXT = IP_ADAPTER_DNS_SERVER_ADDRESS_LAYOUT
            .byteOffset(PathElement.groupElement("Next"));
    public static final long OFFSET_DNS_SERVER_SOCKADDR = IP_ADAPTER_DNS_SERVER_ADDRESS_LAYOUT
            .byteOffset(PathElement.groupElement("Address"), PathElement.groupElement("lpSockaddr"));

    // SOCKADDR_IN holds its address at offset 4, SOCKADDR_IN6 at offset 8; the larger is 28 bytes
    public static final long SOCKADDR_IN6_SIZE = 28;

    public static int GetAdaptersAddresses(int family, int flags, MemorySegment adapterAddresses,
            MemorySegment sizePointer) throws Throwable {
        return (int) GetAdaptersAddresses.invokeExact(family, flags, MemorySegment.NULL, adapterAddresses, sizePointer);
    }

    private static final MethodHandle GetIfEntry2 = downcall(IPHlpAPI, "GetIfEntry2", JAVA_INT, ADDRESS);

    // MIB_IF_ROW2 is 1352 bytes on 64-bit Windows. We define key field offsets.
    // Layout: InterfaceLuid(8) InterfaceIndex(4) InterfaceGuid(16) Alias(514) Description(514)
    // PhysicalAddressLength(4) PhysicalAddress(32) PermanentPhysicalAddress(32) Mtu(4) Type(4)
    // TunnelType(4) MediaType(4) PhysicalMediumType(4) AccessType(4) DirectionType(4)
    // InterfaceAndOperStatusFlags(1+3pad) OperStatus(4) AdminStatus(4) MediaConnectState(4)
    // NetworkGuid(16) ConnectionType(4+4pad) TransmitLinkSpeed(8) ReceiveLinkSpeed(8)
    // InOctets(8) InUcastPkts(8) InNUcastPkts(8) InDiscards(8) InErrors(8) InUnknownProtos(8)
    // InUcastOctets(8) InMulticastOctets(8) InBroadcastOctets(8) OutOctets(8) OutUcastPkts(8)
    // OutNUcastPkts(8) OutDiscards(8) OutErrors(8) OutUcastOctets(8) OutMulticastOctets(8)
    // OutBroadcastOctets(8) OutQLen(8) = 1352
    public static final int MIB_IF_ROW2_SIZE = 1352;
    // Offsets for fields we need (64-bit Windows):
    public static final long OFFSET_INTERFACE_INDEX = 8;
    public static final long OFFSET_ALIAS = 28; // WCHAR[IF_MAX_STRING_SIZE + 1] = WCHAR[257]
    public static final long OFFSET_TYPE = 1128; // IFTYPE Type
    public static final long OFFSET_PHYSICAL_MEDIUM_TYPE = 1140; // NDIS_PHYSICAL_MEDIUM
    public static final long OFFSET_FLAGS = 1152; // InterfaceAndOperStatusFlags (byte)
    public static final long OFFSET_OPER_STATUS = 1156; // IF_OPER_STATUS
    public static final long OFFSET_RECEIVE_LINK_SPEED = 1200; // ULONG64
    public static final long OFFSET_IN_OCTETS = 1208; // ULONG64
    public static final long OFFSET_IN_UCAST_PKTS = 1216; // ULONG64
    public static final long OFFSET_IN_NUCAST_PKTS = 1224; // ULONG64
    public static final long OFFSET_IN_DISCARDS = 1232; // ULONG64
    public static final long OFFSET_IN_ERRORS = 1240; // ULONG64
    public static final long OFFSET_OUT_OCTETS = 1280; // ULONG64
    public static final long OFFSET_OUT_UCAST_PKTS = 1288; // ULONG64
    public static final long OFFSET_OUT_NUCAST_PKTS = 1296; // ULONG64
    public static final long OFFSET_OUT_DISCARDS = 1304; // ULONG64
    public static final long OFFSET_OUT_ERRORS = 1312; // ULONG64

    public static int GetIfEntry2(MemorySegment pIfRow) throws Throwable {
        return (int) GetIfEntry2.invokeExact(pIfRow);
    }

    // GetIpForwardTable2 for the routing table. AF_UNSPEC returns both address families in one call.
    public static final short AF_UNSPEC = 0;

    private static final MethodHandle GetIpForwardTable2 = downcall(IPHlpAPI, "GetIpForwardTable2", JAVA_INT,
            JAVA_SHORT, ADDRESS);

    // FreeMibTable returns void, hence the null return layout, as for WTSFreeMemory.
    private static final MethodHandle FreeMibTable = downcall(IPHlpAPI, "FreeMibTable", null, ADDRESS);

    // A MIB_IPFORWARD_TABLE2 is a ULONG entry count followed by the row array.
    // MIB_IPFORWARD_ROW2 is 8-byte aligned because NET_LUID is a ULONG64, so the row array begins at offset 8
    // rather than immediately after the 4-byte NumEntries.
    public static final long OFFSET_IPFORWARD_TABLE2_NUM_ENTRIES = 0;
    public static final long OFFSET_IPFORWARD_TABLE2_TABLE = 8;

    // MIB_IPFORWARD_ROW2 is 104 bytes on 64-bit Windows.
    // Layout: InterfaceLuid(8) InterfaceIndex(4) DestinationPrefix(32) NextHop(28) SitePrefixLength(1+3pad)
    // ValidLifetime(4) PreferredLifetime(4) Metric(4) Protocol(4) Loopback(1) AutoconfigureAddress(1) Publish(1)
    // Immortal(1) Age(4) Origin(4) = 104
    //
    // DestinationPrefix is an IP_ADDRESS_PREFIX: a SOCKADDR_INET(28) followed by a UINT8 PrefixLength, padded to 32.
    // SOCKADDR_INET is a union of SOCKADDR_IN and SOCKADDR_IN6, 28 bytes with 4-byte alignment. Its IPv4 arm holds
    // sin_addr at offset 4, where the IPv6 arm holds sin6_flowinfo; the IPv6 address itself is at offset 8.
    // Unions are modelled by offset rather than MemoryLayout.unionLayout, which this codebase does not use.
    public static final int MIB_IPFORWARD_ROW2_SIZE = 104;
    public static final long OFFSET_ROUTE_INTERFACE_INDEX = 8;
    public static final long OFFSET_ROUTE_DEST_FAMILY = 12;
    public static final long OFFSET_ROUTE_DEST_IPV4 = 16;
    public static final long OFFSET_ROUTE_DEST_IPV6 = 20;
    public static final long OFFSET_ROUTE_PREFIX_LENGTH = 40;
    public static final long OFFSET_ROUTE_NEXTHOP_FAMILY = 44;
    public static final long OFFSET_ROUTE_NEXTHOP_IPV4 = 48;
    public static final long OFFSET_ROUTE_NEXTHOP_IPV6 = 52;
    public static final long OFFSET_ROUTE_METRIC = 84;

    public static int GetIpForwardTable2(short family, MemorySegment pTable) throws Throwable {
        return (int) GetIpForwardTable2.invokeExact(family, pTable);
    }

    public static void FreeMibTable(MemorySegment memory) throws Throwable {
        // A void invokeExact must be a statement, not an expression, or it throws WrongMethodTypeException
        FreeMibTable.invokeExact(memory);
    }
}
