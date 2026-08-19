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
import oshi.hardware.NetworkIF;
import oshi.software.os.NetworkParams;
import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.ParseUtil;
import oshi.util.PlatformEnum;
import oshi.util.driver.unix.NetstatRoute;

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

    private static String key(IPRoute route) {
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

    @Test
    void testNativeAgreesWithCommand() {
        NetworkParams params = new SystemInfo().getOperatingSystem().getNetworkParams();
        List<IPRoute> nativeRoutes = params.getRoutes();
        List<IPRoute> commandList = commandRoutes();

        assertThat(nativeRoutes).as("native routes").isNotEmpty();
        assumeTrue(!commandList.isEmpty(), "netstat produced no routes to compare against");

        Set<String> nativeKeys = keys(nativeRoutes);
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
        List<IPRoute> nativeRoutes = new SystemInfo().getOperatingSystem().getNetworkParams().getRoutes();
        List<IPRoute> commandList = commandRoutes();
        assumeTrue(!commandList.isEmpty(), "netstat produced no routes to compare against");

        Map<String, String> commandGateways = new HashMap<>();
        for (IPRoute route : commandList) {
            commandGateways.put(key(route), ParseUtil.byteArrayToHexString(route.getGateway()));
        }
        List<String> mismatches = new ArrayList<>();
        int matched = 0;
        for (IPRoute route : nativeRoutes) {
            String expected = commandGateways.get(key(route));
            if (expected != null) {
                matched++;
                if (!expected.equals(ParseUtil.byteArrayToHexString(route.getGateway()))) {
                    mismatches.add(key(route));
                }
            }
        }
        assumeTrue(matched >= 4, "too few shared routes to compare gateways");
        // A gateway rarely changes between two readings, and a misread address changes all of them
        assertThat(mismatches).as("gateways disagreeing on %d shared routes", matched)
                .hasSizeLessThanOrEqualTo(matched / 4);
    }
}
