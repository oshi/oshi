/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import oshi.hardware.NetworkIF;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpState;
import oshi.util.Memoizer;

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
 *
 * <p>
 * An interface's counters are re-read as its meters are sampled, once per interface per
 * {@link Memoizer#defaultExpiration()} window rather than once per meter, so that all of an interface's meters within a
 * scrape report the same reading. The connection counts are cached the same way, for one second.
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

    // Strong reference to prevent GC of the refreshing suppliers, and through them the NetworkIF objects they close
    // over, used by FunctionCounter (Micrometer holds them weakly). FunctionCounter has no strongReference() of its
    // own, as Gauge does, so this field is the only thing keeping the measured object reachable.
    @SuppressWarnings({ "java:S1068", "UnusedVariable" }) // deliberate GC root; must outlive bindTo(), never read
    private List<Supplier<NetworkIF>> refreshedNetworkIFs;

    // Connection count cache to avoid repeated getConnections() calls per scrape
    private volatile long cacheTimestamp;
    private final AtomicReference<Map<TcpState, Long>> tcpCounts = new AtomicReference<>(Collections.emptyMap());
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
        List<NetworkIF> nets = networkIFSupplier.get();
        List<Supplier<NetworkIF>> refreshing = new ArrayList<>(nets.size());

        for (NetworkIF net : nets) {
            String device = net.getName();
            // A bound NetworkIF holds the counters read when the binder was created, so it has to be refreshed as it
            // is sampled. The seven meters below read it through one memoized supplier, both to spare the interface six
            // redundant queries per scrape and so that a single scrape reads one snapshot: bytes, packets, drops and
            // errors are otherwise counted from different moments, and a drop or error rate computed against a byte
            // or packet count from another reading is not comparable.
            Supplier<NetworkIF> refreshed = Memoizer.memoize(() -> {
                net.updateAttributes();
                return net;
            }, Memoizer.defaultExpiration());
            refreshing.add(refreshed);

            // system.network.io — Counter, unit "By", attrs: network.io.direction, system.device
            registerNetCounter(registry, refreshed, device, "receive", NET_IO, "Network bytes transferred", "By",
                    NetworkIF::getBytesRecv);
            registerNetCounter(registry, refreshed, device, "transmit", NET_IO, "Network bytes transferred", "By",
                    NetworkIF::getBytesSent);

            // system.network.packet.count — Counter, unit "{packet}", attrs: network.io.direction, system.device
            registerNetCounter(registry, refreshed, device, "receive", NET_PACKETS, "Network packets transferred",
                    "{packet}", NetworkIF::getPacketsRecv);
            registerNetCounter(registry, refreshed, device, "transmit", NET_PACKETS, "Network packets transferred",
                    "{packet}", NetworkIF::getPacketsSent);

            // system.network.packet.dropped — Counter, unit "{packet}", attrs: network.io.direction, system.device
            registerNetCounter(registry, refreshed, device, "receive", NET_DROPPED, "Count of packets dropped",
                    "{packet}", NetworkIF::getInDrops);

            // system.network.errors — Counter, unit "{error}", attrs: network.io.direction, system.device
            registerNetCounter(registry, refreshed, device, "receive", NET_ERRORS, "Network errors", "{error}",
                    NetworkIF::getInErrors);
            registerNetCounter(registry, refreshed, device, "transmit", NET_ERRORS, "Network errors", "{error}",
                    NetworkIF::getOutErrors);
        }

        // Hold strong references to prevent GC (FunctionCounter uses WeakReference)
        this.refreshedNetworkIFs = refreshing;

        // system.network.connection.count — UpDownCounter (Gauge), unit "{connection}",
        // attrs: network.transport, network.connection.state
        registerConnectionCountGauges(registry);
    }

    private static void registerNetCounter(MeterRegistry registry, Supplier<NetworkIF> refreshed, String device,
            String direction, String name, String description, String baseUnit, ToDoubleFunction<NetworkIF> value) {
        FunctionCounter.builder(name, refreshed, s -> value.applyAsDouble(s.get())).tag(DEVICE_KEY, device)
                .tag(DIRECTION_KEY, direction).description(description).baseUnit(baseUnit).register(registry);
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
                    this.tcpCounts.set(Collections.unmodifiableMap(tcp));
                    this.udpCount = udp;
                    this.cacheTimestamp = now;
                }
            }
        }
    }

    private double getCachedTcpCount(TcpState state) {
        refreshCache();
        return tcpCounts.get().getOrDefault(state, 0L);
    }

    private double getCachedUdpCount() {
        refreshCache();
        return udpCount;
    }
}
