/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.IPHlpAPI.FIXED_INFO;
import com.sun.jna.platform.win32.IPHlpAPI.IP_ADDR_STRING;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.windows.IPHlpAPI;
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

    private static final int COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED = 3;

    @Override
    public String getDomainName() {
        char[] buffer = new char[256];
        try (CloseableIntByReference bufferSize = new CloseableIntByReference(buffer.length)) {
            if (!Kernel32.INSTANCE.GetComputerNameEx(COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED, buffer, bufferSize)) {
                LOG.error("Failed to get dns domain name. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return "";
            }
        }
        return Native.toString(buffer);
    }

    @Override
    public String[] getDnsServers() {
        try (CloseableIntByReference bufferSize = new CloseableIntByReference()) {
            int ret = IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize);
            if (ret != WinError.ERROR_BUFFER_OVERFLOW) {
                LOG.error("Failed to get network parameters buffer size. Error code: {}", ret);
                return new String[0];
            }

            try (Memory buffer = new Memory(bufferSize.getValue())) {
                ret = IPHlpAPI.INSTANCE.GetNetworkParams(buffer, bufferSize);
                if (ret != 0) {
                    LOG.error("Failed to get network parameters. Error code: {}", ret);
                    return new String[0];
                }
                FIXED_INFO fixedInfo = new FIXED_INFO(buffer);

                List<String> list = new ArrayList<>();
                IP_ADDR_STRING dns = fixedInfo.DnsServerList;
                while (dns != null) {
                    // a char array of size 16.
                    // This array holds an IPv4 address in dotted decimal notation.
                    String addr = Native.toString(dns.IpAddress.String, StandardCharsets.US_ASCII);
                    int nullPos = addr.indexOf(0);
                    if (nullPos != -1) {
                        addr = addr.substring(0, nullPos);
                    }
                    list.add(addr);
                    dns = dns.Next;
                }
                return list.toArray(new String[0]);
            }
        }
    }

    @Override
    public String getHostName() {
        try {
            return Kernel32Util.getComputerName();
        } catch (Win32Exception e) {
            return super.getHostName();
        }
    }

    @Override
    protected List<RouteRow> queryRouteRows() {
        try (CloseablePointerByReference tableRef = new CloseablePointerByReference()) {
            int ret = IPHlpAPI.INSTANCE.GetIpForwardTable2((short) IPHlpAPI.AF_UNSPEC, tableRef);
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
        if (address.si_family == IPHlpAPI.AF_INET) {
            // sin_addr occupies the same four bytes the IPv6 arm uses for sin6_flowinfo, in network order
            return ParseUtil.parseIntToIP(address.ipv4AddrOrFlowInfo);
        } else if (address.si_family == IPHlpAPI.AF_INET6) {
            return Arrays.copyOf(address.ipv6Addr, 16);
        }
        return new byte[0];
    }
}
