/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
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

        /** Return codes for successive calls; once used up, every call succeeds. */
        private final Deque<Integer> returnCodes = new ArrayDeque<>();
        private final List<Integer> bufferSizes = new ArrayList<>();

        StubParams withReturnCodes(Integer... codes) {
            returnCodes.addAll(List.of(codes));
            return this;
        }

        @Override
        protected int queryAdapterDnsServers(int bufferSize, int[] requiredSize, List<byte[]> addresses) {
            bufferSizes.add(bufferSize);
            int ret = returnCodes.isEmpty() ? 0 : returnCodes.poll();
            // A too-small buffer reports a larger one; a failed call may still have written partial results
            requiredSize[0] = bufferSize + 1000;
            addresses.addAll(this.dnsServers);
            return ret;
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

    @Test
    void testAdapterQueryGrowsTheBufferWhenTheListOutgrowsIt() {
        byte[] router = { -64, -88, 1, 1 };
        StubParams params = new StubParams(List.of(), List.of(router)).withReturnCodes(111, 111);
        assertThat(params.getDnsServers(), is(new String[] { "192.168.1.1" }));
        assertThat("Each retry passes the size the previous call reported", params.bufferSizes,
                is(List.of(15_000, 16_000, 17_000)));
    }

    @Test
    void testAdapterQueryGivesUpAfterThreeOverflows() {
        StubParams params = new StubParams(List.of(), List.of(new byte[] { -64, -88, 1, 1 })).withReturnCodes(111, 111,
                111);
        assertThat(params.getDnsServers(), is(new String[0]));
        assertThat(params.bufferSizes, hasSize(3));
    }

    @Test
    void testAdapterQueryErrorsReturnNothing() {
        // ERROR_NO_DATA (no adapters) and a real error: whatever a failed call wrote is discarded
        List<byte[]> partial = List.of(new byte[] { -64, -88, 1, 1 });
        assertThat(new StubParams(List.of(), partial).withReturnCodes(232).getDnsServers(), is(new String[0]));
        assertThat(new StubParams(List.of(), partial).withReturnCodes(87).getDnsServers(), is(new String[0]));
    }
}
