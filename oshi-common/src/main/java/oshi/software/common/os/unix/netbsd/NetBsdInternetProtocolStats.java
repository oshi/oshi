/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.driver.unix.NetStat;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class NetBsdInternetProtocolStats extends AbstractInternetProtocolStats {

    @Override
    public TcpStats getTCPv4Stats() {
        return NetStat.queryBsdTcpStats();
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return NetStat.queryUdpStats("netstat -s -p udp");
    }
}
