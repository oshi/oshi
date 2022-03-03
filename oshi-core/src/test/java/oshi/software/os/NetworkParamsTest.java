/*
 * MIT License
 *
 * Copyright (c) 2017-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

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
