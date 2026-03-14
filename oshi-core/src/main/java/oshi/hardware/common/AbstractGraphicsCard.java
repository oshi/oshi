/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.Locale;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuStats;

/**
 * An abstract Graphics Card
 */
@Immutable
public abstract class AbstractGraphicsCard implements GraphicsCard {

    private final String name;
    private final String deviceId;
    private final String vendor;
    private final String versionInfo;
    private final long vram;

    /**
     * Constructor for AbstractGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    protected AbstractGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        this.name = name;
        this.deviceId = deviceId;
        this.vendor = vendor;
        this.versionInfo = versionInfo;
        this.vram = vram;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public String getVersionInfo() {
        return versionInfo;
    }

    @Override
    public long getVRam() {
        return vram;
    }

    @Override
    public GpuStats createStatsSession() {
        return new NoOpGpuStats();
    }

    /**
     * Returns a string representation of this graphics card including any available dynamic metrics.
     *
     * <p>
     * <strong>Performance note:</strong> this method opens a native {@link GpuStats} session via
     * {@link #createStatsSession()}, samples all available metrics, then closes the session. On platforms with native
     * GPU subscriptions (e.g. Apple Silicon via IOReport) this involves kernel-level IPC and should not be called in
     * hot paths. Cache the result if repeated string representations are needed.
     *
     * <p>
     * Delta-based metric backends ({@link GpuStats#getGpuUtilization()} and {@link GpuStats#getPowerDraw()}) may return
     * -1 on the first call after a session is opened while they record the initial baseline. Backends that read
     * instantaneous values (e.g. Linux sysfs and the macOS IOAccelerator fallback) can return a valid value on the
     * first call. Use a persistent {@link GpuStats} session with a polling loop for reliable delta values.
     *
     * @return a human-readable description of this graphics card
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GraphicsCard@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append(" [name=");
        builder.append(this.name);
        builder.append(", deviceId=");
        builder.append(this.deviceId);
        builder.append(", vendor=");
        builder.append(this.vendor);
        builder.append(", vRam=");
        builder.append(this.vram);
        GpuStats stats = null;
        try {
            stats = createStatsSession();
            long vramUsed = stats.getVramUsed();
            if (vramUsed >= 0) {
                builder.append(", vramUsed=");
                builder.append(vramUsed);
            }
            long sharedUsed = stats.getSharedMemoryUsed();
            if (sharedUsed >= 0) {
                builder.append(", sharedMemUsed=");
                builder.append(sharedUsed);
            }
            double utilization = stats.getGpuUtilization();
            if (utilization >= 0) {
                builder.append(", utilization=");
                builder.append(String.format(Locale.ROOT, "%.1f%%", utilization));
            }
            double temp = stats.getTemperature();
            if (temp >= 0) {
                builder.append(", temp=");
                builder.append(String.format(Locale.ROOT, "%.1f°C", temp));
            }
            double power = stats.getPowerDraw();
            if (power >= 0) {
                builder.append(", power=");
                builder.append(String.format(Locale.ROOT, "%.1fW", power));
            }
            long coreClock = stats.getCoreClockMhz();
            if (coreClock >= 0) {
                builder.append(", coreClock=");
                builder.append(coreClock);
                builder.append("MHz");
            }
            long memClock = stats.getMemoryClockMhz();
            if (memClock >= 0) {
                builder.append(", memClock=");
                builder.append(memClock);
                builder.append("MHz");
            }
            double fan = stats.getFanSpeedPercent();
            if (fan >= 0) {
                builder.append(", fan=");
                builder.append(String.format(Locale.ROOT, "%.1f%%", fan));
            }
        } catch (Exception e) {
            builder.append(", metricsUnavailable");
        } finally {
            if (stats != null) {
                stats.close();
            }
        }
        builder.append(", versionInfo=[");
        builder.append(this.versionInfo);
        builder.append("]]");
        return builder.toString();
    }
}
