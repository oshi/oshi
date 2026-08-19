/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import oshi.SystemInfo;
import oshi.driver.unix.bsd.BsdRouteDump;
import oshi.hardware.NetworkIF;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.ParseUtil;
import oshi.util.PlatformEnum;
import oshi.util.driver.unix.NetstatRoute;
import oshi.util.driver.unix.RouteTableDump;

/**
 * Compares the routing table read natively through a {@code NET_RT_DUMP} sysctl against the same table read by running
 * {@code netstat}.
 * <p>
 * These are two readings of one kernel table, so they should agree. They are worth comparing because the native path
 * has to reproduce conventions the command applies on its way to printing: a routing socket embeds an interface index
 * inside link-local and scoped multicast addresses, and lists cache entries the table does not show. Each of those was
 * caught here rather than by reading the headers.
 */
@DisabledIf("isNotBsd")
class RouteComparisonTest {

    private static PlatformEnum platform;
    private static String ipv4Command;
    private static String ipv6Command;
    private static int ifNameIndex;

    @BeforeAll
    static void setUp() {
        platform = PlatformEnum.getCurrentPlatform();
        ipv4Command = "netstat -rn -f inet";
        ipv6Command = "netstat -rn -f inet6";
        // The column holding the interface name, which differs by platform
        ifNameIndex = platform == PlatformEnum.OPENBSD ? 7 : 3;
    }

    /** Only the platforms with a native routing table implementation. */
    static boolean isNotBsd() {
        PlatformEnum p = PlatformEnum.getCurrentPlatform();
        return p != PlatformEnum.MACOS && p != PlatformEnum.FREEBSD && p != PlatformEnum.OPENBSD;
    }

    /**
     * A route's full identity. The gateway belongs in it: an address the walk reads at the wrong offset still yields a
     * correct destination, since that is the first address in the message and no padding has been applied yet.
     */
    private static String key(IPRoute route) {
        return destination(route) + " via " + ParseUtil.byteArrayToHexString(route.getGateway());
    }

    /**
     * A route's destination alone. The gateway comparison has to match routes on this rather than on the full key,
     * which carries the gateway and so would only ever pair a route with one that already agrees.
     */
    private static String destination(IPRoute route) {
        return ParseUtil.byteArrayToHexString(route.getDestination()) + "/" + route.getPrefixLength();
    }

    private static Set<String> keys(List<IPRoute> routes) {
        Set<String> keys = new TreeSet<>();
        for (IPRoute route : routes) {
            keys.add(key(route));
        }
        return keys;
    }

    private static List<IPRoute> commandRoutes() {
        Map<String, Integer> indexByName = new HashMap<>();
        for (NetworkIF nif : new SystemInfo().getHardware().getNetworkIFs(true)) {
            indexByName.put(nif.getName(), nif.getIndex());
        }
        return NetstatRoute.queryRoutes(ipv4Command, ipv6Command, ifNameIndex, indexByName);
    }

    /** The kernel's dump, and the layout for reading it. */
    private static byte[] nativeDump() {
        switch (platform) {
            case MACOS:
                return oshi.driver.mac.net.RouteDump.queryRouteDump();
            case FREEBSD:
                return BsdRouteDump.queryRouteDump(FreeBsdLibc.INSTANCE);
            default:
                return BsdRouteDump.queryRouteDump(OpenBsdLibc.INSTANCE);
        }
    }

    private static RouteTableDump.Layout layout() {
        switch (platform) {
            case MACOS:
                return RouteTableDump.Layout.MACOS;
            case FREEBSD:
                return RouteTableDump.Layout.FREEBSD;
            default:
                return RouteTableDump.Layout.OPENBSD;
        }
    }

    /**
     * Reads the table natively. Deliberately not through {@code getRoutes()}, which falls back to running a command
     * when the sysctl yields nothing -- that would compare the command against itself and pass without testing
     * anything.
     */
    private static List<IPRoute> nativeRoutes() {
        byte[] dump = nativeDump();
        assertThat(dump).as("NET_RT_DUMP buffer").isNotEmpty();
        Map<Integer, String> nameByIndex = new HashMap<>();
        for (NetworkIF nif : new SystemInfo().getHardware().getNetworkIFs(true)) {
            nameByIndex.put(nif.getIndex(), nif.getName());
        }
        List<IPRoute> routes = RouteTableDump.parse(dump, layout(), nameByIndex);
        assertThat(routes).as("routes parsed from the dump").isNotEmpty();
        return routes;
    }

    @Test
    void testNativeAgreesWithCommand() {
        List<IPRoute> nativeList = nativeRoutes();
        List<IPRoute> commandList = commandRoutes();
        assumeTrue(!commandList.isEmpty(), "netstat produced no routes to compare against");

        Set<String> nativeKeys = keys(nativeList);
        Set<String> commandKeys = keys(commandList);
        Set<String> shared = new TreeSet<>(nativeKeys);
        shared.retainAll(commandKeys);

        // Routes come and go between the two readings, so require a large majority rather than equality. A parsing
        // fault does not look like churn: it moves every address at once.
        assertThat(shared.size())
                .as("destinations seen by both of %d native and %d from netstat", nativeKeys.size(), commandKeys.size())
                .isGreaterThanOrEqualTo((Math.min(nativeKeys.size(), commandKeys.size()) * 3) / 4);
    }

    @Test
    void testGatewaysAgree() {
        List<IPRoute> nativeList = nativeRoutes();
        List<IPRoute> commandList = commandRoutes();
        assumeTrue(!commandList.isEmpty(), "netstat produced no routes to compare against");

        Map<String, String> commandGateways = new HashMap<>();
        for (IPRoute route : commandList) {
            commandGateways.put(destination(route), ParseUtil.byteArrayToHexString(route.getGateway()));
        }
        // Only routes that carry a gateway say anything here. Most do not, and counting them dilutes a real
        // disagreement to a few percent of the comparison
        List<String> mismatches = new ArrayList<>();
        int matched = 0;
        for (IPRoute route : nativeList) {
            String expected = commandGateways.get(destination(route));
            String actual = ParseUtil.byteArrayToHexString(route.getGateway());
            if (expected == null || (expected.isEmpty() && actual.isEmpty())) {
                continue;
            }
            matched++;
            if (!expected.equals(actual)) {
                mismatches.add(destination(route));
            }
        }
        assumeTrue(matched >= 4, "too few routes with a gateway to compare");
        // A gateway rarely changes between two readings, and a misread address changes all of them
        assertThat(mismatches).as("gateways disagreeing on %d routes carrying one", matched)
                .hasSizeLessThanOrEqualTo(matched / 4);
    }

    @Test
    void testPublicApiUsesTheNativePath() {
        // getRoutes() should return what the dump says, not what the command fallback would
        List<IPRoute> viaApi = new SystemInfo().getOperatingSystem().getNetworkParams().getRoutes();
        assertThat(keys(viaApi)).as("routes from getRoutes()").isEqualTo(keys(nativeRoutes()));
    }

    @Test
    void testPrefixLengthsAreValid() {
        // A prefix outside this range means the netmask was read from the wrong place. It is worth asserting
        // separately from the comparison above, because a misread address array corrupts only the routes whose
        // padding differs between the candidate units, which can still leave the two lists overlapping heavily.
        for (IPRoute route : nativeRoutes()) {
            int bits = route.getDestination().length * 8;
            assertThat(route.getPrefixLength()).as("prefix length of %s", key(route)).isBetween(0, bits);
        }
    }
}
