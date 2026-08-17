/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query the routing table from {@code netstat}.
 * <p>
 * Two layouts are handled. The BSD-derived one, used by macOS, the BSDs and AIX, always places the destination, gateway
 * and flags in the first three whitespace-delimited columns and varies only in where the interface name sits. The SVR4
 * one, used by Solaris and illumos, differs between address families and can shift its trailing columns, so it gets its
 * own parser.
 */
@ThreadSafe
public final class NetstatRoute {

    /**
     * An interface name is a letter followed by name characters. Used to reject a numeric or decorative token that a
     * miscounted column index would otherwise report as an interface.
     */
    private static final Pattern INTERFACE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9._:@-]*");

    /** Column headers under which each platform prints the interface name. */
    private static final String[] INTERFACE_HEADERS = { "Netif", "Iface", "Interface", "If", "Device" };

    private NetstatRoute() {
    }

    /**
     * Queries both address families from a BSD-derived {@code netstat} and returns their routes as one list.
     * <p>
     * Each family is queried with its own explicitly-scoped command rather than reading a combined table, which removes
     * any need to detect the section banner separating the two halves and makes the literal {@code default}
     * unambiguous.
     *
     * @param ipv4Command        the command listing the IPv4 table
     * @param ipv6Command        the command listing the IPv6 table
     * @param defaultIfNameIndex the token index of the interface name, used when no column header is recognized
     * @param ifIndexByName      a map of interface name to index; an empty map reports every index as -1
     * @return the parsed routes for both families
     */
    public static List<IPRoute> queryRoutes(String ipv4Command, String ipv6Command, int defaultIfNameIndex,
            Map<String, Integer> ifIndexByName) {
        List<IPRoute> routes = new ArrayList<>(queryRoutes(ipv4Command, false, defaultIfNameIndex, ifIndexByName));
        routes.addAll(queryRoutes(ipv6Command, true, defaultIfNameIndex, ifIndexByName));
        return routes;
    }

    /**
     * Queries and parses a BSD-derived {@code netstat} routing table.
     *
     * @param command            the command to run, which must name a single address family
     * @param ipv6               whether the command selects the IPv6 table
     * @param defaultIfNameIndex the token index of the interface name, used when no column header is recognized
     * @param ifIndexByName      a map of interface name to index; an empty map reports every index as -1
     * @return the parsed routes
     */
    public static List<IPRoute> queryRoutes(String command, boolean ipv6, int defaultIfNameIndex,
            Map<String, Integer> ifIndexByName) {
        return parseRoutes(ExecutingCommand.runNative(command), ipv6, defaultIfNameIndex, ifIndexByName);
    }

    /**
     * Parses the BSD-derived {@code netstat} routing table layout used by macOS, FreeBSD, DragonFly BSD, NetBSD,
     * OpenBSD and AIX, in which the destination, gateway and flags are always the first three whitespace-delimited
     * tokens.
     * <p>
     * Rows are recognized structurally rather than by matching header text, because the three verified platforms print
     * their banner, column header and separator in three different orders, and header text may be localized.
     *
     * @param netstat            the lines of {@code netstat} output for one address family
     * @param ipv6               whether this is the IPv6 table
     * @param defaultIfNameIndex the token index of the interface name, used when no column header is recognized
     * @param ifIndexByName      a map of interface name to index; an empty map reports every index as -1
     * @return the parsed routes
     */
    static List<IPRoute> parseRoutes(List<String> netstat, boolean ipv6, int defaultIfNameIndex,
            Map<String, Integer> ifIndexByName) {
        int ifNameIndex = defaultIfNameIndex;
        int metricIndex = -1;
        List<IPRoute> routes = new ArrayList<>();
        for (String line : netstat) {
            String[] fields = ParseUtil.whitespaces.split(line.trim(), -1);
            if (fields.length < 3) {
                continue;
            }
            // A column header both identifies itself and tells us where this platform puts the interface name, which
            // is the only thing that varies across the six platforms sharing this layout.
            if (isColumnHeader(fields[0])) {
                int headerIfIndex = indexOfAny(fields, INTERFACE_HEADERS);
                if (headerIfIndex >= 0) {
                    ifNameIndex = headerIfIndex;
                }
                metricIndex = indexOfAny(fields, "Prio");
                continue;
            }
            IPRoute route = parseBsdRow(fields, ipv6, ifNameIndex, metricIndex, ifIndexByName);
            if (route != null) {
                routes.add(route);
            }
        }
        return routes;
    }

    private static @Nullable IPRoute parseBsdRow(String[] fields, boolean ipv6, int ifNameIndex, int metricIndex,
            Map<String, Integer> ifIndexByName) {
        // The flags column is a run of letters and the destination is an address. Together these identify a data row
        // and reject every banner, header and separator line the three verified platforms emit.
        if (!ParseUtil.isRouteFlags(fields[2])) {
            return null;
        }
        Pair<byte[], Integer> dest = ParseUtil.parseRouteDestination(fields[0], ipv6);
        byte[] destination = dest.getA();
        if (destination.length == 0) {
            return null;
        }
        String flags = fields[2];
        boolean isGateway = flags.indexOf('G') >= 0;
        boolean hostFlag = flags.indexOf('H') >= 0;
        int prefixLength = resolvePrefixLength(dest.getB(), hostFlag, destination.length);
        // Only a gateway route has a next hop. On a directly attached route this column holds the interface's own
        // address, a link-layer address, or a symbolic name such as "link#14", none of which is a gateway.
        byte[] gateway = isGateway ? parseAddress(fields[1], ipv6) : new byte[0];
        String interfaceName = readInterfaceName(fields, ifNameIndex);
        long metric = metricIndex >= 0 && metricIndex < fields.length
                ? ParseUtil.parseLongOrDefault(fields[metricIndex], -1L)
                : -1L;
        return new IPRoute(destination, prefixLength, gateway, interfaceName,
                indexOfInterface(interfaceName, ifIndexByName), metric, isGateway,
                hostFlag || prefixLength == destination.length * 8);
    }

    /**
     * Queries both address families from a Solaris {@code netstat -rnv} and returns their routes as one list.
     *
     * @param ipv4Command   the command listing the IPv4 table
     * @param ipv6Command   the command listing the IPv6 table
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed routes for both families
     */
    public static List<IPRoute> querySolarisRoutes(String ipv4Command, String ipv6Command,
            Map<String, Integer> ifIndexByName) {
        List<IPRoute> routes = new ArrayList<>(querySolarisRoutes(ipv4Command, false, ifIndexByName));
        routes.addAll(querySolarisRoutes(ipv6Command, true, ifIndexByName));
        return routes;
    }

    /**
     * Queries and parses a Solaris {@code netstat -rnv} routing table.
     *
     * @param command       the command to run, which must name a single address family
     * @param ipv6          whether the command selects the IPv6 table
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed routes
     */
    public static List<IPRoute> querySolarisRoutes(String command, boolean ipv6, Map<String, Integer> ifIndexByName) {
        return parseSolarisRoutes(ExecutingCommand.runNative(command), ipv6, ifIndexByName);
    }

    /**
     * Parses the Solaris {@code netstat -rnv} IRE table.
     * <p>
     * The verbose form is required because {@code netstat -rn -f inet} publishes no netmask at all, leaving the prefix
     * length permanently unknown. The verbose form adds a Mask column for IPv4, but reorders the remaining columns and
     * places the flags differently for the two address families:
     *
     * <pre>
     * IPv4: Destination Mask Gateway Device MTU Ref Flg Out In/Fwd
     * IPv6: Destination/Mask Gateway If MTU Ref Flags Out In/Fwd
     * </pre>
     *
     * The optional Device column may be empty, which whitespace splitting collapses, shifting every following token
     * left by one. The flags column is therefore located by scanning from the right, where the trailing columns are
     * always numeric, rather than by a fixed index.
     *
     * @param netstat       the lines of {@code netstat -rnv} output for one address family
     * @param ipv6          whether this is the IPv6 table
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed routes
     */
    static List<IPRoute> parseSolarisRoutes(List<String> netstat, boolean ipv6, Map<String, Integer> ifIndexByName) {
        // IPv4 carries a Mask column between the destination and the gateway; IPv6 folds the prefix into the
        // destination and has none.
        int gatewayIndex = ipv6 ? 1 : 2;
        int deviceIndex = gatewayIndex + 1;
        List<IPRoute> routes = new ArrayList<>();
        for (String line : netstat) {
            String[] fields = ParseUtil.whitespaces.split(line.trim(), -1);
            if (fields.length < deviceIndex + 1) {
                continue;
            }
            int flagsIndex = findFlagsFromRight(fields, deviceIndex);
            if (flagsIndex < 0) {
                continue;
            }
            Pair<byte[], Integer> dest = ParseUtil.parseRouteDestination(fields[0], ipv6);
            byte[] destination = dest.getA();
            if (destination.length == 0) {
                continue;
            }
            String flags = fields[flagsIndex];
            boolean isGateway = flags.indexOf('G') >= 0;
            boolean hostFlag = flags.indexOf('H') >= 0;
            int prefixLength = dest.getB();
            if (prefixLength < 0 && !ipv6) {
                prefixLength = ParseUtil.netmaskToPrefixLength(fields[1]);
            }
            prefixLength = resolvePrefixLength(prefixLength, hostFlag, destination.length);
            byte[] gateway = isGateway ? parseAddress(fields[gatewayIndex], ipv6) : new byte[0];
            // With every column present the flags sit three past the device. Anything less means the optional device
            // column was empty and collapsed under whitespace splitting, so there is no interface to report. Do not
            // derive the device index from the flags index arithmetically: on a collapsed row that lands on the
            // gateway.
            String interfaceName = flagsIndex == deviceIndex + 3 ? readInterfaceName(fields, deviceIndex) : "";
            routes.add(new IPRoute(destination, prefixLength, gateway, interfaceName,
                    indexOfInterface(interfaceName, ifIndexByName), -1L, isGateway,
                    hostFlag || prefixLength == destination.length * 8));
        }
        return routes;
    }

    /**
     * Locates the flags column by scanning right to left. Every column to its right is numeric, so the first token of
     * letters found is the flags field.
     */
    private static int findFlagsFromRight(String[] fields, int floor) {
        for (int i = fields.length - 1; i >= floor; i--) {
            if (ParseUtil.isRouteFlags(fields[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Applies the H flag to a destination token that stated no prefix. A bare address in a routing table is a host
     * route, which covers the whole address.
     */
    private static int resolvePrefixLength(int statedPrefix, boolean hostFlag, int addressBytes) {
        if (statedPrefix >= 0) {
            return statedPrefix;
        }
        return hostFlag ? addressBytes * 8 : -1;
    }

    private static byte[] parseAddress(String token, boolean ipv6) {
        return ipv6 ? ParseUtil.parseIpv6AddressToBytes(token) : ParseUtil.parseIpv4AddressToBytes(token);
    }

    /**
     * Reads the interface name, rejecting anything not shaped like one. A wrong column index therefore degrades to an
     * empty name rather than reporting a reference count or an expiry time as an interface.
     */
    private static String readInterfaceName(String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return "";
        }
        String token = fields[index];
        return INTERFACE_NAME.matcher(token).matches() ? token : "";
    }

    private static int indexOfInterface(String name, Map<String, Integer> ifIndexByName) {
        if (name.isEmpty()) {
            return -1;
        }
        Integer index = ifIndexByName.get(name);
        return index == null ? -1 : index;
    }

    private static boolean isColumnHeader(String firstToken) {
        return "Destination".equals(firstToken) || "Destination/Mask".equals(firstToken);
    }

    private static int indexOfAny(String[] fields, String... candidates) {
        for (int i = 0; i < fields.length; i++) {
            for (String candidate : candidates) {
                if (candidate.equals(fields[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
}
