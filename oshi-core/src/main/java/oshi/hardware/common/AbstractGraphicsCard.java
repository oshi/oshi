/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;

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
        long vramUsed = getVramUsed();
        if (vramUsed >= 0) {
            builder.append(", vramUsed=");
            builder.append(vramUsed);
        }
        long sharedUsed = getSharedMemoryUsed();
        if (sharedUsed >= 0) {
            builder.append(", sharedMemUsed=");
            builder.append(sharedUsed);
        }
        double utilization = getGpuUtilization();
        if (utilization >= 0) {
            builder.append(", utilization=");
            builder.append(String.format(java.util.Locale.ROOT, "%.1f%%", utilization));
        }
        double temp = getTemperature();
        if (temp >= 0) {
            builder.append(", temp=");
            builder.append(String.format(java.util.Locale.ROOT, "%.1f°C", temp));
        }
        double power = getPowerDraw();
        if (power >= 0) {
            builder.append(", power=");
            builder.append(String.format(java.util.Locale.ROOT, "%.1fW", power));
        }
        long coreClock = getCoreClockMhz();
        if (coreClock >= 0) {
            builder.append(", coreClock=");
            builder.append(coreClock);
            builder.append("MHz");
        }
        long memClock = getMemoryClockMhz();
        if (memClock >= 0) {
            builder.append(", memClock=");
            builder.append(memClock);
            builder.append("MHz");
        }
        double fan = getFanSpeedPercent();
        if (fan >= 0) {
            builder.append(", fan=");
            builder.append(String.format(java.util.Locale.ROOT, "%.1f%%", fan));
        }
        builder.append(", versionInfo=[");
        builder.append(this.versionInfo);
        builder.append("]]");
        return builder.toString();
    }

    @Override
    public GpuTicks getGpuTicks() {
        return new DefaultGpuTicks(System.nanoTime() / 100L, 0L);
    }

    @Override
    public double getGpuUtilization() {
        return -1d;
    }

    @Override
    public long getVramUsed() {
        return -1L;
    }

    @Override
    public long getSharedMemoryUsed() {
        return -1L;
    }

    @Override
    public double getTemperature() {
        return -1d;
    }

    @Override
    public double getPowerDraw() {
        return -1d;
    }

    @Override
    public long getCoreClockMhz() {
        return -1L;
    }

    @Override
    public long getMemoryClockMhz() {
        return -1L;
    }

    @Override
    public double getFanSpeedPercent() {
        return -1d;
    }

    @Override
    public GpuStats createStatsSession() {
        return new NoOpGpuStats();
    }
}
