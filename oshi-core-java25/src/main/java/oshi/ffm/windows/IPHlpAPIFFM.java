/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

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

    public static final StructLayout IP_ADDR_STRING_LAYOUT = structLayout(ValueLayout.ADDRESS.withName("Next"),
            IP_ADDRESS_STRING_LAYOUT.withName("IpAddress"), IP_ADDRESS_STRING_LAYOUT.withName("IpMask"),
            ValueLayout.JAVA_INT.withName("Context"));

    public static final StructLayout FIXED_INFO_LAYOUT = structLayout(
            sequenceLayout(132, ValueLayout.JAVA_BYTE).withName("HostName"),
            sequenceLayout(132, ValueLayout.JAVA_BYTE).withName("DomainName"),
            ValueLayout.ADDRESS.withName("CurrentDnsServer"), IP_ADDR_STRING_LAYOUT.withName("DnsServerList"),
            ValueLayout.JAVA_INT.withName("NodeType"), sequenceLayout(260, ValueLayout.JAVA_BYTE).withName("ScopeId"),
            ValueLayout.JAVA_INT.withName("EnableRouting"), ValueLayout.JAVA_INT.withName("EnableProxy"),
            ValueLayout.JAVA_INT.withName("EnableDns"));

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

    public static final StructLayout MIB_UDPSTATS_LAYOUT = structLayout(ValueLayout.JAVA_INT.withName("dwInDatagrams"),
            ValueLayout.JAVA_INT.withName("dwNoPorts"), ValueLayout.JAVA_INT.withName("dwInErrors"),
            ValueLayout.JAVA_INT.withName("dwOutDatagrams"), ValueLayout.JAVA_INT.withName("dwNumAddrs"));

    public static int GetUdpStatisticsEx(MemorySegment stats, int family) throws Throwable {
        return (int) GetUdpStatisticsEx.invokeExact(stats, family);
    }
}
