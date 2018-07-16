/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; //NOSONAR
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.windows.IPHlpAPI;
import oshi.jna.platform.windows.IPHlpAPI.FIXED_INFO;
import oshi.jna.platform.windows.IPHlpAPI.IP_ADDR_STRING;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;
import oshi.util.platform.windows.WmiUtil.WmiProperty;

public class WindowsNetworkParams extends AbstractNetworkParams {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParams.class);

    private static final String IPV4_DEFAULT_DEST = "0.0.0.0/0"; // NOSONAR
    private static final String IPV6_DEFAULT_DEST = "::/0";

    private static final int COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED = 3;

    enum NetworkProperty implements WmiProperty {
        NEXTHOP(ValueType.STRING), //
        ROUTEMETRIC(ValueType.UINT16), //
        METRIC1(ValueType.UINT16);

        private ValueType type;

        NetworkProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }

        @Override
        public String getName() {
            return this.name();
        }
    }

    // MSFT_NetRoute
    private static final NetworkProperty[] NETROUTE_PROPERTIES = new NetworkProperty[] { NetworkProperty.NEXTHOP,
            NetworkProperty.ROUTEMETRIC };
    private static final String[] NETROUTE_STRINGS = WmiUtil.getPropertyStrings(NETROUTE_PROPERTIES);
    private static final ValueType[] NETROUTE_TYPES = WmiUtil.getPropertyTypes(NETROUTE_PROPERTIES);

    // IP4RouteTable
    private static final NetworkProperty[] IP4ROUTE_PROPERTIES = new NetworkProperty[] { NetworkProperty.NEXTHOP,
            NetworkProperty.METRIC1 };
    private static final String[] IP4ROUTE_STRINGS = WmiUtil.getPropertyStrings(IP4ROUTE_PROPERTIES);
    private static final ValueType[] IP4ROUTE_TYPES = WmiUtil.getPropertyTypes(IP4ROUTE_PROPERTIES);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        char[] buffer = new char[256];
        IntByReference bufferSize = new IntByReference(buffer.length);
        if (!Kernel32.INSTANCE.GetComputerNameEx(COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED, buffer, bufferSize)) {
            LOG.error("Failed to get dns domain name. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return "";
        }
        return new String(buffer).trim();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        // this may be done by iterating WMI instances
        // ROOT\CIMV2\Win32_NetworkAdapterConfiguration
        // then sort by IPConnectionMetric, but current JNA release does not
        // have string array support
        // for Variant (it's merged but not release yet).
        IntByReference bufferSize = new IntByReference();
        int ret = IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize);
        if (ret != WinError.ERROR_BUFFER_OVERFLOW) {
            LOG.error("Failed to get network parameters buffer size. Error code: {}", ret);
            return new String[0];
        }

        FIXED_INFO buffer = new FIXED_INFO(new Memory(bufferSize.getValue()));
        ret = IPHlpAPI.INSTANCE.GetNetworkParams(buffer, bufferSize);
        if (ret != 0) {
            LOG.error("Failed to get network parameters. Error code: {}", ret);
            return new String[0];
        }

        List<String> list = new ArrayList<>();
        IP_ADDR_STRING dns = buffer.DnsServerList;
        while (dns != null) {
            String addr = new String(dns.IpAddress.String);
            int nullPos = addr.indexOf(0);
            if (nullPos != -1) {
                addr = addr.substring(0, nullPos);
            }
            list.add(addr);
            dns = dns.Next == null ? null : new IP_ADDR_STRING(dns.Next);
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        // IPv6 info not available in WMI pre Windows 8
        if (WmiUtil.hasNamespace("StandardCimv2")) {
            return getNextHop(IPV4_DEFAULT_DEST);
        }
        // IPv4 info available in Win32_IP4RouteTable
        return getNextHopWin7(IPV4_DEFAULT_DEST.split("/")[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        // IPv6 info not available in WMI pre Windows 8
        if (WmiUtil.hasNamespace("StandardCimv2")) {
            return getNextHop(IPV6_DEFAULT_DEST);
        }
        return parseIpv6Route();
    }

    private String getNextHop(String dest) {
        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom("ROOT\\StandardCimv2", "MSFT_NetRoute",
                NETROUTE_STRINGS, "WHERE DestinationPrefix=\"" + dest + "\"", NETROUTE_TYPES);
        List<Object> metrics = vals.get(NetworkProperty.ROUTEMETRIC.name());
        if (vals.get(NetworkProperty.ROUTEMETRIC.name()).isEmpty()) {
            return "";
        }
        int index = 0;
        Long min = Long.MAX_VALUE;
        for (int i = 0; i < metrics.size(); i++) {
            Long metric = (Long) metrics.get(i);
            if (metric < min) {
                min = metric;
                index = i;
            }
        }
        return (String) vals.get(NetworkProperty.NEXTHOP.name()).get(index);
    }

    private String getNextHopWin7(String dest) {
        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom(null, "Win32_IP4RouteTable", IP4ROUTE_STRINGS,
                "WHERE Destination=\"" + dest + "\"", IP4ROUTE_TYPES);
        List<Object> metrics = vals.get(NetworkProperty.METRIC1.name());
        if (vals.get(NetworkProperty.METRIC1.name()).isEmpty()) {
            return "";
        }
        int index = 0;
        Long min = Long.MAX_VALUE;
        for (int i = 0; i < metrics.size(); i++) {
            Long metric = (Long) metrics.get(i);
            if (metric < min) {
                min = metric;
                index = i;
            }
        }
        return (String) vals.get(NetworkProperty.NEXTHOP.name()).get(index);
    }

    private String parseIpv6Route() {
        List<String> lines = ExecutingCommand.runNative("route print -6 ::/0");
        for (String line : lines) {
            String[] fields = ParseUtil.whitespaces.split(line.trim());
            if (fields.length > 3 && "::/0".equals(fields[2])) {
                return fields[3];
            }
        }
        return "";
    }

}
