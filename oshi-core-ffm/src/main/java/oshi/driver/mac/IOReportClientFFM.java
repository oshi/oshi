/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static oshi.util.ExceptionUtil.runSilently;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import oshi.ffm.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.mac.IOReportFunctions;
import oshi.hardware.GpuTicks;

/**
 * FFM equivalent of {@code IOReportClient}: manages a single IOReport subscription for GPU Stats and Energy Model
 * channels, providing per-instance sampling of GPU active ticks, utilization, and power draw.
 *
 * <p>
 * Returns sentinel values ({@code (0,0)} / {@code -1.0}) when IOReport is unavailable.
 *
 * <p>
 * Call {@link #close()} when done to release all CoreFoundation references.
 */
public final class IOReportClientFFM {

    private static final String GROUP_GPU_STATS = "GPU Stats";
    private static final String GROUP_ENERGY = "Energy Model";
    private static final String CHANNEL_GPU_ENERGY = "GPU Energy";
    private static final String SUBGROUP_GPU_PERF_STATES = "GPU Performance States";
    private static final String STATE_OFF = "OFF";
    private static final String KEY_CHANNELS = "IOReportChannels";

    private final MemorySegment subscription;
    private final MemorySegment subscribedChannels;

    private MemorySegment prevSampleUtil;
    private MemorySegment prevSamplePower;
    private long prevSamplePowerNanos;

    private boolean closed;

    private IOReportClientFFM(MemorySegment subscription, MemorySegment subscribedChannels) {
        this.subscription = subscription;
        this.subscribedChannels = subscribedChannels;
    }

