/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;

/**
 * Common logic for a macOS {@link GpuStats} session.
 *
 * <p>
 * On Apple Silicon, GPU ticks, utilization, and power are sourced from an {@link IOReportSampler} subscription.
 * Utilization falls back to IOAccelerator PerformanceStatistics when the subscription fails or returns a negative
 * value. Temperature is read from the SMC first, then falls back to IOAccelerator {@code Temperature(C)}.
 *
 * <p>
 * On Intel Mac, utilization and VRAM used are sourced from IOAccelerator PerformanceStatistics.
 *
 * <p>
 * Clock speeds, fan speed, and shared memory are not available on any macOS path and always return -1.
 *
 * <p>
 * Only the two native reads differ between the bindings, so subclasses implement {@link #queryPerfStats} and
 * {@link #queryGpuTemperatureFromSmc} and inherit everything else.
 */
@ThreadSafe
public abstract class MacGpuStats implements GpuStats {

    private static final Logger LOG = LoggerFactory.getLogger(MacGpuStats.class);

    // IOAccelerator PerformanceStatistics keys. Private because subclasses never name a key themselves: this class
    // decides which to ask for and passes them to queryPerfStats.
    private static final String GPU_CORE_UTIL_KEY = "GPU Core Utilization";
    private static final String DEVICE_UTIL_KEY = "Device Utilization %";
    private static final String VRAM_USED_KEY = "vramUsedBytes";
    private static final String VRAM_USED_KEY_AS = "In use system memory";
    private static final String TEMPERATURE_KEY = "Temperature(C)";

    private static final double GPU_UTIL_DIVISOR = 0xFFFFFFFFL;

    /**
     * Cards whose IOAccelerator statistics have no Temperature(C) key, so the lookup is not repeated. Static because a
     * GpuStats session is short-lived, and keyed by card because one card lacking the sensor says nothing about another
     * on the same machine.
     */
    private static final Set<String> PERF_STATS_TEMP_ABSENT = ConcurrentHashMap.newKeySet();

    private static final Pattern TRADEMARK_PATTERN = Pattern.compile("[®™]|\\([Rr]\\)|\\([Tt][Mm]\\)");

    private final boolean isAppleSilicon;
    private final String normCardName;
    private final Pattern cardNamePattern;

    /** Non-null only on Apple Silicon, where the IOReport GPU channels exist. */
    private final @Nullable IOReportSampler sampler;

    private boolean closed;

    /**
     * Creates a session for one card.
     *
     * @param isAppleSilicon whether this machine is Apple Silicon
     * @param cardName       the card's reported name, matched against IOAccelerator model names
     * @param samplerFactory creates the IOReport subscription, returning {@code null} if it could not be established.
     *                       It is consulted only on Apple Silicon: the IOReport GPU channels do not exist on Intel, so
     *                       subscribing there would attempt a subscription that can never yield a sample.
     */
    protected MacGpuStats(boolean isAppleSilicon, String cardName, Supplier<@Nullable IOReportSampler> samplerFactory) {
        this.isAppleSilicon = isAppleSilicon;
        this.normCardName = normalize(cardName);
        this.cardNamePattern = Pattern.compile("\\b" + Pattern.quote(normCardName) + "\\b");
        this.sampler = isAppleSilicon ? samplerFactory.get() : null;
        if (isAppleSilicon && sampler == null) {
            LOG.warn("IOReport subscription failed for '{}'; GPU ticks and power will be unavailable."
                    + " Utilization will fall back to IOAccelerator PerformanceStatistics.", cardName);
        }
    }

    /**
     * Reads IOAccelerator PerformanceStatistics values for this card, in a single registry walk.
     * <p>
     * Returning {@code null} rather than an empty map when the card's statistics cannot be read at all is load-bearing:
     * {@link #getTemperature()} latches a card as having no temperature sensor only when the dictionary was present and
     * the key was absent, never when the dictionary itself was missing, which can be transient.
     *
     * @param keys the keys to read
     * @return the requested keys that were present, or {@code null} if this card's statistics could not be read
     */
    protected abstract Map<String, Long> queryPerfStats(String... keys);

    /**
     * Reads the GPU temperature from the SMC. Called only on Apple Silicon.
     *
     * @return the temperature in degrees Celsius, or a value at or below zero if unavailable
     */
    protected abstract double queryGpuTemperatureFromSmc();

