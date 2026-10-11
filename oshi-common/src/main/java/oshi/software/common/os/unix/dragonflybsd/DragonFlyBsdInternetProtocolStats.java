/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.driver.unix.NetStat;

/**
 * Internet Protocol Stats implementation for DragonFly BSD, read from {@code netstat -s}.
 * <p>
 * DragonFly's {@code net.inet.tcp.stats} and {@code net.inet.udp.stats} sysctls return one structure per CPU, in a
 * layout that differs from FreeBSD's {@code tcpstat} and {@code udpstat}, so the FreeBSD sysctl readers do not apply.
 */
@ThreadSafe
public class DragonFlyBsdInternetProtocolStats extends AbstractInternetProtocolStats {

    @Override
    public TcpStats getTCPv4Stats() {
        return NetStat.queryTcpStats("netstat -s -p tcp");
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return NetStat.queryUdpStats("netstat -s -p udp");
    }
}