    /**
     * Creates a new {@code IOReportClientFFM} subscribed to GPU Stats and Energy Model channels.
     *
     * @return a new client, or {@code null} if IOReport is unavailable or subscription fails
     */
    public static IOReportClientFFM create() {
        if (!IOReportFunctions.isAvailable()) {
            return null;
        }
        CFStringRef gpuGroup = CFStringRef.createCFString(GROUP_GPU_STATS);
        CFStringRef energyGroup = CFStringRef.createCFString(GROUP_ENERGY);
        try (gpuGroup; energyGroup) {
            MemorySegment gpuChannels = IOReportFunctions.IOReportCopyChannelsInGroup(gpuGroup.segment(),
                    MemorySegment.NULL, 0, 0, 0);
            if (gpuChannels.equals(MemorySegment.NULL)) {
                return null;
            }
            MemorySegment energyChannels = IOReportFunctions.IOReportCopyChannelsInGroup(energyGroup.segment(),
                    MemorySegment.NULL, 0, 0, 0);
            try (CFTypeRef gpuRef = new CFTypeRef(gpuChannels); CFTypeRef energyRef = new CFTypeRef(energyChannels)) {
                if (!energyChannels.equals(MemorySegment.NULL)) {
                    IOReportFunctions.IOReportMergeChannels(gpuChannels, energyChannels, MemorySegment.NULL);
                }
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment subRefOut = arena.allocate(ValueLayout.ADDRESS);
                    MemorySegment sub = IOReportFunctions.IOReportCreateSubscription(MemorySegment.NULL, gpuChannels,
                            subRefOut, 0, MemorySegment.NULL);
                    if (sub.equals(MemorySegment.NULL)) {
                        return null;
                    }
                    MemorySegment subPtr = subRefOut.get(ValueLayout.ADDRESS, 0);
                    if (subPtr.equals(MemorySegment.NULL)) {
                        cfRelease(sub);
                        return null;
                    }
                    return new IOReportClientFFM(sub, subPtr);
                }
            }
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Returns a {@link GpuTicks} snapshot of cumulative GPU active and idle ticks.
     *
     * @return GpuTicks snapshot; never null
     */
    public synchronized GpuTicks sampleGpuTicks() {
        if (closed) {
            return new GpuTicks(0L, 0L);
        }
        try {
            MemorySegment sample = IOReportFunctions.IOReportCreateSamples(subscription, subscribedChannels,
                    MemorySegment.NULL);
            try (CFTypeRef sampleRef = new CFTypeRef(sample)) {
                if (sample.equals(MemorySegment.NULL)) {
                    return new GpuTicks(0L, 0L);
                }
                Map<String, Long> states = extractChannelStates(sample, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
                if (states.isEmpty()) {
                    return new GpuTicks(0L, 0L);
                }
                long idle = states.getOrDefault(STATE_OFF, 0L);
                long total = states.values().stream().mapToLong(Long::longValue).sum();
                return new GpuTicks(total - idle, idle);
            }
        } catch (Throwable e) {
            return new GpuTicks(0L, 0L);
        }
    }

    /**
     * Returns instantaneous GPU utilization as a percentage (0–100), or {@code -1.0} if unavailable or closed.
     *
     * @return GPU utilization percentage, or -1.0
     */
    public synchronized double sampleGpuUtilization() {
        if (closed) {
            return -1d;
        }
        MemorySegment sample = MemorySegment.NULL;
        try {
            sample = IOReportFunctions.IOReportCreateSamples(subscription, subscribedChannels, MemorySegment.NULL);
            if (sample.equals(MemorySegment.NULL)) {
                return -1d;
            }
            if (prevSampleUtil == null) {
                prevSampleUtil = sample;
                sample = MemorySegment.NULL;
                return -1d;
            }
            MemorySegment delta = IOReportFunctions.IOReportCreateSamplesDelta(prevSampleUtil, sample,
                    MemorySegment.NULL);
            cfRelease(prevSampleUtil);
            prevSampleUtil = sample;
            sample = MemorySegment.NULL;
            if (delta.equals(MemorySegment.NULL)) {
                return -1d;
            }
            try {
                Map<String, Long> states = extractChannelStates(delta, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
                if (states.isEmpty()) {
                    return -1d;
                }
                long off = states.getOrDefault(STATE_OFF, 0L);
                long total = states.values().stream().mapToLong(Long::longValue).sum();
                return total > 0 ? (total - off) * 100.0 / total : -1d;
            } finally {
                cfRelease(delta);
            }
        } catch (Throwable e) {
            return -1d;
        } finally {
            if (sample != null && !sample.equals(MemorySegment.NULL)) {
                cfRelease(sample);
            }
        }
    }

    /**
     * Returns instantaneous GPU power in watts, or {@code -1.0} if unavailable or closed.
     *
     * @return GPU power in watts, or -1.0
     */
    public synchronized double samplePowerWatts() {
        if (closed) {
            return -1d;
        }
        long beforeNanos = prevSamplePowerNanos;
        MemorySegment sample = MemorySegment.NULL;
        try {
            sample = IOReportFunctions.IOReportCreateSamples(subscription, subscribedChannels, MemorySegment.NULL);
            if (sample.equals(MemorySegment.NULL)) {
                return -1d;
            }
            if (prevSamplePower == null) {
                prevSamplePower = sample;
                prevSamplePowerNanos = System.nanoTime();
                sample = MemorySegment.NULL;
                return -1d;
            }
            long nowNanos = System.nanoTime();
            MemorySegment delta = IOReportFunctions.IOReportCreateSamplesDelta(prevSamplePower, sample,
                    MemorySegment.NULL);
            cfRelease(prevSamplePower);
            prevSamplePower = sample;
            prevSamplePowerNanos = nowNanos;
            sample = MemorySegment.NULL;
            if (delta.equals(MemorySegment.NULL)) {
                return -1d;
            }
            try {
                long dtNanos = nowNanos - beforeNanos;
                if (dtNanos <= 0) {
                    return -1d;
                }
                long energyUj = extractGpuEnergyMicrojoules(delta);
                if (energyUj < 0) {
                    return -1d;
                }
                return energyUj * 1000.0 / dtNanos;
            } finally {
                cfRelease(delta);
            }
        } catch (Throwable e) {
            return -1d;
        } finally {
            if (sample != null && !sample.equals(MemorySegment.NULL)) {
                cfRelease(sample);
            }
        }
    }

    /**
     * Releases all CoreFoundation references held by this client. Idempotent.
     */
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (prevSampleUtil != null) {
            cfRelease(prevSampleUtil);
            prevSampleUtil = null;
        }
        if (prevSamplePower != null) {
            cfRelease(prevSamplePower);
            prevSamplePower = null;
        }
        cfRelease(subscribedChannels);
        cfRelease(subscription);
    }

    private long extractGpuEnergyMicrojoules(MemorySegment delta) throws Throwable {
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try (channelsKey) {
            MemorySegment arrSeg = CFDictionaryRef.getValue(delta, channelsKey);
            if (arrSeg.equals(MemorySegment.NULL)) {
                return -1L;
            }
            int count = CFArrayRef.getCount(arrSeg);
            for (int i = 0; i < count; i++) {
                MemorySegment entrySeg = CFArrayRef.getValueAtIndex(arrSeg, i);
                if (entrySeg.equals(MemorySegment.NULL)) {
                    continue;
                }
                MemorySegment groupSeg = IOReportFunctions.IOReportChannelGetGroup(entrySeg);
                if (groupSeg.equals(MemorySegment.NULL) || !GROUP_ENERGY.equals(CFStringRef.stringValue(groupSeg))) {
                    continue;
                }
                MemorySegment nameSeg = IOReportFunctions.IOReportChannelGetChannelName(entrySeg);
                if (nameSeg.equals(MemorySegment.NULL)
                        || !CHANNEL_GPU_ENERGY.equals(CFStringRef.stringValue(nameSeg))) {
                    continue;
                }
                return IOReportFunctions.IOReportSimpleGetIntegerValue(entrySeg, 0);
            }
        }
        return -1L;
    }

    private Map<String, Long> extractChannelStates(MemorySegment dict, String group, String subgroup) throws Throwable {
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try (channelsKey) {
            MemorySegment arrSeg = CFDictionaryRef.getValue(dict, channelsKey);
            if (arrSeg.equals(MemorySegment.NULL)) {
                return Collections.emptyMap();
            }
            int count = CFArrayRef.getCount(arrSeg);
            Map<String, Long> result = new HashMap<>();
            for (int i = 0; i < count; i++) {
                MemorySegment entrySeg = CFArrayRef.getValueAtIndex(arrSeg, i);
                if (entrySeg.equals(MemorySegment.NULL)) {
                    continue;
                }
                MemorySegment groupSeg = IOReportFunctions.IOReportChannelGetGroup(entrySeg);
                if (groupSeg.equals(MemorySegment.NULL) || !group.equals(CFStringRef.stringValue(groupSeg))) {
                    continue;
                }
                if (subgroup != null) {
                    MemorySegment subSeg = IOReportFunctions.IOReportChannelGetSubGroup(entrySeg);
                    if (subSeg.equals(MemorySegment.NULL) || !subgroup.equals(CFStringRef.stringValue(subSeg))) {
                        continue;
                    }
                }
                int stateCount = IOReportFunctions.IOReportStateGetCount(entrySeg);
                for (int s = 0; s < stateCount; s++) {
                    MemorySegment nameSeg = IOReportFunctions.IOReportStateGetNameForIndex(entrySeg, s);
                    if (nameSeg.equals(MemorySegment.NULL)) {
                        continue;
                    }
                    String stateName = CFStringRef.stringValue(nameSeg);
                    long ticks = IOReportFunctions.IOReportStateGetResidency(entrySeg, s);
                    if (!stateName.isEmpty()) {
                        result.merge(stateName, ticks, Long::sum);
                    }
                }
            }
            return result;
        }
    }

    private static void cfRelease(MemorySegment seg) {
        if (seg != null && !seg.equals(MemorySegment.NULL)) {
            runSilently(() -> oshi.ffm.mac.CoreFoundationFunctions.CFRelease(seg));
        }
    }
}