    /**
     * Tests whether an IOAccelerator model name refers to this card, ignoring case and trademark symbols.
     *
     * @param model the model name read from the registry
     * @return true if it names this card
     */
    protected final boolean matchesName(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        String normModel = normalize(model);
        return normModel.equals(normCardName) || cardNamePattern.matcher(normModel).find();
    }

    private static String normalize(String name) {
        return TRADEMARK_PATTERN.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("").trim();
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (sampler != null) {
            sampler.close();
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized GpuTicks getGpuTicks() {
        checkOpen();
        if (sampler != null) {
            return sampler.sampleGpuTicks();
        }
        return new GpuTicks(0L, 0L);
    }

    @Override
    public synchronized double getGpuUtilization() {
        checkOpen();
        if (sampler != null) {
            double util = sampler.sampleGpuUtilization();
            if (util >= 0) {
                return util;
            }
        }
        Map<String, Long> stats = queryPerfStats(GPU_CORE_UTIL_KEY, DEVICE_UTIL_KEY);
        if (stats == null) {
            return -1d;
        }
        Long coreUtil = stats.get(GPU_CORE_UTIL_KEY);
        if (coreUtil != null) {
            return coreUtil / GPU_UTIL_DIVISOR * 100.0;
        }
        Long deviceUtil = stats.get(DEVICE_UTIL_KEY);
        return deviceUtil == null ? -1d : deviceUtil;
    }

    @Override
    public synchronized long getVramUsed() {
        checkOpen();
        // Apple Silicon reports GPU memory as a share of unified system memory, so its key differs from the discrete
        // VRAM key an Intel Mac reports. Each is tried as the other's fallback.
        String primaryKey = isAppleSilicon ? VRAM_USED_KEY_AS : VRAM_USED_KEY;
        String fallbackKey = isAppleSilicon ? VRAM_USED_KEY : VRAM_USED_KEY_AS;
        Map<String, Long> stats = queryPerfStats(primaryKey, fallbackKey);
        if (stats == null) {
            return -1L;
        }
        Long used = stats.get(primaryKey);
        if (used == null) {
            used = stats.get(fallbackKey);
        }
        return used == null ? -1L : used;
    }

    @Override
    public synchronized long getSharedMemoryUsed() {
        checkOpen();
        return -1L;
    }

    @Override
    public synchronized double getTemperature() {
        checkOpen();
        if (isAppleSilicon) {
            double temp = queryGpuTemperatureFromSmc();
            if (temp > 0) {
                return temp;
            }
        }
        // IOAccelerator statistics, not the SMC: a different sensor that is not power-gated with the GPU cluster, so
        // the SMC plausibility floor deliberately does not apply here. Unavailable is -1, not 0. This key does not
        // exist on Apple Silicon, so it is tried once per card and then latched off rather than repeating an IOKit
        // registry walk on every call. Latch only on structural absence, not on a low reading.
        if (PERF_STATS_TEMP_ABSENT.contains(normCardName)) {
            return -1d;
        }
        Map<String, Long> stats = queryPerfStats(TEMPERATURE_KEY);
        if (stats == null) {
            // Not latched: a missing accelerator entry can be transient (driver reset, eGPU unplugged), unlike a
            // dictionary that is present but has no temperature key.
            return -1d;
        }
        Long temp = stats.get(TEMPERATURE_KEY);
        if (temp == null) {
            LOG.debug("No Temperature(C) in IOAccelerator statistics for {}; not retrying.", normCardName);
            PERF_STATS_TEMP_ABSENT.add(normCardName);
            return -1d;
        }
        return temp > 0 ? temp : -1d;
    }

    @Override
    public synchronized double getPowerDraw() {
        checkOpen();
        if (sampler != null) {
            return sampler.samplePowerWatts();
        }
        return -1d;
    }

    @Override
    public synchronized long getCoreClockMhz() {
        checkOpen();
        return -1L;
    }

    @Override
    public synchronized long getMemoryClockMhz() {
        checkOpen();
        return -1L;
    }

    @Override
    public synchronized double getFanSpeedPercent() {
        checkOpen();
        return -1d;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "GpuStats session has been closed. Obtain a new session via GraphicsCard.createStatsSession().");
        }
    }
}
