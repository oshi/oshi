/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

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
 * Tests the routing table parsers against output captured verbatim from real hosts: macOS on the development machine,
 * and AIX and Solaris 11.4 SPARC on the GCC compile farm.
 */
class NetstatRouteTest {

    private static final Map<String, Integer> NO_INDICES = Collections.emptyMap();

    // Verbatim `netstat -rn -f inet` from macOS. Note the trailing "!" on two rows and the empty Expire column on the
    // default route, which make the field count vary from row to row.
    private static final List<String> MACOS_V4 = Arrays.asList("Routing tables", //
            "", //
            "Internet:", //
            "Destination        Gateway            Flags               Netif Expire", //
            "default            10.0.0.1           UGScg                 en0       ", //
            "10/24              link#14            UCS                   en0      !", //
            "10.0.0.1/32        link#14            UCS                   en0      !", //
            "10.0.0.1           40:f:c1:cb:2a:97   UHLWIir               en0   1198", //
            "10.0.0.3           d4:80:8b:1e:6b:b9  UHLWI                 en0   1196");

    // Verbatim `netstat -rn -f inet6` from macOS.
    private static final List<String> MACOS_V6 = Arrays.asList("Routing tables", //
            "", //
            "Internet6:", //
            "Destination                             Gateway                                 Flags               Netif Expire", //
            "default                                 fe80::420f:c1ff:fecb:2a97%en0           UGcg                  en0       ", //
            "default                                 fe80::%utun0                            UGcIg               utun0       ", //
            "::1                                     ::1                                     UHL                   lo0       ", //
            "2601:601:d47c:3090::/64                 link#14                                 UC                    en0       ", //
            "2601:601:d47c:3090::13e2                0:11:32:c5:e:9b                         UHLWI                 en0       ");

    // Verbatim `netstat -rnf inet` from AIX. The column header precedes the section banner here, the reverse of the
    // macOS order, and two rows carry a trailing "=>".
    private static final List<String> AIX_V4 = Arrays.asList("Routing tables", //
            "Destination        Gateway           Flags   Refs     Use  If   Exp  Groups", //
            "", //
            "Route Tree for Protocol Family 2 (Internet):", //
            "default            140.211.9.1       UG      124  27595284 en1      -      -   ", //
            "10.1.0.0           10.1.0.3          UHSb      0         0 en0      -      -   =>", //
            "10.1/23            10.1.0.3          U         0   1422409 en0      -      -   ", //
            "10.1.0.3           127.0.0.1         UGHS      0    313680 lo0      -      -   ", //
            "127/8              127.0.0.1         U         3   3564888 lo0      -      -   ", //
            "140.211.9/24       140.211.9.96      U         1   1835134 en1      -      -   ");

    // Verbatim `netstat -rnf inet6` from AIX.
    private static final List<String> AIX_V6 = Arrays.asList("Routing tables", //
            "Destination        Gateway           Flags   Refs     Use  If   Exp  Groups", //
            "", //
            "Route Tree for Protocol Family 24 (Internet v6):", //
            "::1%1              ::1%1             UH        1    466765 lo0      -      -   ");

    // Verbatim `netstat -rnv -f inet` from Solaris 11.4. The default route has an empty Device column, which collapses
    // under whitespace splitting and shifts every following token left by one.
    private static final List<String> SOLARIS_V4 = Arrays.asList("", //
            "IRE Table: IPv4", //
            "  Destination             Mask           Gateway          Device  MTU  Ref Flg  Out  In/Fwd ", //
            "-------------------- --------------- -------------------- ------ ----- --- --- ----- ------ ", //
            "default              0.0.0.0         129.70.163.177                  0   5 UG  5666596      0 ", //
            "127.0.0.1            255.255.255.255 127.0.0.1            lo0     8232   2 UH   31396  31391 ", //
            "129.70.163.176       255.255.255.248 129.70.163.179       net0    1500   3 U     2181      0 ");

    // Verbatim `netstat -rnv -f inet6` from Solaris 11.4. IPv6 has no Mask column and puts the flags one position
    // earlier than IPv4 does.
    private static final List<String> SOLARIS_V6 = Arrays.asList("", //
            "IRE Table: IPv6", //
            "  Destination/Mask            Gateway                    If    MTU  Ref Flags  Out   In/Fwd ", //
            "--------------------------- --------------------------- ----- ----- --- ----- ------ ------ ", //
            "::1                         ::1                         lo0    8252   2 UH      1113   1113 ", //
            "fe80::/10                   fe80::214:4fff:fefb:a5d7    net0   1500   2 U          0      0 ");

