/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static oshi.ffm.platform.mac.CoreFoundationFunctions.CFRelease;
import static oshi.util.ExceptionUtil.getOrDefault;
import static oshi.util.ExceptionUtil.runSilently;
import static oshi.util.LogLevel.TRACE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.ffm.platform.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.platform.mac.IOReportFunctions;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.platform.mac.IOReportCpuSampler;
import oshi.hardware.common.platform.mac.IOReportSampler;

/**
 * FFM equivalent of {@code IOReportClient}: manages a single IOReport subscription, providing per-instance sampling of
 * the channels it subscribed to. {@link #create()} subscribes to the GPU Stats and Energy Model channels, for GPU
 * active ticks, utilization and power draw; {@link #createForCpu()} subscribes to the CPU core performance state
 * channels, for per-core frequency residency. Sampling a channel this instance did not subscribe to returns a sentinel
 * value.
 *
 * <p>
 * Returns sentinel values ({@code (0,0)} / {@code -1.0}) when IOReport is unavailable.
 *
 * <p>
 * Call {@link #close()} when done to release all CoreFoundation references.
 */
public final class IOReportClientFFM implements IOReportSampler, IOReportCpuSampler {

    private static final Logger LOG = LoggerFactory.getLogger(IOReportClientFFM.class);

    private static final String GROUP_GPU_STATS = "GPU Stats";
    private static final String GROUP_ENERGY = "Energy Model";
    private static final String CHANNEL_GPU_ENERGY = "GPU Energy";
    private static final String SUBGROUP_GPU_PERF_STATES = "GPU Performance States";
    private static final String GROUP_CPU_STATS = "CPU Stats";
    private static final String SUBGROUP_CPU_CORE_PERF_STATES = "CPU Core Performance States";
    private static final String STATE_OFF = "OFF";
    private static final String KEY_CHANNELS = "IOReportChannels";

    private final MemorySegment subscription;
    private final MemorySegment subscribedChannels;

