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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Stands in for a host whose own name does not resolve, which is not reproducible from a test otherwise.
     *
     * @param lookups counts the local host lookups the instance performs
     */
    private static AbstractNetworkParams createUnresolvableParams(AtomicInteger lookups) {
        return new AbstractNetworkParams() {
            @Override
            protected @Nullable InetAddress queryLocalHost() {
                lookups.incrementAndGet();
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
        AbstractNetworkParams params = createUnresolvableParams(new AtomicInteger());
        assertThat(params.getHostName(), is(emptyString()));
        assertThat(params.getDomainName(), is(emptyString()));
    }

    @Test
    void testBothNamesShareOneLocalHostLookup() {
        AtomicInteger lookups = new AtomicInteger();
        AbstractNetworkParams params = createUnresolvableParams(lookups);
        params.getHostName();
        params.getDomainName();
        // The JDK does not cache a failed lookup, so without memoization this would be 2.
        assertThat(lookups.get(), is(1));
    }

    @Test
    void testSearchGatewayFound() {
        List<String> lines = List.of("  route to: default", "  gateway: 192.168.1.1%en0", "  interface: en0");
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
