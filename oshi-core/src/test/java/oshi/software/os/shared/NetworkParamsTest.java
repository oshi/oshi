/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.software.os.NetworkParams;

/**
 * Test network parameters
 */
class NetworkParamsTest {

    /**
     * Test network parameters
     */
    @Test
    void testNetworkParams() {
        SystemInfo si = new SystemInfo();
        NetworkParams params = si.getOperatingSystem().getNetworkParams();
        assertThat("Network parameters hostname is null.", params.getHostName(), is(notNullValue()));
        assertThat("Network parameters domain name is null.", params.getDomainName(), is(notNullValue()));
        assertThat("Network parameters DNS server is null.", params.getDnsServers(), is(notNullValue()));
        assertThat("Network parameters IPv4 default gateway is null.", params.getIpv4DefaultGateway(),
                is(notNullValue()));
        assertThat("Network parameters IPv6 default gateway is null.", params.getIpv6DefaultGateway(),
                is(notNullValue()));
    }
}