    @Test
    void testParseMacOsIpv4() {
        List<IPRoute> routes = NetstatRoute.parseRoutes(MACOS_V4, false, 3, NO_INDICES);
        assertThat("Banner, blank and header lines should be skipped", routes, hasSize(5));

        IPRoute def = routes.get(0);
        assertThat(def.getDestination(), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(def.getPrefixLength(), is(0));
        assertThat(def.isGateway(), is(true));
        assertThat(def.getGateway(), is(new byte[] { 10, 0, 0, 1 }));
        assertThat(def.getInterfaceName(), is("en0"));
        assertThat(def.isHost(), is(false));
        assertThat("macOS publishes no metric column", def.getMetric(), is(-1L));

        IPRoute abbreviated = routes.get(1);
        assertThat("10/24 should expand to 10.0.0.0", abbreviated.getDestination(), is(new byte[] { 10, 0, 0, 0 }));
        assertThat(abbreviated.getPrefixLength(), is(24));
        assertThat("link#14 is not a gateway", abbreviated.isGateway(), is(false));
        assertThat(abbreviated.getGateway(), is(new byte[0]));
        assertThat("The trailing ! should not shift the interface column", abbreviated.getInterfaceName(), is("en0"));

        assertThat(routes.get(2).getPrefixLength(), is(32));
        assertThat(routes.get(2).isHost(), is(true));

        // The regression this rule exists for: an ARP cache row whose gateway column holds a MAC address
        IPRoute arp = routes.get(3);
        assertThat(arp.getDestination(), is(new byte[] { 10, 0, 0, 1 }));
        assertThat("A host route with no prefix takes its width from the H flag", arp.getPrefixLength(), is(32));
        assertThat(arp.isHost(), is(true));
        assertThat(arp.isGateway(), is(false));
        assertThat("A MAC address must never be reported as a gateway", arp.getGateway(), is(new byte[0]));
        assertThat(routes.get(4).getGateway(), is(new byte[0]));
    }

    @Test
    void testParseMacOsIpv6() {
        List<IPRoute> routes = NetstatRoute.parseRoutes(MACOS_V6, true, 3, NO_INDICES);
        assertThat(routes, hasSize(5));

        // A default route is not unique: this host has one real one and several over tunnel interfaces
        assertThat(routes.get(0).getDestination(), is(new byte[16]));
        assertThat(routes.get(0).getPrefixLength(), is(0));
        assertThat(routes.get(0).isGateway(), is(true));
        assertThat("The %en0 zone should be stripped", routes.get(0).getGateway(),
                is(ParseUtil.parseIpv6AddressToBytes("fe80::420f:c1ff:fecb:2a97")));
        assertThat(routes.get(0).getInterfaceName(), is("en0"));

        assertThat(routes.get(1).getDestination(), is(new byte[16]));
        assertThat("The %utun0 zone should be stripped", routes.get(1).getGateway(),
                is(ParseUtil.parseIpv6AddressToBytes("fe80::")));
        assertThat(routes.get(1).getInterfaceName(), is("utun0"));

        assertThat(routes.get(2).getDestination(), is(ParseUtil.parseIpv6AddressToBytes("::1")));
        assertThat(routes.get(2).getPrefixLength(), is(128));
        assertThat(routes.get(2).isHost(), is(true));

        assertThat(routes.get(3).getPrefixLength(), is(64));
        assertThat(routes.get(3).isGateway(), is(false));

        assertThat("An unpadded MAC must not parse as an IPv6 gateway", routes.get(4).getGateway(), is(new byte[0]));
    }

    @Test
    void testParseAixIpv4() {
        List<IPRoute> routes = NetstatRoute.parseRoutes(AIX_V4, false, 5, NO_INDICES);
        assertThat("The Route Tree banner should be skipped", routes, hasSize(6));

        IPRoute def = routes.get(0);
        assertThat(def.getDestination(), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(def.isGateway(), is(true));
        assertThat(def.getGateway(), is(new byte[] { -116, -45, 9, 1 }));
        assertThat("AIX puts the interface in column 5", def.getInterfaceName(), is("en1"));
        assertThat(def.getMetric(), is(-1L));

        IPRoute trailing = routes.get(1);
        assertThat("A trailing => must not be read as a column", trailing.getInterfaceName(), is("en0"));
        assertThat(trailing.isHost(), is(true));
        assertThat(trailing.getPrefixLength(), is(32));
        assertThat(trailing.getGateway(), is(new byte[0]));

        IPRoute abbreviated = routes.get(2);
        assertThat("10.1/23 should expand to 10.1.0.0", abbreviated.getDestination(), is(new byte[] { 10, 1, 0, 0 }));
        assertThat(abbreviated.getPrefixLength(), is(23));

        IPRoute gatewayHost = routes.get(3);
        assertThat(gatewayHost.isGateway(), is(true));
        assertThat(gatewayHost.isHost(), is(true));
        assertThat(gatewayHost.getGateway(), is(new byte[] { 127, 0, 0, 1 }));

        assertThat(routes.get(4).getDestination(), is(new byte[] { 127, 0, 0, 0 }));
        assertThat(routes.get(4).getPrefixLength(), is(8));
        assertThat(routes.get(5).getDestination(), is(new byte[] { -116, -45, 9, 0 }));
        assertThat(routes.get(5).getPrefixLength(), is(24));
    }

    @Test
    void testParseAixIpv6() {
        List<IPRoute> routes = NetstatRoute.parseRoutes(AIX_V6, true, 5, NO_INDICES);
        assertThat(routes, hasSize(1));
        assertThat("The numeric %1 zone should be stripped", routes.get(0).getDestination(),
                is(ParseUtil.parseIpv6AddressToBytes("::1")));
        assertThat(routes.get(0).getPrefixLength(), is(128));
        assertThat(routes.get(0).isHost(), is(true));
        assertThat(routes.get(0).isGateway(), is(false));
        assertThat(routes.get(0).getGateway(), is(new byte[0]));
        assertThat(routes.get(0).getInterfaceName(), is("lo0"));
    }

    @Test
    void testParseSolarisIpv4() {
        List<IPRoute> routes = NetstatRoute.parseSolarisRoutes(SOLARIS_V4, false, NO_INDICES);
        assertThat("The banner, header and dashed separator should be skipped", routes, hasSize(3));

        // The collapsed row: an empty Device column shifts the flags from index 6 to index 5
        IPRoute def = routes.get(0);
        assertThat(def.getDestination(), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(def.getPrefixLength(), is(0));
        assertThat(def.isGateway(), is(true));
        assertThat("Reading flags right to left survives the collapsed Device column", def.getGateway(),
                is(new byte[] { -127, 70, -93, -79 }));
        assertThat("A collapsed Device column leaves no interface to report", def.getInterfaceName(), is(""));

        IPRoute loopback = routes.get(1);
        assertThat(loopback.getInterfaceName(), is("lo0"));
        assertThat("The prefix comes from the Mask column, which -rn does not print", loopback.getPrefixLength(),
                is(32));
        assertThat(loopback.isHost(), is(true));
        assertThat(loopback.isGateway(), is(false));
        assertThat(loopback.getGateway(), is(new byte[0]));

        IPRoute subnet = routes.get(2);
        assertThat(subnet.getInterfaceName(), is("net0"));
        assertThat("255.255.255.248 is a /29", subnet.getPrefixLength(), is(29));
        assertThat(subnet.isHost(), is(false));
        assertThat(subnet.getMetric(), is(-1L));
    }

    @Test
    void testParseSolarisIpv6() {
        List<IPRoute> routes = NetstatRoute.parseSolarisRoutes(SOLARIS_V6, true, NO_INDICES);
        assertThat(routes, hasSize(2));

        assertThat(routes.get(0).getDestination(), is(ParseUtil.parseIpv6AddressToBytes("::1")));
        assertThat("IPv6 puts the flags one column earlier than IPv4", routes.get(0).getPrefixLength(), is(128));
        assertThat(routes.get(0).isHost(), is(true));
        assertThat(routes.get(0).getInterfaceName(), is("lo0"));

        assertThat(routes.get(1).getDestination(), is(ParseUtil.parseIpv6AddressToBytes("fe80::")));
        assertThat(routes.get(1).getPrefixLength(), is(10));
        assertThat(routes.get(1).isGateway(), is(false));
        assertThat(routes.get(1).getGateway(), is(new byte[0]));
        assertThat(routes.get(1).getInterfaceName(), is("net0"));
    }

    /**
     * DragonFly BSD prints Refs and Use columns that macOS does not, putting Netif at index 5 rather than 3. The header
     * scan should override the caller's default index, so the same parser serves both without a per-platform class.
     */
    @Test
    void testHeaderScanOverridesDefaultInterfaceIndex() {
        List<String> dragonFly = Arrays.asList("Routing tables", //
                "", //
                "Internet:", //
                "Destination        Gateway            Flags     Refs     Use   Netif Expire", //
                "default            10.0.0.1           UGS         0        0     em0");
        List<IPRoute> routes = NetstatRoute.parseRoutes(dragonFly, false, 3, NO_INDICES);
        assertThat(routes, hasSize(1));
        assertThat("The header should move the interface column from 3 to 5", routes.get(0).getInterfaceName(),
                is("em0"));
    }

    /**
     * OpenBSD is the only BSD publishing a route metric, in a Prio column, and the only platform whose interface sits
     * in the eighth column. This layout is taken from the netstat documentation rather than captured, since no OpenBSD
     * host was available; the header scan means a wrong default index self-corrects.
     */
    @Test
    void testParseOpenBsdPrioMetric() {
        List<String> openBsd = Arrays.asList("Routing tables", "", "Internet:",
                "Destination        Gateway            Flags   Refs      Use   Mtu  Prio Iface",
                "default            192.168.1.1        UGS        0        8     -     8 em0",
                "224/4              127.0.0.1          URS        0        0 32768     8 lo0");
        List<IPRoute> routes = NetstatRoute.parseRoutes(openBsd, false, 7, NO_INDICES);
        assertThat(routes, hasSize(2));
        assertThat(routes.get(0).getDestination(), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(routes.get(0).isGateway(), is(true));
        assertThat(routes.get(0).getGateway(), is(new byte[] { -64, -88, 1, 1 }));
        assertThat("The header should locate Iface in the eighth column", routes.get(0).getInterfaceName(), is("em0"));
        assertThat("OpenBSD publishes a metric where the other BSDs do not", routes.get(0).getMetric(), is(8L));
        assertThat(routes.get(1).getDestination(), is(new byte[] { -32, 0, 0, 0 }));
        assertThat(routes.get(1).getPrefixLength(), is(4));
        assertThat(routes.get(1).getMetric(), is(8L));
    }

    /** A header naming no interface column at all leaves the caller's default index in place. */
    @Test
    void testHeaderWithoutAnInterfaceColumn() {
        List<String> lines = Arrays.asList("Destination        Gateway            Flags",
                "default            10.0.0.1           UGSc                  en0");
        List<IPRoute> routes = NetstatRoute.parseRoutes(lines, false, 3, NO_INDICES);
        assertThat(routes, hasSize(1));
        assertThat(routes.get(0).getInterfaceName(), is("en0"));
    }

    /** A line with enough columns but no flags field is not a route. */
    @Test
    void testLineWithoutAFlagsColumnIsSkipped() {
        List<String> lines = Arrays.asList("10.0.0.1           10.0.0.254         1198                  en0",
                "default            10.0.0.1           UGSc                  en0");
        List<IPRoute> routes = NetstatRoute.parseRoutes(lines, false, 3, NO_INDICES);
        assertThat("Only the row whose third column is a flags field should parse", routes, hasSize(1));
        assertThat(routes.get(0).getDestination(), is(new byte[] { 0, 0, 0, 0 }));
    }

    /** A row shorter than the interface column reports no name rather than throwing. */
    @Test
    void testRowShorterThanTheInterfaceColumn() {
        List<IPRoute> routes = NetstatRoute.parseRoutes(Collections.singletonList("default   10.0.0.1   UGSc"), false,
                3, NO_INDICES);
        assertThat(routes, hasSize(1));
        assertThat(routes.get(0).getInterfaceName(), is(""));
        assertThat(routes.get(0).getInterfaceIndex(), is(-1));
    }

    /**
     * Without a recognizable header the caller's default index is used, and a wrong index must degrade to an empty name
     * rather than reporting a reference count as an interface.
     */
    @Test
    void testWrongInterfaceIndexDegradesToEmpty() {
        List<String> headerless = Collections
                .singletonList("default            10.0.0.1           UGS         0        0     em0");
        List<IPRoute> routes = NetstatRoute.parseRoutes(headerless, false, 3, NO_INDICES);
        assertThat(routes, hasSize(1));
        assertThat("A numeric token must not be reported as an interface name", routes.get(0).getInterfaceName(),
                is(""));
        assertThat(routes.get(0).getInterfaceIndex(), is(-1));
    }

    @Test
    void testInterfaceIndexLookup() {
        Map<String, Integer> indices = new HashMap<>();
        indices.put("en0", 14);
        List<IPRoute> routes = NetstatRoute.parseRoutes(MACOS_V4, false, 3, indices);
        assertThat(routes.get(0).getInterfaceIndex(), is(14));
    }

    @Test
    void testEmptyInput() {
        assertThat(NetstatRoute.parseRoutes(Collections.emptyList(), false, 3, NO_INDICES), hasSize(0));
        assertThat(NetstatRoute.parseSolarisRoutes(Collections.emptyList(), false, NO_INDICES), hasSize(0));
    }
}
