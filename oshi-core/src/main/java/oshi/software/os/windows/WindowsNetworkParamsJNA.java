/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static com.sun.jna.platform.win32.IPHlpAPI.AF_INET;
import static com.sun.jna.platform.win32.IPHlpAPI.AF_INET6;
import static com.sun.jna.platform.win32.IPHlpAPI.AF_UNSPEC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinError;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.windows.IPHlpAPI;
import oshi.jna.platform.windows.IPHlpAPI.IP_ADAPTER_ADDRESSES;
import oshi.jna.platform.windows.IPHlpAPI.IP_ADAPTER_DNS_SERVER_ADDRESS;
import oshi.jna.platform.windows.IPHlpAPI.MIB_IPFORWARD_ROW2;
import oshi.jna.platform.windows.IPHlpAPI.SOCKADDR_INET;
import oshi.software.common.os.windows.WindowsNetworkParams;
import oshi.util.ParseUtil;

/**
 * WindowsNetworkParamsJNA class.
 */
@ThreadSafe
final class WindowsNetworkParamsJNA extends WindowsNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParamsJNA.class);

    private static final int COMPUTER_NAME_DNS_HOSTNAME = 1;
    private static final int COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED = 3;

    // Microsoft's recommended starting size, which avoids a second call on most systems
    private static final int ADAPTER_BUFFER_SIZE = 15_000;
    private static final int GAA_FLAGS = IPHlpAPI.GAA_FLAG_SKIP_UNICAST | IPHlpAPI.GAA_FLAG_SKIP_ANYCAST
            | IPHlpAPI.GAA_FLAG_SKIP_MULTICAST | IPHlpAPI.GAA_FLAG_SKIP_FRIENDLY_NAME;
    private static final int IF_OPER_STATUS_UP = 1;

    @Override
    public String getDomainName() {
        return computerNameEx(COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED);
    }

    private static String computerNameEx(int format) {
        char[] buffer = new char[256];
        try (CloseableIntByReference bufferSize = new CloseableIntByReference(buffer.length)) {
            if (!Kernel32.INSTANCE.GetComputerNameEx(format, buffer, bufferSize)) {
                LOG.error("Failed to get computer name format {}. Error code: {}", format,
                        Kernel32.INSTANCE.GetLastError());
                return "";
            }
        }
        return Native.toString(buffer);
    }

    @Override
    protected List<byte[]> queryDnsServerAddresses() {
        List<byte[]> addresses = new ArrayList<>();
        int size = ADAPTER_BUFFER_SIZE;
        // The adapter list can grow between the call that reports the size and the one that fills it
        for (int attempt = 0; attempt < 3; attempt++) {
            try (Memory buffer = new Memory(size);
                    CloseableIntByReference sizeRef = new CloseableIntByReference(size)) {
                int ret = IPHlpAPI.INSTANCE.GetAdaptersAddresses(IPHlpAPI.AF_UNSPEC, GAA_FLAGS, null, buffer, sizeRef);
                if (ret == WinError.ERROR_BUFFER_OVERFLOW) {
                    size = sizeRef.getValue();
                    continue;
                }
                if (ret == WinError.NO_ERROR) {
                    Pointer p = buffer;
                    while (p != null) {
                        IP_ADAPTER_ADDRESSES adapter = new IP_ADAPTER_ADDRESSES(p);
                        if (adapter.OperStatus == IF_OPER_STATUS_UP) {
                            addDnsServers(adapter.FirstDnsServerAddress, addresses);
                        }
                        p = adapter.Next;
                    }
                } else if (ret != WinError.ERROR_NO_DATA) {
                    LOG.error("Failed to get adapter addresses. Error code: {}", ret);
                }
                return addresses;
            }
        }
        return addresses;
    }

    private static void addDnsServers(@Nullable Pointer first, List<byte[]> addresses) {
        Pointer p = first;
        while (p != null) {
            IP_ADAPTER_DNS_SERVER_ADDRESS dns = new IP_ADAPTER_DNS_SERVER_ADDRESS(p);
            Pointer sockaddr = dns.lpSockaddr;
            if (sockaddr != null) {
                short family = sockaddr.getShort(0);
                if (family == IPHlpAPI.AF_INET) {
                    addresses.add(sockaddr.getByteArray(4, 4));
                } else if (family == IPHlpAPI.AF_INET6) {
                    addresses.add(sockaddr.getByteArray(8, 16));
                }
            }
            p = dns.Next;
        }
    }

    @Override
    public String getHostName() {
        // The DNS host name: GetComputerName returns the NetBIOS name, upper-cased and cut to 15 characters
        String name = computerNameEx(COMPUTER_NAME_DNS_HOSTNAME);
        return name.isEmpty() ? super.getHostName() : name;
    }

    @Override
    protected List<RouteRow> queryRouteRows() {
        try (CloseablePointerByReference tableRef = new CloseablePointerByReference()) {
            int ret = IPHlpAPI.INSTANCE.GetIpForwardTable2((short) AF_UNSPEC, tableRef);
            if (ret != WinError.NO_ERROR) {
                LOG.error("Failed to get the IP forward table. Error code: {}", ret);
                return new ArrayList<>();
            }
            Pointer table = tableRef.getValue();
            try {
                return readRows(table);
            } finally {
                IPHlpAPI.INSTANCE.FreeMibTable(table);
            }
        }
    }

    private static List<RouteRow> readRows(Pointer table) {
        int numEntries = table.getInt(0);
        if (numEntries <= 0) {
            return new ArrayList<>();
        }
        // MIB_IPFORWARD_ROW2 is 8-byte aligned because NET_LUID is a ULONG64, so the row array begins at offset 8
        // rather than immediately after the 4-byte NumEntries. Let JNA compute the row size rather than hardcoding
        // it, so this stays correct if the structure is ever remapped.
        int rowSize = new MIB_IPFORWARD_ROW2().size();
        List<RouteRow> rows = new ArrayList<>(numEntries);
        for (int i = 0; i < numEntries; i++) {
            MIB_IPFORWARD_ROW2 row = Structure.newInstance(MIB_IPFORWARD_ROW2.class,
                    table.share(8L + (long) i * rowSize));
            row.read();
            RouteRow out = new RouteRow();
            out.destination = addressBytes(row.DestinationPrefix.Prefix);
            out.prefixLength = row.DestinationPrefix.PrefixLength & 0xff;
            out.nextHop = addressBytes(row.NextHop);
            out.interfaceIndex = row.InterfaceIndex;
            out.metric = ParseUtil.unsignedIntToLong(row.Metric);
            rows.add(out);
        }
        return rows;
    }

    private static byte[] addressBytes(SOCKADDR_INET address) {
        if (address.si_family == AF_INET) {
            // sin_addr occupies the same four bytes the IPv6 arm uses for sin6_flowinfo, in network order
            return ParseUtil.parseIntToIP(address.ipv4AddrOrFlowInfo);
        } else if (address.si_family == AF_INET6) {
            return Arrays.copyOf(address.ipv6Addr, 16);
        }
        return new byte[0];
    }
}
