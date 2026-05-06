/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final long CACHE_TTL_MS = 1000L;

    private final Supplier<List<NetworkIF>> networkIFSupplier;
    private final InternetProtocolStats ipStats;

    // Strong reference to prevent GC of NetworkIF objects used by FunctionCounter
    private List<NetworkIF> networkIFs;

    // Connection count cache to avoid repeated getConnections() calls per scrape
    private volatile long cacheTimestamp;
    private volatile Map<TcpState, Long> tcpCounts = new EnumMap<>(TcpState.class);
    private volatile long udpCount;

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
        this.networkIFs = networkIFSupplier.get();

        for (NetworkIF net : networkIFs) {
            String device = net.getName();

            // system.network.io — Counter, unit "By", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_IO, net, n -> {
                n.updateAttributes();
                return n.getBytesRecv();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "receive").description("Network bytes transferred")
                    .baseUnit("By").register(registry);
            FunctionCounter.builder(NET_IO, net, n -> {
                n.updateAttributes();
                return n.getBytesSent();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "transmit").description("Network bytes transferred")
                    .baseUnit("By").register(registry);

            // system.network.packet.count — Counter, unit "{packet}", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_PACKETS, net, n -> {
                n.updateAttributes();
                return n.getPacketsRecv();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "receive").description("Network packets transferred")
                    .baseUnit("{packet}").register(registry);
            FunctionCounter.builder(NET_PACKETS, net, n -> {
                n.updateAttributes();
                return n.getPacketsSent();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "transmit").description("Network packets transferred")
                    .baseUnit("{packet}").register(registry);

            // system.network.packet.dropped — Counter, unit "{packet}", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_DROPPED, net, n -> {
                n.updateAttributes();
                return n.getInDrops();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "receive").description("Count of packets dropped")
                    .baseUnit("{packet}").register(registry);

            // system.network.errors — Counter, unit "{error}", attrs: network.io.direction, system.device
            FunctionCounter.builder(NET_ERRORS, net, n -> {
                n.updateAttributes();
                return n.getInErrors();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "receive").description("Network errors").baseUnit("{error}")
                    .register(registry);
            FunctionCounter.builder(NET_ERRORS, net, n -> {
                n.updateAttributes();
                return n.getOutErrors();
            }).tag(DEVICE_KEY, device).tag(DIRECTION_KEY, "transmit").description("Network errors").baseUnit("{error}")
                    .register(registry);
        }

        // system.network.connection.count — UpDownCounter (Gauge), unit "{connection}",
        // attrs: network.transport, network.connection.state
        registerConnectionCountGauges(registry);
    }

    private void registerConnectionCountGauges(MeterRegistry registry) {
        for (TcpState state : TcpState.values()) {
            if (state == TcpState.NONE) {
                continue;
            }
            String stateValue = state.name().toLowerCase(Locale.ROOT);
            Gauge.builder(NET_CONNECTIONS, this, self -> self.getCachedTcpCount(state)).tag(TRANSPORT_KEY, "tcp")
                    .tag(STATE_KEY, stateValue).description("Total number of connections in each state")
                    .baseUnit("{connection}").strongReference(true).register(registry);
        }
        Gauge.builder(NET_CONNECTIONS, this, self -> self.getCachedUdpCount()).tag(TRANSPORT_KEY, "udp")
                .description("Total number of UDP connections").baseUnit("{connection}").strongReference(true)
                .register(registry);
    }

    private void refreshCache() {
        if (System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS) {
            synchronized (this) {
                long now = System.currentTimeMillis();
                if (now - cacheTimestamp > CACHE_TTL_MS) {
                    List<IPConnection> connections = ipStats.getConnections();
                    Map<TcpState, Long> tcp = new EnumMap<>(TcpState.class);
                    long udp = 0;
                    for (IPConnection conn : connections) {
                        if (conn.getType().startsWith("tcp")) {
                            tcp.merge(conn.getState(), 1L, Long::sum);
                        } else if (conn.getType().startsWith("udp")) {
                            udp++;
                        }
                    }
                    this.tcpCounts = tcp;
                    this.udpCount = udp;
                    this.cacheTimestamp = now;
                }
            }
        }
    }

    private double getCachedTcpCount(TcpState state) {
        refreshCache();
        return tcpCounts.getOrDefault(state, 0L);
    }

    private double getCachedUdpCount() {
        refreshCache();
        return udpCount;
    }
}
