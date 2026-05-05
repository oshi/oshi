/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import oshi.hardware.NetworkIF;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpState;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * {@link MeterBinder} for system network metrics following
 * <a href="https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#network-metrics">OpenTelemetry semantic
 * conventions</a>.
 *
 * <p>
 * Registers per network interface:
 * <ul>
 * <li>{@code system.network.io} — network bytes transferred by direction (transmit, receive)</li>
 * <li>{@code system.network.packet.count} — network packets by direction (transmit, receive)</li>
 * <li>{@code system.network.packet.dropped} — dropped packets (receive only; OSHI does not expose transmit drops)</li>
 * <li>{@code system.network.errors} — network errors by direction (transmit, receive)</li>
 * </ul>
 *
 * <p>
 * Registers aggregate:
 * <ul>
 * <li>{@code system.network.connection.count} — connection count by protocol and state</li>
 * </ul>
 */
public class NetworkMetrics implements MeterBinder {

    private static final String NET_IO = "system.network.io";
    private static final String NET_PACKETS = "system.network.packet.count";
    private static final String NET_DROPPED = "system.network.packet.dropped";
    private static final String NET_ERRORS = "system.network.errors";
    private static final String NET_CONNECTIONS = "system.network.connection.count";
    private static final String DEVICE_KEY = "system.device";
    private static final String DIRECTION_KEY = "network.io.direction";
    private static final String TRANSPORT_KEY = "network.transport";
    private static final String STATE_KEY = "network.connection.state";

    private final Supplier<List<NetworkIF>> networkIFSupplier;
    private final InternetProtocolStats ipStats;

    /**
     * Creates a new {@code NetworkMetrics} binder.
     *
     * @param networkIFSupplier supplier that returns the current list of {@link NetworkIF} instances
     * @param ipStats           the {@link InternetProtocolStats} instance for connection counting
     */
    public NetworkMetrics(Supplier<List<NetworkIF>> networkIFSupplier, InternetProtocolStats ipStats) {
        this.networkIFSupplier = networkIFSupplier;
        this.ipStats = ipStats;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (NetworkIF net : networkIFSupplier.get()) {
            String device = net.getName();

            // system.network.io — Counter, unit "By", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_IO, net, NetworkIF::getBytesRecv).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "receive").description("Network bytes transferred").baseUnit("By")
                    .register(registry);
            FunctionCounter.builder(NET_IO, net, NetworkIF::getBytesSent).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "transmit").description("Network bytes transferred").baseUnit("By")
                    .register(registry);

            // system.network.packet.count — Counter, unit "{packet}", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_PACKETS, net, NetworkIF::getPacketsRecv).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "receive").description("Network packets transferred").baseUnit("{packet}")
                    .register(registry);
            FunctionCounter.builder(NET_PACKETS, net, NetworkIF::getPacketsSent).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "transmit").description("Network packets transferred").baseUnit("{packet}")
                    .register(registry);

            // system.network.packet.dropped — Counter, unit "{packet}", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_DROPPED, net, NetworkIF::getInDrops).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "receive").description("Count of packets dropped").baseUnit("{packet}")
                    .register(registry);

            // system.network.errors — Counter, unit "{error}", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_ERRORS, net, NetworkIF::getInErrors).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "receive").description("Network errors").baseUnit("{error}").register(registry);
            FunctionCounter.builder(NET_ERRORS, net, NetworkIF::getOutErrors).tag(DEVICE_KEY, device)
                    .tag(DIRECTION_KEY, "transmit").description("Network errors").baseUnit("{error}")
                    .register(registry);
        }

        // system.network.connection.count — UpDownCounter (Gauge), unit "{connection}",
        // attrs: network.transport, network.connection.state
        registerConnectionCountGauges(registry);
    }

    private void registerConnectionCountGauges(MeterRegistry registry) {
        // Register gauges for tcp and udp with all known states
        for (TcpState state : TcpState.values()) {
            if (state == TcpState.NONE) {
                continue;
            }
            String stateValue = state.name().toLowerCase(Locale.ROOT);
            Gauge.builder(NET_CONNECTIONS, ipStats, ip -> countConnections(ip, "tcp", state)).tag(TRANSPORT_KEY, "tcp")
                    .tag(STATE_KEY, stateValue).description("Total number of connections in each state")
                    .baseUnit("{connection}").strongReference(true).register(registry);
        }
        // UDP has no state; register without state attribute using NONE
        Gauge.builder(NET_CONNECTIONS, ipStats, ip -> countConnections(ip, "udp", TcpState.NONE))
                .tag(TRANSPORT_KEY, "udp").description("Total number of UDP connections").baseUnit("{connection}")
                .strongReference(true).register(registry);
    }

    private static double countConnections(InternetProtocolStats ip, String transport, TcpState state) {
        List<IPConnection> connections = ip.getConnections();
        return connections.stream().filter(c -> c.getType().startsWith(transport))
                .filter(c -> state == TcpState.NONE || c.getState() == state).count();
    }
}
