/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.NetworkParams.IPRoute;

/**
 * Tests the Windows routing table interpretation, which lives in {@code oshi-common} so that it can be exercised off
 * Windows. The backends supply only the raw rows, which the stub below fabricates.
 */
class WindowsNetworkParamsTest {

    /** A params instance whose IP Helper API rows are supplied by the test. */
    private static final class StubParams extends WindowsNetworkParams {
        private final List<WindowsNetworkParams.RouteRow> rows;
        private final List<byte[]> dnsServers;

        StubParams(List<WindowsNetworkParams.RouteRow> rows) {
            this(rows, List.of());
        }

        StubParams(List<WindowsNetworkParams.RouteRow> rows, List<byte[]> dnsServers) {
            this.rows = rows;
            this.dnsServers = dnsServers;
        }

        @Override
        protected List<WindowsNetworkParams.RouteRow> queryRouteRows() {
            return this.rows;
        }

        @Override
        protected List<byte[]> queryDnsServerAddresses() {
            return this.dnsServers;
        }
    }

    private static WindowsNetworkParams.RouteRow row(byte[] destination, int prefixLength, byte[] nextHop,
            int interfaceIndex, long metric) {
        WindowsNetworkParams.RouteRow r = new WindowsNetworkParams.RouteRow();
        r.destination = destination;
        r.prefixLength = prefixLength;
        r.nextHop = nextHop;
        r.interfaceIndex = interfaceIndex;
        r.metric = metric;
        return r;
    }

    @Test
    void testAnUnspecifiedNextHopIsNotAGateway() {
        // Windows publishes no flags column, so an on-link route is recognized by its all-zero next hop
        List<IPRoute> routes = new StubParams(
                List.of(row(new byte[] { 10, 0, 0, 0 }, 24, new byte[] { 0, 0, 0, 0 }, 5, 256L))).getRoutes();
        assertThat(routes, hasSize(1));
        assertThat(routes.get(0).isGateway(), is(false));
        assertThat("A non-gateway route reports no gateway address", routes.get(0).getGateway(), is(new byte[0]));
        assertThat(routes.get(0).getMetric(), is(256L));
    }

    @Test
    void testARealNextHopIsAGateway() {
        List<IPRoute> routes = new StubParams(
                List.of(row(new byte[] { 0, 0, 0, 0 }, 0, new byte[] { -64, -88, 1, 1 }, 5, 25L))).getRoutes();
        assertThat(routes.get(0).isGateway(), is(true));
        assertThat(routes.get(0).getGateway(), is(new byte[] { -64, -88, 1, 1 }));
        assertThat(routes.get(0).getPrefixLength(), is(0));
        assertThat(routes.get(0).isHost(), is(false));
    }

    @Test
    void testHostRoutesAreRecognizedByPrefixWidthInBothFamilies() {
        byte[] v6 = new byte[16];
        v6[15] = 1;
        List<IPRoute> routes = new StubParams(
                List.of(row(new byte[] { 127, 0, 0, 1 }, 32, new byte[] { 0, 0, 0, 0 }, 1, 331L),
                        row(v6, 128, new byte[16], 1, 331L))).getRoutes();
        assertThat(routes, hasSize(2));
        assertThat("A /32 IPv4 route is a host route", routes.get(0).isHost(), is(true));
        assertThat("A /128 IPv6 route is a host route", routes.get(1).isHost(), is(true));
        assertThat(routes.get(1).getDestination().length, is(16));
    }

    @Test
    void testARowWithAnUnrecognizedFamilyIsDropped() {
        // The backends report an empty array for a SOCKADDR_INET family that is neither AF_INET nor AF_INET6.
        // Dropping the row keeps the promise that a destination's length identifies the family.
        List<WindowsNetworkParams.RouteRow> rows = new ArrayList<>();
        rows.add(row(new byte[0], 0, new byte[0], 5, 0L));
        rows.add(row(new byte[] { 10, 0, 0, 0 }, 8, new byte[0], 5, 0L));
        List<IPRoute> routes = new StubParams(rows).getRoutes();
        assertThat(routes, hasSize(1));
        assertThat(routes.get(0).getDestination().length, is(4));
    }

    @Test
    void testAnUnknownInterfaceIndexReportsAnEmptyName() {
        // Index 999999 will not be present in this machine's interface list
        List<IPRoute> routes = new StubParams(List.of(row(new byte[] { 10, 0, 0, 0 }, 8, new byte[0], 999999, 0L)))
                .getRoutes();
        assertThat(routes.get(0).getInterfaceName(), is(""));
        assertThat(routes.get(0).getInterfaceIndex(), is(999999));
    }

    @Test
    void testAnEmptyTableProducesAnEmptyList() {
        assertThat(new StubParams(Collections.emptyList()).getRoutes(), hasSize(0));
    }

    @Test
    void testDnsServersBothFamiliesDeduplicatedWithoutPlaceholders() {
        byte[] google6 = { 0x20, 0x01, 0x48, 0x60, 0x48, 0x60, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x88, (byte) 0x88 };
        byte[] placeholder = { (byte) 0xfe, (byte) 0xc0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 1 };
        byte[] router = { -64, -88, 1, 1 };
        // Two adapters that are up: one with an IPv4 and an IPv6 server, one repeating the IPv4 server and
        // reporting the placeholder Windows lists when no IPv6 server is configured
        String[] servers = new StubParams(List.of(), List.of(router, google6, router.clone(), placeholder, new byte[3]))
                .getDnsServers();
        assertThat(servers, is(new String[] { "192.168.1.1", "2001:4860:4860::8888" }));
    }

    @Test
    void testUnconfiguredIpv6DnsPlaceholders() {
        byte[] address = { (byte) 0xfe, (byte) 0xc0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 3 };
        assertThat(WindowsNetworkParams.isUnconfiguredIpv6Dns(address), is(true));
        address[15] = 4;
        assertThat(WindowsNetworkParams.isUnconfiguredIpv6Dns(address), is(false));
        address[15] = 1;
        address[7] = 0;
        assertThat(WindowsNetworkParams.isUnconfiguredIpv6Dns(address), is(false));
        assertThat(WindowsNetworkParams.isUnconfiguredIpv6Dns(new byte[] { -2, -64, 0, 1 }), is(false));
    }
}
