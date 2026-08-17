/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.ParseUtil;

/**
 * Tests the Linux routing table parsers against {@code /proc} content captured verbatim from a Linux container, with
 * the corresponding {@code ip route} output used to confirm every decoded value.
 */
class RouteTableTest {

    private static final Map<String, Integer> NO_INDICES = Collections.emptyMap();

    // Verbatim /proc/net/route. The matching `ip route show` reported:
    // default via 172.17.0.1 dev eth0
    // 172.17.0.0/16 dev eth0 scope link src 172.17.0.2
    private static final List<String> PROC_NET_ROUTE = Arrays.asList(
            "Iface\tDestination\tGateway \tFlags\tRefCnt\tUse\tMetric\tMask\t\tMTU\tWindow\tIRTT                   ", //
            "eth0\t00000000\t010011AC\t0003\t0\t0\t0\t00000000\t0\t0\t0                                           ", //
            "eth0\t000011AC\t00000000\t0001\t0\t0\t0\t0000FFFF\t0\t0\t0                                           ");

    // Verbatim /proc/net/ipv6_route, with a 2001:db8:1122:3344::1/64 address added to lo so the byte order is
    // unambiguous. The matching `ip -6 route show` reported:
    // 2001:db8:1122:3344::/64 dev lo metric 256
    // fe80::/64 dev eth0 metric 256
    private static final List<String> PROC_NET_IPV6_ROUTE = Arrays.asList(
            "20010db8112233440000000000000000 40 00000000000000000000000000000000 00 00000000000000000000000000000000 00000100 00000001 00000000 00200200       lo", //
            "fe800000000000000000000000000000 40 00000000000000000000000000000000 00 00000000000000000000000000000000 00000100 00000001 00000000 00000001     eth0", //
            "00000000000000000000000000000000 00 00000000000000000000000000000000 00 00000000000000000000000000000000 ffffffff 00000001 00000000 00200200       lo", //
            "00000000000000000000000000000001 80 00000000000000000000000000000000 00 00000000000000000000000000000000 00000000 00000003 00000000 80200001       lo", //
            "20010db8112233440000000000000001 80 00000000000000000000000000000000 00 00000000000000000000000000000000 00000000 00000002 00000000 80200001       lo", //
            "ff000000000000000000000000000000 08 00000000000000000000000000000000 00 00000000000000000000000000000000 00000100 00000002 00000000 00000001     eth0");

    @Test
    void testParseIpv4Routes() {
        List<IPRoute> routes = RouteTable.parseIpv4Routes(PROC_NET_ROUTE, NO_INDICES);
        assertThat("The header line should be skipped", routes, hasSize(2));

        IPRoute def = routes.get(0);
        assertThat(def.getDestination(), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(def.getPrefixLength(), is(0));
        assertThat(def.isGateway(), is(true));
        // 010011AC little-endian decodes to 172.17.0.1, matching "default via 172.17.0.1"
        assertThat(def.getGateway(), is(new byte[] { -84, 17, 0, 1 }));
        assertThat(def.getInterfaceName(), is("eth0"));
        assertThat(def.isHost(), is(false));
        assertThat("Linux publishes a metric column", def.getMetric(), is(0L));

        IPRoute subnet = routes.get(1);
        // 000011AC decodes to 172.17.0.0 and mask 0000FFFF to 255.255.0.0, matching "172.17.0.0/16"
        assertThat(subnet.getDestination(), is(new byte[] { -84, 17, 0, 0 }));
        assertThat(subnet.getPrefixLength(), is(16));
        assertThat(subnet.isGateway(), is(false));
        assertThat(subnet.getGateway(), is(new byte[0]));
        assertThat(subnet.isHost(), is(false));
    }

    @Test
    void testParseIpv6Routes() {
        List<IPRoute> routes = RouteTable.parseIpv6Routes(PROC_NET_IPV6_ROUTE, NO_INDICES);
        assertThat("There is no header line to skip", routes, hasSize(6));

        // The address is printed in network order, so no per-word reversal is applied. This is what distinguishes
        // /proc/net/ipv6_route from /proc/net/tcp6, and 2001:db8:1122:3344 is asymmetric enough to prove it.
        IPRoute added = routes.get(0);
        assertThat(added.getDestination(), is(ParseUtil.parseIpv6AddressToBytes("2001:db8:1122:3344::")));
        assertThat("Prefix length is hex: 40 is 64", added.getPrefixLength(), is(64));
        assertThat("Metric is hex: 00000100 is 256", added.getMetric(), is(256L));
        assertThat(added.getInterfaceName(), is("lo"));
        assertThat(added.isGateway(), is(false));
        assertThat(added.isHost(), is(false));

        assertThat(routes.get(1).getDestination(), is(ParseUtil.parseIpv6AddressToBytes("fe80::")));
        assertThat(routes.get(1).getPrefixLength(), is(64));
        assertThat(routes.get(1).getInterfaceName(), is("eth0"));

        assertThat(routes.get(2).getDestination(), is(new byte[16]));
        assertThat(routes.get(2).getPrefixLength(), is(0));
        assertThat("ffffffff is an unreachable route's metric, not a negative", routes.get(2).getMetric(),
                is(4294967295L));

        // Flags 80200001 does not include RTF_HOST (0x4), so a /128 is only recognized by its prefix width
        IPRoute loopback = routes.get(3);
        assertThat(loopback.getDestination(), is(ParseUtil.parseIpv6AddressToBytes("::1")));
        assertThat(loopback.getPrefixLength(), is(128));
        assertThat("A /128 local route carries no RTF_HOST flag, so the prefix width must decide", loopback.isHost(),
                is(true));

        assertThat(routes.get(4).getDestination(), is(ParseUtil.parseIpv6AddressToBytes("2001:db8:1122:3344::1")));
        assertThat(routes.get(4).isHost(), is(true));

        assertThat(routes.get(5).getDestination(), is(ParseUtil.parseIpv6AddressToBytes("ff00::")));
        assertThat("Prefix length is hex: 08 is 8", routes.get(5).getPrefixLength(), is(8));
    }

    @Test
    void testInterfaceIndexLookup() {
        Map<String, Integer> indices = new HashMap<>();
        indices.put("eth0", 2);
        List<IPRoute> routes = RouteTable.parseIpv4Routes(PROC_NET_ROUTE, indices);
        assertThat(routes.get(0).getInterfaceIndex(), is(2));
    }

    @Test
    void testEmptyAndMalformedInput() {
        assertThat(RouteTable.parseIpv4Routes(Collections.emptyList(), NO_INDICES), hasSize(0));
        assertThat(RouteTable.parseIpv6Routes(Collections.emptyList(), NO_INDICES), hasSize(0));
        assertThat(RouteTable.parseIpv4Routes(Collections.singletonList("truncated\tline"), NO_INDICES), hasSize(0));
        assertThat(RouteTable.parseIpv6Routes(Collections.singletonList("not hex at all"), NO_INDICES), hasSize(0));
    }
}