    private @Nullable MemorySegment prevSampleUtil;
    private @Nullable MemorySegment prevSamplePower;
    private long prevSamplePowerNanos;
    private @Nullable MemorySegment prevSampleCpu;

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
    public static @Nullable IOReportClientFFM create() {
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
            // wrapped only to release the native CF object on close
            try (var _ = new CFTypeRef(gpuChannels); var _ = new CFTypeRef(energyChannels)) {
                if (!energyChannels.equals(MemorySegment.NULL)) {
                    IOReportFunctions.IOReportMergeChannels(gpuChannels, energyChannels, MemorySegment.NULL);
                }
                return subscribe(gpuChannels);
            }
        } catch (Throwable _) {
            return null;
        }
    }

    /**
     * Creates a new {@code IOReportClientFFM} subscribed to the per-core CPU performance state channels.
     *
     * @return a new client, or {@code null} if IOReport is unavailable or subscription fails
     */
    public static @Nullable IOReportClientFFM createForCpu() {
        if (!IOReportFunctions.isAvailable()) {
            return null;
        }
        CFStringRef cpuGroup = CFStringRef.createCFString(GROUP_CPU_STATS);
        CFStringRef coreSubgroup = CFStringRef.createCFString(SUBGROUP_CPU_CORE_PERF_STATES);
        try (cpuGroup; coreSubgroup) {
            MemorySegment cpuChannels = IOReportFunctions.IOReportCopyChannelsInGroup(cpuGroup.segment(),
                    coreSubgroup.segment(), 0, 0, 0);
            if (cpuChannels.equals(MemorySegment.NULL)) {
                return null;
            }
            // wrapped only to release the native CF object on close
            try (var _ = new CFTypeRef(cpuChannels)) {
                return subscribe(cpuChannels);
            }
        } catch (Throwable _) {
            return null;
        }
    }

    private static @Nullable IOReportClientFFM subscribe(MemorySegment channels) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment subRefOut = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment sub = IOReportFunctions.IOReportCreateSubscription(MemorySegment.NULL, channels, subRefOut, 0,
                    MemorySegment.NULL);
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

    /**
     * Returns a {@link GpuTicks} snapshot of cumulative GPU active and idle ticks.
     *
     * @return GpuTicks snapshot; never null
     */
    @Override
    public synchronized GpuTicks sampleGpuTicks() {
        if (closed) {
            return new GpuTicks(0L, 0L);
        }
        return getOrDefault(() -> {
            MemorySegment sample = IOReportFunctions.IOReportCreateSamples(subscription, subscribedChannels,
                    MemorySegment.NULL);
            // wrapped only to release the native CF object on close
            try (var _ = new CFTypeRef(sample)) {
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
        }, new GpuTicks(0L, 0L), LOG, TRACE, "Failed to sample GPU ticks");
    }

    /**
     * Returns instantaneous GPU utilization as a percentage (0–100), or {@code -1.0} if unavailable or closed.
     *
     * @return GPU utilization percentage, or -1.0
     */
    @Override
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
        } catch (Throwable _) {
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
    @Override
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
        } catch (Throwable _) {
            return -1d;
        } finally {
            if (sample != null && !sample.equals(MemorySegment.NULL)) {
                cfRelease(sample);
            }
        }
    }

    /**
     * Returns the per-core CPU performance state residency accumulated since the previous call.
     *
     * @return a map from channel name to that core's state residency in channel state order, or {@code null} if this is
     *         the first call, the sample could not be taken, or this client is closed
     */
    @Override
    public synchronized @Nullable Map<String, Map<String, Long>> sampleCoreResidencyDelta() {
        if (closed) {
            return null;
        }
        MemorySegment sample = MemorySegment.NULL;
        try {
            sample = IOReportFunctions.IOReportCreateSamples(subscription, subscribedChannels, MemorySegment.NULL);
            if (sample.equals(MemorySegment.NULL)) {
                return null;
            }
            if (prevSampleCpu == null) {
                prevSampleCpu = sample;
                sample = MemorySegment.NULL;
                return null;
            }
            MemorySegment delta = IOReportFunctions.IOReportCreateSamplesDelta(prevSampleCpu, sample,
                    MemorySegment.NULL);
            cfRelease(prevSampleCpu);
            prevSampleCpu = sample;
            sample = MemorySegment.NULL;
            if (delta.equals(MemorySegment.NULL)) {
                return null;
            }
            try {
                return extractCoreStates(delta);
            } finally {
                cfRelease(delta);
            }
        } catch (Throwable _) {
            return null;
        } finally {
            if (sample != null && !sample.equals(MemorySegment.NULL)) {
                cfRelease(sample);
            }
        }
    }

    /**
     * Releases all CoreFoundation references held by this client. Idempotent.
     */
    @Override
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
        if (prevSampleCpu != null) {
            cfRelease(prevSampleCpu);
            prevSampleCpu = null;
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

    /** Sums the state residency of every channel in the group into one map. */
    private Map<String, Long> extractChannelStates(MemorySegment dict, String group, String subgroup) throws Throwable {
        Map<String, Long> result = new HashMap<>();
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try (channelsKey) {
            MemorySegment arrSeg = CFDictionaryRef.getValue(dict, channelsKey);
            if (arrSeg.equals(MemorySegment.NULL)) {
                return Collections.emptyMap();
            }
            int count = CFArrayRef.getCount(arrSeg);
            for (int i = 0; i < count; i++) {
                MemorySegment entrySeg = channelInGroup(arrSeg, i, group, subgroup);
                if (entrySeg == null) {
                    continue;
                }
                for (Map.Entry<String, Long> state : readStates(entrySeg).entrySet()) {
                    result.merge(state.getKey(), state.getValue(), Long::sum);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Keeps each channel's state residency separate, keyed by channel name, for the per-core CPU states. */
    private Map<String, Map<String, Long>> extractCoreStates(MemorySegment dict) throws Throwable {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try (channelsKey) {
            MemorySegment arrSeg = CFDictionaryRef.getValue(dict, channelsKey);
            if (arrSeg.equals(MemorySegment.NULL)) {
                return Collections.emptyMap();
            }
            int count = CFArrayRef.getCount(arrSeg);
            for (int i = 0; i < count; i++) {
                MemorySegment entrySeg = channelInGroup(arrSeg, i, GROUP_CPU_STATS, SUBGROUP_CPU_CORE_PERF_STATES);
                if (entrySeg == null) {
                    continue;
                }
                MemorySegment nameSeg = IOReportFunctions.IOReportChannelGetChannelName(entrySeg);
                if (nameSeg.equals(MemorySegment.NULL)) {
                    continue;
                }
                String channelName = CFStringRef.stringValue(nameSeg);
                if (!channelName.isEmpty()) {
                    result.put(channelName, readStates(entrySeg));
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Returns the channel at the array index if it belongs to the group and subgroup, otherwise {@code null}. */
    private @Nullable MemorySegment channelInGroup(MemorySegment channels, int index, String group,
            @Nullable String subgroup) throws Throwable {
        MemorySegment entrySeg = CFArrayRef.getValueAtIndex(channels, index);
        if (entrySeg.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment groupSeg = IOReportFunctions.IOReportChannelGetGroup(entrySeg);
        if (groupSeg.equals(MemorySegment.NULL) || !group.equals(CFStringRef.stringValue(groupSeg))) {
            return null;
        }
        if (subgroup != null) {
            MemorySegment subSeg = IOReportFunctions.IOReportChannelGetSubGroup(entrySeg);
            if (subSeg.equals(MemorySegment.NULL) || !subgroup.equals(CFStringRef.stringValue(subSeg))) {
                return null;
            }
        }
        return entrySeg;
    }

    /** Reads one channel's residency ticks, in channel state order. */
    private Map<String, Long> readStates(MemorySegment entrySeg) throws Throwable {
        int stateCount = IOReportFunctions.IOReportStateGetCount(entrySeg);
        Map<String, Long> states = new LinkedHashMap<>();
        for (int s = 0; s < stateCount; s++) {
            MemorySegment nameSeg = IOReportFunctions.IOReportStateGetNameForIndex(entrySeg, s);
            if (nameSeg.equals(MemorySegment.NULL)) {
                continue;
            }
            String stateName = CFStringRef.stringValue(nameSeg);
            if (!stateName.isEmpty()) {
                states.merge(stateName, IOReportFunctions.IOReportStateGetResidency(entrySeg, s), Long::sum);
            }
        }
        return Collections.unmodifiableMap(states);
    }

    private static void cfRelease(MemorySegment seg) {
        if (seg != null && !seg.equals(MemorySegment.NULL)) {
            runSilently(() -> CFRelease(seg));
        }
    }
}
