/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.software.os.InternetProtocolStats;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests that {@link NetworkMetrics} reads an interface once per scrape rather than once per meter, which the live tests
 * in {@code OshiMetricsTest} cannot do: a real interface's counters may or may not move between samples.
 */
class NetworkMetricsTest {

    private static final String NET_IO = "system.network.io";
    private static final String NET_PACKETS = "system.network.packet.count";
    private static final String NET_DROPPED = "system.network.packet.dropped";
    private static final String NET_ERRORS = "system.network.errors";
    private static final String DIRECTION = "network.io.direction";

    private MeterRegistry registry;

    /**
     * An interface whose counters advance on every refresh, and which counts how often it was refreshed. Backed by a
     * real {@link NetworkInterface} because that is all {@link AbstractNetworkIF} needs; only the counters below, which
     * the fixture owns, are read by the meters under test.
     */
    private static class CountingNetworkIF extends AbstractNetworkIF {
        private int refreshes;

        CountingNetworkIF(NetworkInterface netint) throws InstantiationException {
            super(netint);
        }

        @Override
        public boolean updateAttributes() {
            this.refreshes++;
            // Advance every counter by the same amount, so any two of them read from one refresh agree
            long n = this.refreshes;
            this.bytesRecv = n;
            this.bytesSent = n;
            this.packetsRecv = n;
            this.packetsSent = n;
            this.inErrors = n;
            this.outErrors = n;
            this.inDrops = n;
            return true;
        }

        int refreshes() {
            return this.refreshes;
        }
    }

    /** Reports no connections, so the connection-count gauges register without needing a live query. */
    private static final class NoConnections implements InternetProtocolStats {
        @Override
        public TcpStats getTCPv4Stats() {
            throw new UnsupportedOperationException("not sampled by NetworkMetrics");
        }

        @Override
        public TcpStats getTCPv6Stats() {
            throw new UnsupportedOperationException("not sampled by NetworkMetrics");
        }

        @Override
        public UdpStats getUDPv4Stats() {
            throw new UnsupportedOperationException("not sampled by NetworkMetrics");
        }

        @Override
        public UdpStats getUDPv6Stats() {
            throw new UnsupportedOperationException("not sampled by NetworkMetrics");
        }

        @Override
        public List<IPConnection> getConnections() {
            return Collections.emptyList();
        }
    }

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    private static CountingNetworkIF loopbackFixture() throws SocketException, InstantiationException {
        NetworkInterface netint = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        assumeTrue(netint != null, "No loopback interface to back the fixture; skipping");
        return new CountingNetworkIF(netint);
    }

    private CountingNetworkIF bind() throws SocketException, InstantiationException {
        CountingNetworkIF net = loopbackFixture();
        new NetworkMetrics(() -> Collections.<NetworkIF>singletonList(net), new NoConnections()).bindTo(registry);
        return net;
    }

    private double counter(String name, String direction, String device) {
        FunctionCounter counter = registry.find(name).tag("system.device", device).tag(DIRECTION, direction)
                .functionCounter();
        assertNotNull(counter, name + "{" + DIRECTION + "=" + direction + "} should be registered");
        return counter.count();
    }

    @Test
    void allMetersOfOneInterfaceShareASingleReading() throws SocketException, InstantiationException {
        // Sampling seven meters used to query the interface seven times, so each meter's value came from a different
        // reading. They now share one memoized refresh per interface, which holds for 300 ms by default -- far longer
        // than sampling seven meters in process.
        CountingNetworkIF net = bind();
        String device = net.getName();

        double bytesRecv = counter(NET_IO, "receive", device);
        double bytesSent = counter(NET_IO, "transmit", device);
        double packetsRecv = counter(NET_PACKETS, "receive", device);
        double packetsSent = counter(NET_PACKETS, "transmit", device);
        double drops = counter(NET_DROPPED, "receive", device);
        double inErrors = counter(NET_ERRORS, "receive", device);
        double outErrors = counter(NET_ERRORS, "transmit", device);

        assertEquals(1, net.refreshes(), "One scrape should read the interface once, not once per meter");
        assertEquals(bytesRecv, bytesSent, "Bytes received and sent should come from the same reading");
        assertEquals(packetsRecv, packetsSent, "Packets received and sent should come from the same reading");
        assertEquals(bytesRecv, packetsRecv, "Bytes and packets should come from the same reading");
        assertEquals(bytesRecv, drops, "Drops and bytes should come from the same reading");
        assertEquals(inErrors, outErrors, "Errors in and out should come from the same reading");
        assertEquals(bytesRecv, inErrors, "Errors and bytes should come from the same reading");
    }

    @Test
    void bindingDoesNotReadTheInterface() throws SocketException, InstantiationException {
        // The refresh is memoized lazily, so binding an interface registers meters without querying it
        CountingNetworkIF net = bind();
        assertEquals(0, net.refreshes(), "Binding should not query the interface");
    }
}
