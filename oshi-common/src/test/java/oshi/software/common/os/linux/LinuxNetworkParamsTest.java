/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.ParseUtil;

class LinuxNetworkParamsTest {

    /** Concrete subclass; every method under test is inherited. */
    private static final class StubLinuxNetworkParams extends LinuxNetworkParams {
    }

    /**
     * The host name must be the kernel's, read here independently of {@code FileUtil} and {@code ProcPath}. A name
     * resolution based implementation fails this whenever the kernel host name is a FQDN, because it truncates at the
     * first dot, or whenever the host name does not resolve, because it reports {@code localhost}.
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    void testHostNameIsTheKernelHostName() throws IOException {
        byte[] raw = Files.readAllBytes(Paths.get("/proc/sys/kernel/hostname"));
        String kernelHostName = new String(raw, StandardCharsets.UTF_8).trim();
        assertThat("The kernel always has a host name", kernelHostName, is(not(emptyString())));
        assertThat(new StubLinuxNetworkParams().getHostName(), is(kernelHostName));
    }

    private static IPRoute route(String destination, int prefixLength, String gateway, long metric) {
        byte[] gw = gateway.isEmpty() ? new byte[0]
                : gateway.indexOf(':') >= 0 ? ParseUtil.parseIpv6AddressToBytes(gateway)
                        : ParseUtil.parseIpv4AddressToBytes(gateway);
        byte[] dest = destination.indexOf(':') >= 0 ? ParseUtil.parseIpv6AddressToBytes(destination)
                : ParseUtil.parseIpv4AddressToBytes(destination);
        return new IPRoute(dest, prefixLength, gw, "enc1", 2, metric, !gateway.isEmpty(), false);
    }

    /**
     * The routing table of an s390x host, which `ip route` reported as {@code default via 148.100.74.1}, plus a second
     * default route with a higher metric listed first, as a backup route via another interface would be.
     */
    @Test
    void testDefaultGatewayIpv4() {
        List<IPRoute> routes = List.of(route("0.0.0.0", 0, "10.0.0.1", 100), route("0.0.0.0", 0, "148.100.74.1", 0),
                route("148.100.74.0", 23, "", 0), route("169.254.169.254", 32, "148.100.74.5", 0));
        assertThat("The lowest-metric default route wins", LinuxNetworkParams.defaultGateway(routes),
                is("148.100.74.1"));
    }

    /**
     * The same host's IPv6 table: a link-local default gateway, which `ip -6 route` printed as
     * {@code default via fe80::4a2e:72ff:fe54:5e9f}, and the kernel's unreachable {@code ::/0} route on {@code lo},
     * which has no next hop.
     */
    @Test
    void testDefaultGatewayIpv6() {
        List<IPRoute> routes = List.of(route("2620:91:0:657::", 64, "", 256),
                route("::", 0, "fe80::4a2e:72ff:fe54:5e9f", 1024), route("::", 0, "", 4294967295L));
        assertThat(LinuxNetworkParams.defaultGateway(routes), is("fe80::4a2e:72ff:fe54:5e9f"));
    }

    @Test
    void testNoDefaultGateway() {
        assertThat(LinuxNetworkParams.defaultGateway(Collections.emptyList()), is(""));
        assertThat("A default route without a next hop is not a gateway",
                LinuxNetworkParams.defaultGateway(List.of(route("::", 0, "", 4294967295L))), is(""));
    }
}
