/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class AbstractNetworkParamsTest {

    private static AbstractNetworkParams createParams() {
        return new AbstractNetworkParams() {
            @Override
            public String getIpv4DefaultGateway() {
                return "10.0.0.1";
            }

            @Override
            public String getIpv6DefaultGateway() {
                return "::1";
            }
        };
    }

    /** Stands in for a host whose own name does not resolve, which is not reproducible from a test otherwise. */
    private static AbstractNetworkParams createUnresolvableParams() {
        return new AbstractNetworkParams() {
            @Override
            protected @Nullable InetAddress queryLocalHost() {
                return null;
            }

            @Override
            public String getIpv4DefaultGateway() {
                return "";
            }

            @Override
            public String getIpv6DefaultGateway() {
                return "";
            }
        };
    }

    @Test
    void testHostAndDomainNotNull() {
        AbstractNetworkParams params = createParams();
        assertThat(params.getHostName(), is(notNullValue()));
        assertThat(params.getDomainName(), is(notNullValue()));
    }

    @Test
    void testUnresolvableLocalHostReportsTheSentinelRatherThanLocalhost() {
        AbstractNetworkParams params = createUnresolvableParams();
        assertThat(params.getHostName(), is(emptyString()));
        assertThat(params.getDomainName(), is(emptyString()));
    }

    @Test
    void testSearchGatewayFound() {
        List<String> lines = Arrays.asList("  route to: default", "  gateway: 192.168.1.1%en0", "  interface: en0");
        assertThat(AbstractNetworkParams.searchGateway(lines), is("192.168.1.1"));
    }

    @Test
    void testSearchGatewayNotFound() {
        assertThat(AbstractNetworkParams.searchGateway(Collections.emptyList()), is(""));
    }

    @Test
    void testToString() {
        String s = createParams().toString();
        assertThat(s, containsString("Host name:"));
        assertThat(s, containsString("10.0.0.1"));
        assertThat(s, containsString("::1"));
    }
}
