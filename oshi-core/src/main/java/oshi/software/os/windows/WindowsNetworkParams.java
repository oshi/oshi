/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; //NOSONAR
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.IPHlpAPI.FIXED_INFO;
import com.sun.jna.platform.win32.IPHlpAPI.IP_ADDR_STRING;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.windows.VersionHelpers;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

public class WindowsNetworkParams extends AbstractNetworkParams {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParams.class);

    private static final String IPV4_DEFAULT_DEST = "0.0.0.0/0"; // NOSONAR
    private static final String IPV6_DEFAULT_DEST = "::/0";

    private static final int COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED = 3;

    enum NetRouteProperty {
        NEXTHOP, ROUTEMETRIC;
    }

    private static final String NETROUTE_BASE_CLASS = "MSFT_NetRoute";
    private static final WmiQuery<NetRouteProperty> NETROUTE_QUERY = new WmiQuery<>("ROOT\\StandardCimv2", null,
            NetRouteProperty.class);

    enum IP4RouteProperty {
        NEXTHOP, METRIC1;
    }

    private static final String IP4ROUTE_BASE_CLASS = "Win32_IP4RouteTable";
    private static final WmiQuery<IP4RouteProperty> IP4ROUTE_QUERY = new WmiQuery<>(null, IP4RouteProperty.class);

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
        IntByReference bufferSize = new IntByReference();
        int ret = IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize);
        if (ret != WinError.ERROR_BUFFER_OVERFLOW) {
            LOG.error("Failed to get network parameters buffer size. Error code: {}", ret);
            return new String[0];
        }

        Memory buffer = new Memory(bufferSize.getValue());
        ret = IPHlpAPI.INSTANCE.GetNetworkParams(buffer, bufferSize);
        if (ret != 0) {
            LOG.error("Failed to get network parameters. Error code: {}", ret);
            return new String[0];
        }
        FIXED_INFO fixedInfo = new FIXED_INFO(buffer);

        List<String> list = new ArrayList<>();
        IP_ADDR_STRING dns = fixedInfo.DnsServerList;
        while (dns != null) {
            String addr = new String(dns.IpAddress.String);
            int nullPos = addr.indexOf(0);
            if (nullPos != -1) {
                addr = addr.substring(0, nullPos);
            }
            list.add(addr);
            dns = dns.Next;
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        // IPv6 info not available in WMI pre Windows 8
        if (VersionHelpers.IsWindows8OrGreater()) {
            return getNextHop(IPV4_DEFAULT_DEST);
        } else if (VersionHelpers.IsWindowsVistaOrGreater()) {
            // IPv4 info available in Win32_IP4RouteTable
            return getNextHopWin7(IPV4_DEFAULT_DEST.split("/")[0]);
        }
        return parseIpv4Route();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        // IPv6 info not available in WMI pre Windows 8
        if (VersionHelpers.IsWindows8OrGreater()) {
            return getNextHop(IPV6_DEFAULT_DEST);
        }
        return parseIpv6Route();
    }

    private String getNextHop(String dest) {
        StringBuilder sb = new StringBuilder(NETROUTE_BASE_CLASS);
        sb.append(" WHERE DestinationPrefix=\"").append(dest).append('\"');
        NETROUTE_QUERY.setWmiClassName(sb.toString());
        WmiResult<NetRouteProperty> vals = WmiQueryHandler.getInstance().queryWMI(NETROUTE_QUERY);
        if (vals.getResultCount() < 1) {
            return "";
        }
        int index = 0;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < vals.getResultCount(); i++) {
            int metric = WmiUtil.getUint16(vals, NetRouteProperty.ROUTEMETRIC, i);
            if (metric < min) {
                min = metric;
                index = i;
            }
        }
        return WmiUtil.getString(vals, NetRouteProperty.NEXTHOP, index);
    }

    private String getNextHopWin7(String dest) {
        StringBuilder sb = new StringBuilder(IP4ROUTE_BASE_CLASS);
        sb.append(" WHERE Destination=\"").append(dest).append('\"');
        IP4ROUTE_QUERY.setWmiClassName(sb.toString());
        WmiResult<IP4RouteProperty> vals = WmiQueryHandler.getInstance().queryWMI(IP4ROUTE_QUERY);
        if (vals.getResultCount() < 1) {
            return "";
        }
        int index = 0;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < vals.getResultCount(); i++) {
            int metric = WmiUtil.getSint32(vals, IP4RouteProperty.METRIC1, i);
            if (metric < min) {
                min = metric;
                index = i;
            }
        }
        return WmiUtil.getString(vals, IP4RouteProperty.NEXTHOP, index);
    }

    private String parseIpv4Route() {
        List<String> lines = ExecutingCommand.runNative("route print -4 0.0.0.0");
        for (String line : lines) {
            String[] fields = ParseUtil.whitespaces.split(line.trim());
            if (fields.length > 2 && "0.0.0.0".equals(fields[0])) {
                return fields[2];
            }
        }
        return "";
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
