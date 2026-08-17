/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.ProcPath;

/**
 * Utility to read the routing table from {@code /proc/net/route} and {@code /proc/net/ipv6_route}.
 * <p>
 * The two files share no format. The IPv4 one has a header line, eleven columns, the interface first, and addresses as
 * eight little-endian hex digits with a separate mask column. The IPv6 one has no header, ten columns, the interface
 * last, addresses as thirty-two hex digits in network order, and the prefix length beside the destination.
 */
@ThreadSafe
public final class RouteTable {

    /** Route is a gateway route, {@code RTF_GATEWAY}. */
    private static final int RTF_GATEWAY = 0x0002;
    /** Route is to a single host, {@code RTF_HOST}. */
    private static final int RTF_HOST = 0x0004;

    private static final int IPV4_FIELD_COUNT = 11;
    private static final int IPV6_FIELD_COUNT = 10;

    private RouteTable() {
    }

    /**
     * Queries and parses {@code /proc/net/route}.
     *
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed IPv4 routes
     */
    public static List<IPRoute> queryIpv4Routes(Map<String, Integer> ifIndexByName) {
        return parseIpv4Routes(FileUtil.readFile(ProcPath.ROUTE), ifIndexByName);
    }

    /**
     * Parses {@code /proc/net/route} content.
     * <p>
     * Columns are {@code Iface Destination Gateway Flags RefCnt Use Metric Mask MTU Window IRTT}. Addresses are the
     * kernel's {@code %08X} of a {@code __be32}, which on a little-endian host is exactly what
     * {@link ParseUtil#parseIntToIP(int)} inverts.
     *
     * @param lines         the lines of {@code /proc/net/route}
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed IPv4 routes
     */
    static List<IPRoute> parseIpv4Routes(List<String> lines, Map<String, Integer> ifIndexByName) {
        List<IPRoute> routes = new ArrayList<>();
        for (String line : lines) {
            String[] fields = ParseUtil.whitespaces.split(line.trim(), -1);
            if (fields.length < IPV4_FIELD_COUNT) {
                continue;
            }
            // The header line's Destination column is the word "Destination", which is not eight hex digits. Testing
            // the shape rather than a parse result keeps a genuine 0xFFFFFFFF destination from being mistaken for a
            // parse failure.
            if (!isHexWord(fields[1], 8)) {
                continue;
            }
            byte[] destination = ParseUtil.parseIntToIP((int) ParseUtil.hexStringToLong(fields[1], 0L));
            int flags = ParseUtil.hexStringToInt(fields[3], 0);
            boolean isGateway = (flags & RTF_GATEWAY) != 0;
            int prefixLength = ParseUtil
                    .netmaskToPrefixLength(ParseUtil.parseIntToIP((int) ParseUtil.hexStringToLong(fields[7], 0L)));
            byte[] gateway = isGateway ? ParseUtil.parseIntToIP((int) ParseUtil.hexStringToLong(fields[2], 0L))
                    : new byte[0];
            String interfaceName = fields[0];
            routes.add(new IPRoute(destination, prefixLength, gateway, interfaceName,
                    indexOfInterface(interfaceName, ifIndexByName), ParseUtil.parseLongOrDefault(fields[6], -1L),
                    isGateway, (flags & RTF_HOST) != 0 || prefixLength == 32));
        }
        return routes;
    }

    /**
     * Queries and parses {@code /proc/net/ipv6_route}.
     *
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed IPv6 routes
     */
    public static List<IPRoute> queryIpv6Routes(Map<String, Integer> ifIndexByName) {
        return parseIpv6Routes(FileUtil.readFile(ProcPath.IPV6_ROUTE), ifIndexByName);
    }

    /**
     * Parses {@code /proc/net/ipv6_route} content.
     * <p>
     * There is no header line. Columns are {@code destination prefixLength source sourcePrefixLength nextHop metric
     * refCount use flags interface}, with addresses as thirty-two hex digits.
     *
     * @param lines         the lines of {@code /proc/net/ipv6_route}
     * @param ifIndexByName a map of interface name to index; an empty map reports every index as -1
     * @return the parsed IPv6 routes
     */
    static List<IPRoute> parseIpv6Routes(List<String> lines, Map<String, Integer> ifIndexByName) {
        List<IPRoute> routes = new ArrayList<>();
        for (String line : lines) {
            String[] fields = ParseUtil.whitespaces.split(line.trim(), -1);
            if (fields.length < IPV6_FIELD_COUNT) {
                continue;
            }
            if (!isHexWord(fields[0], 32)) {
                continue;
            }
            // The kernel prints these addresses one byte at a time, so they are already in network order and need
            // none of the per-word reversal that /proc/net/tcp6 requires.
            byte[] destination = ParseUtil.hexStringToByteArray(fields[0]);
            long flags = ParseUtil.hexStringToLong(fields[8], 0L);
            boolean isGateway = (flags & RTF_GATEWAY) != 0;
            int prefixLength = ParseUtil.hexStringToInt(fields[1], -1);
            byte[] nextHop = ParseUtil.hexStringToByteArray(fields[4]);
            byte[] gateway = isGateway && nextHop.length == 16 ? nextHop : new byte[0];
            String interfaceName = fields[9];
            // A /128 local route does not reliably carry RTF_HOST, so the prefix width is checked as well
            routes.add(new IPRoute(destination, prefixLength, gateway, interfaceName,
                    indexOfInterface(interfaceName, ifIndexByName), ParseUtil.hexStringToLong(fields[5], -1L),
                    isGateway, (flags & RTF_HOST) != 0 || prefixLength == 128));
        }
        return routes;
    }

    private static boolean isHexWord(String token, int length) {
        if (token.length() != length) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfInterface(String name, Map<String, Integer> ifIndexByName) {
        if (name.isEmpty()) {
            return -1;
        }
        Integer index = ifIndexByName.get(name);
        return index == null ? -1 : index;
    }
}
