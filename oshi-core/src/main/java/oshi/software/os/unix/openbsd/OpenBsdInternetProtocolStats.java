/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStat;
import oshi.software.common.AbstractInternetProtocolStats;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class OpenBsdInternetProtocolStats extends AbstractInternetProtocolStats {

    @Override
    public TcpStats getTCPv4Stats() {
        return NetStat.queryTcpStats("netstat -s -p tcp");
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return NetStat.queryUdpStats("netstat -s -p udp");
    }
}
