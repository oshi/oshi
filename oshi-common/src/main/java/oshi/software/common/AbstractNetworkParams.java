/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.NetworkParams;
import oshi.util.ExceptionUtil;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Common NetworkParams implementation.
 */
@ThreadSafe
public abstract class AbstractNetworkParams implements NetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworkParams.class);

    private static final Pattern SERVER_VALUE_DELIM = Pattern.compile("[ \t#;]");

    /**
     * Default constructor.
     */
    protected AbstractNetworkParams() {
    }

    private static final String NAMESERVER = "nameserver";

    private final Supplier<@Nullable InetAddress> localHost = memoize(this::queryLocalHost, defaultExpiration());

    @Override
    public String getDomainName() {
        InetAddress addr = this.localHost.get();
        return addr == null ? "" : addr.getCanonicalHostName();
    }

    @Override
    public String getHostName() {
        InetAddress addr = this.localHost.get();
        if (addr == null) {
            return "";
        }
        String hn = addr.getHostName();
        int dot = hn.indexOf('.');
        if (dot == -1) {
            return hn;
        }
        return hn.substring(0, dot);
    }

    /**
     * Resolves the local host, the source for both the host name and the domain name when the platform has no better
     * one. The result is memoized, because the JDK does not cache a failed lookup and both names would otherwise pay a
     * full failing DNS round trip apiece.
     *
     * @return the local host, or {@code null} if it does not resolve, in which case both names report the empty-string
     *         sentinel
     */
    protected @Nullable InetAddress queryLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // Deliberately not falling back to InetAddress.getLoopbackAddress(): its name and canonical name are
            // "localhost", which is a fabricated answer for a host whose own name does not resolve.
            LOG.debug("Unknown host exception when getting address of local host", e);
            return null;
        }
    }

    @Override
    public String[] getDnsServers() {
        List<String> resolv = FileUtil.readFile("/etc/resolv.conf");
        String key = NAMESERVER;
        int maxNameServer = 3;
        List<String> servers = new ArrayList<>();
        for (int i = 0; i < resolv.size() && servers.size() < maxNameServer; i++) {
            String line = resolv.get(i);
            if (line.startsWith(key)) {
                String value = ParseUtil.trimLeadingWhitespace(line.substring(key.length()));
                if (!value.isEmpty() && value.charAt(0) != '#' && value.charAt(0) != ';') {
                    String val = SERVER_VALUE_DELIM.split(value, 2)[0];
                    servers.add(val);
                }
            }
        }
        return servers.toArray(new String[0]);
    }

    /**
     * Convenience method to parse the output of the `route` command. While the command arguments vary between OS's the
     * output is consistently parsable.
     *
     * @param lines output of OS-specific route command
     * @return default gateway
     */
    protected static String searchGateway(List<String> lines) {
        for (String line : lines) {
            String leftTrimmed = ParseUtil.trimLeadingWhitespace(line);
            if (leftTrimmed.startsWith("gateway:")) {
                String[] split = ParseUtil.whitespaces.split(leftTrimmed, -1);
                if (split.length < 2) {
                    return "";
                }
                return split[1].split("%", -1)[0];
            }
        }
        return "";
    }

    /**
     * Maps interface names to their indices, for the platforms whose routing table publishes only a name.
     * <p>
     * Enumerating the interfaces is not free, so callers should invoke this once per routing table read rather than
     * once per route.
     *
     * @return a map of interface name to index, empty if the interfaces could not be enumerated
     */
    protected static Map<String, Integer> queryInterfaceIndexByName() {
        Map<String, Integer> map = new HashMap<>();
        for (NetworkInterface netIf : queryNetworkInterfaces()) {
            map.put(netIf.getName(), netIf.getIndex());
        }
        return map;
    }

    /**
     * Maps interface indices to their names, for the platforms whose routing table publishes only an index.
     * <p>
     * Enumerating the interfaces is not free, so callers should invoke this once per routing table read rather than
     * once per route.
     *
     * @return a map of interface index to name, empty if the interfaces could not be enumerated
     */
    protected static Map<Integer, String> queryInterfaceNameByIndex() {
        Map<Integer, String> map = new HashMap<>();
        for (NetworkInterface netIf : queryNetworkInterfaces()) {
            map.put(netIf.getIndex(), netIf.getName());
        }
        return map;
    }

    private static List<NetworkInterface> queryNetworkInterfaces() {
        return ExceptionUtil.getOrDefault(() -> {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            return interfaces == null ? Collections.<NetworkInterface>emptyList() : Collections.list(interfaces);
        }, Collections.<NetworkInterface>emptyList(), LOG, "Socket exception when retrieving interfaces");
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "Host name: %s, Domain name: %s, DNS servers: %s, IPv4 Gateway: %s, IPv6 Gateway: %s",
                this.getHostName(), this.getDomainName(), Arrays.toString(this.getDnsServers()),
                this.getIpv4DefaultGateway(), this.getIpv6DefaultGateway());

    }
}
