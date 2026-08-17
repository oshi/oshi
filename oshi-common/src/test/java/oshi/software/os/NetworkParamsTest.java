/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.NetworkParams.IPRoute;

/**
 * Tests the parts of {@link NetworkParams} a platform implementation does not supply: the {@code getRoutes()} default
 * body, which exists so that a third-party implementer of this {@code @PublicApi} interface keeps compiling, and the
 * {@link IPRoute} value semantics.
 */
class NetworkParamsTest {

    /** The minimum a third-party implementer has to write, implementing only the five abstract methods. */
    private static final class MinimalParams implements NetworkParams {
        @Override
        public String getHostName() {
            return "host";
        }

        @Override
        public String getDomainName() {
            return "example.com";
        }

        @Override
        public String[] getDnsServers() {
            return new String[0];
        }

        @Override
        public String getIpv4DefaultGateway() {
            return "";
        }

        @Override
        public String getIpv6DefaultGateway() {
            return "";
        }
    }

    @Test
    void testAnImplementationThatDoesNotOverrideRoutesReportsNone() {
        assertThat(new MinimalParams().getRoutes(), hasSize(0));
    }

    @Test
    void testIPRouteCopiesItsArraysInAndOut() {
        byte[] destination = { 10, 0, 0, 0 };
        byte[] gateway = { 10, 0, 0, 1 };
        IPRoute route = new IPRoute(destination, 24, gateway, "en0", 4, 100L, true, false);

        destination[0] = 127;
        gateway[0] = 127;
        assertThat("Mutating the caller's array must not change the route", route.getDestination(),
                is(new byte[] { 10, 0, 0, 0 }));
        assertThat(route.getGateway(), is(new byte[] { 10, 0, 0, 1 }));

        route.getDestination()[0] = 127;
        assertThat("Mutating a returned array must not change the route", route.getDestination(),
                is(new byte[] { 10, 0, 0, 0 }));
    }

    @Test
    void testIPRouteToStringRendersAddressesAndSentinels() {
        IPRoute gatewayRoute = new IPRoute(new byte[] { 0, 0, 0, 0 }, 0, new byte[] { 10, 0, 0, 1 }, "en0", 4, 100L,
                true, false);
        assertThat(gatewayRoute.toString(),
                is("IPRoute [destination=0.0.0.0/0, gateway=10.0.0.1, interfaceName=en0, interfaceIndex=4, "
                        + "metric=100, isGateway=true, isHost=false]"));

        // An absent gateway renders as a placeholder rather than an empty field
        IPRoute onLink = new IPRoute(new byte[] { 10, 0, 0, 1 }, 32, new byte[0], "", -1, -1L, false, true);
        assertThat(onLink.toString(),
                is("IPRoute [destination=10.0.0.1/32, gateway=*, interfaceName=, interfaceIndex=-1, "
                        + "metric=-1, isGateway=false, isHost=true]"));
    }
}
