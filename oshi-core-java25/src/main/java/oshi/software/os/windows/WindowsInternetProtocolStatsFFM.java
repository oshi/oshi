/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.Win32Exception;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.platform.windows.IPHlpAPIUtilFFM;

import java.util.ArrayList;
import java.util.List;

import static oshi.ffm.windows.IPHlpAPIFFM.AF_INET;
import static oshi.ffm.windows.IPHlpAPIFFM.AF_INET6;
import static oshi.util.platform.windows.IPHlpAPIUtilFFM.queryTCPv4Connections;
import static oshi.util.platform.windows.IPHlpAPIUtilFFM.queryTCPv6Connections;
import static oshi.util.platform.windows.IPHlpAPIUtilFFM.queryUDPv4Connections;
import static oshi.util.platform.windows.IPHlpAPIUtilFFM.queryUDPv6Connections;

public class WindowsInternetProtocolStatsFFM extends AbstractInternetProtocolStats {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsInternetProtocolStatsFFM.class);

    public List<IPConnection> getConnections() {
        List<IPConnection> conns = new ArrayList<>();
        conns.addAll(queryTCPv4Connections());
        conns.addAll(queryTCPv6Connections());
        conns.addAll(queryUDPv4Connections());
        conns.addAll(queryUDPv6Connections());
        return conns;
    }

    @Override
    public TcpStats getTCPv4Stats() {
        try {
            return IPHlpAPIUtilFFM.getTcpStats(AF_INET);
        } catch (Win32Exception e) {
            LOG.error("Failed to read TCPv4 stats: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public TcpStats getTCPv6Stats() {
        try {
            return IPHlpAPIUtilFFM.getTcpStats(AF_INET6);
        } catch (Win32Exception e) {
            LOG.error("Failed to read TCPv6 stats: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public UdpStats getUDPv4Stats() {
        try {
            return IPHlpAPIUtilFFM.getUdpStats(AF_INET);
        } catch (Win32Exception e) {
            LOG.error("Failed to read UDPv4 stats: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public UdpStats getUDPv6Stats() {
        try {
            return IPHlpAPIUtilFFM.getUdpStats(AF_INET6);
        } catch (Win32Exception e) {
            LOG.error("Failed to read UDPv6 stats: {}", e.getMessage());
            return null;
        }
    }
}
