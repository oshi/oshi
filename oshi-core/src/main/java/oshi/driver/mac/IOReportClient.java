/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.GpuTicks;
import oshi.hardware.common.platform.mac.IOReportCpuSampler;
import oshi.hardware.common.platform.mac.IOReportSampler;
import oshi.jna.platform.mac.IOReport;
import oshi.jna.platform.mac.IOReport.IOReportSubscriptionRef;

/**
 * Manages a single IOReport subscription, providing per-instance sampling of the channels it subscribed to.
 * {@link #create()} subscribes to the GPU Stats and Energy Model channels, for GPU active ticks, utilization and power
 * draw; {@link #createForCpu()} subscribes to the CPU core performance state channels, for per-core frequency
 * residency. Sampling a channel this instance did not subscribe to returns a sentinel value.
 *
 * <p>
 * Each instance holds its own subscription and previous-sample state, making it suitable for use inside a
 * {@link oshi.hardware.GpuStats} session with explicit lifecycle management.
 *
 * <p>
 * Call {@link #close()} when done to release all CoreFoundation references. After {@code close()}, all sampling methods
 * return sentinel values.
 */
public final class IOReportClient implements IOReportSampler, IOReportCpuSampler {

    private static final String GROUP_GPU_STATS = "GPU Stats";
    private static final String GROUP_ENERGY = "Energy Model";
    private static final String CHANNEL_GPU_ENERGY = "GPU Energy";
    private static final String SUBGROUP_GPU_PERF_STATES = "GPU Performance States";
    private static final String GROUP_CPU_STATS = "CPU Stats";
    private static final String SUBGROUP_CPU_CORE_PERF_STATES = "CPU Core Performance States";
    private static final String STATE_OFF = "OFF";
    private static final String KEY_CHANNELS = "IOReportChannels";

    private final IOReport ioReport;
    private final IOReportSubscriptionRef subscription;
    private final CFDictionaryRef subscribedChannels;

    // Previous sample for utilization delta
    private @Nullable CFDictionaryRef prevSampleUtil;

    // Previous sample and timestamp for power delta
    private @Nullable CFDictionaryRef prevSamplePower;
    private long prevSamplePowerNanos;

    // Previous sample for the per-core residency delta
    private @Nullable CFDictionaryRef prevSampleCpu;

    private boolean closed;

    private IOReportClient(IOReport ioReport, IOReportSubscriptionRef subscription,
            CFDictionaryRef subscribedChannels) {
        this.ioReport = ioReport;
        this.subscription = subscription;
        this.subscribedChannels = subscribedChannels;
    }

    /**
     * Creates a new {@code IOReportClient} subscribed to GPU Stats and Energy Model channels.
     *
     * @return a new client, or {@code null} if IOReport is unavailable or subscription fails
     */
    public static @Nullable IOReportClient create() {
        IOReport io = loadIOReport();
        if (io == null) {
            return null;
        }
        CFStringRef gpuGroup = CFStringRef.createCFString(GROUP_GPU_STATS);
        CFStringRef energyGroup = CFStringRef.createCFString(GROUP_ENERGY);
        CFDictionaryRef gpuChannels = null;
        CFDictionaryRef energyChannels = null;
        try {
            gpuChannels = io.IOReportCopyChannelsInGroup(gpuGroup, null, 0, 0, 0);
            energyChannels = io.IOReportCopyChannelsInGroup(energyGroup, null, 0, 0, 0);
            if (gpuChannels == null) {
                return null;
            }
            if (energyChannels != null) {
                io.IOReportMergeChannels(gpuChannels, energyChannels, null);
            }
            return subscribe(io, gpuChannels);
        } catch (Exception e) {
            return null;
        } finally {
            gpuGroup.release();
            energyGroup.release();
            if (gpuChannels != null) {
                gpuChannels.release();
            }
            if (energyChannels != null) {
                energyChannels.release();
            }
        }
    }

    /**
     * Creates a new {@code IOReportClient} subscribed to the per-core CPU performance state channels.
     *
     * @return a new client, or {@code null} if IOReport is unavailable or subscription fails
     */
    public static @Nullable IOReportClient createForCpu() {
        IOReport io = loadIOReport();
        if (io == null) {
            return null;
        }
        CFStringRef cpuGroup = CFStringRef.createCFString(GROUP_CPU_STATS);
        CFStringRef coreSubgroup = CFStringRef.createCFString(SUBGROUP_CPU_CORE_PERF_STATES);
        CFDictionaryRef cpuChannels = null;
        try {
            cpuChannels = io.IOReportCopyChannelsInGroup(cpuGroup, coreSubgroup, 0, 0, 0);
            if (cpuChannels == null) {
                return null;
            }
            return subscribe(io, cpuChannels);
        } catch (Exception e) {
            return null;
        } finally {
            cpuGroup.release();
            coreSubgroup.release();
            if (cpuChannels != null) {
                cpuChannels.release();
            }
        }
    }

    private static @Nullable IOReport loadIOReport() {
        try {
            return Native.load("IOReport", IOReport.class);
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    private static @Nullable IOReportClient subscribe(IOReport io, CFDictionaryRef channels) {
        PointerByReference subRef = new PointerByReference();
        IOReportSubscriptionRef sub = io.IOReportCreateSubscription(null, channels, subRef, 0, null);
        if (sub == null) {
            return null;
        }
        Pointer subPtr = subRef.getValue();
        if (subPtr == null) {
            sub.release();
            return null;
        }
        return new IOReportClient(io, sub, new CFDictionaryRef(subPtr));
    }

    /**
     * Returns a {@link GpuTicks} snapshot of cumulative GPU active and idle ticks in raw IOReport residency units. The
     * kernel residency counters are monotonically increasing; callers diff two snapshots to compute utilization:
     * {@code dActive / (dActive + dIdle)}.
     *
     * @return GpuTicks snapshot; never null
     */
    @Override
    public synchronized GpuTicks sampleGpuTicks() {
        if (closed) {
            return new GpuTicks(0L, 0L);
        }
        CFDictionaryRef sample = null;
        try {
            sample = ioReport.IOReportCreateSamples(subscription, subscribedChannels, null);
            if (sample == null) {
                return new GpuTicks(0L, 0L);
            }
            try {
                Map<String, Long> states = extractChannelStates(sample, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
                if (states.isEmpty()) {
                    return new GpuTicks(0L, 0L);
                }
                long idle = states.getOrDefault(STATE_OFF, 0L);
                long total = states.values().stream().mapToLong(Long::longValue).sum();
                return new GpuTicks(total - idle, idle);
            } finally {
                sample.release();
                sample = null;
            }
        } catch (Exception e) {
            return new GpuTicks(0L, 0L);
        } finally {
            if (sample != null) {
                sample.release();
            }
        }
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
        CFDictionaryRef sample = null;
        try {
            sample = ioReport.IOReportCreateSamples(subscription, subscribedChannels, null);
            if (sample == null) {
                return -1d;
            }
            if (prevSampleUtil == null) {
                prevSampleUtil = sample;
                sample = null;
                return -1d;
            }
            CFDictionaryRef delta = ioReport.IOReportCreateSamplesDelta(prevSampleUtil, sample, null);
            prevSampleUtil.release();
            prevSampleUtil = sample;
            sample = null;
            if (delta == null) {
                return -1d;
            }
            try {
                Map<String, Long> states = extractChannelStates(delta, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
                if (states.isEmpty()) {
                    return -1d;
                }
                long off = states.getOrDefault(STATE_OFF, 0L);
                long total = states.values().stream().mapToLong(Long::longValue).sum();
                long active = total - off;
                return total > 0 ? active * 100.0 / total : -1d;
            } finally {
                delta.release();
            }
        } catch (Exception e) {
            return -1d;
        } finally {
            if (sample != null) {
                sample.release();
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
        CFDictionaryRef sample = null;
        try {
            sample = ioReport.IOReportCreateSamples(subscription, subscribedChannels, null);
            if (sample == null) {
                return -1d;
            }
            if (prevSamplePower == null) {
                prevSamplePower = sample;
                prevSamplePowerNanos = System.nanoTime();
                sample = null;
                return -1d;
            }
            long nowNanos = System.nanoTime();
            CFDictionaryRef delta = ioReport.IOReportCreateSamplesDelta(prevSamplePower, sample, null);
            prevSamplePower.release();
            prevSamplePower = sample;
            prevSamplePowerNanos = nowNanos;
            sample = null;
            if (delta == null) {
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
                // energyUj / dtNanos * 1e9 = watts; equivalently energyUj * 1000.0 / dtNanos
                // (µJ / ns = µJ / (µs * 1000) = W / 1000 * 1e6 / 1000 → energyUj * 1e9 / dtNanos W)
                return energyUj * 1000.0 / dtNanos;
            } finally {
                delta.release();
            }
        } catch (Exception e) {
            return -1d;
        } finally {
            if (sample != null) {
                sample.release();
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
        CFDictionaryRef sample = null;
        try {
            sample = ioReport.IOReportCreateSamples(subscription, subscribedChannels, null);
            if (sample == null) {
                return null;
            }
            if (prevSampleCpu == null) {
                prevSampleCpu = sample;
                sample = null;
                return null;
            }
            CFDictionaryRef delta = ioReport.IOReportCreateSamplesDelta(prevSampleCpu, sample, null);
            prevSampleCpu.release();
            prevSampleCpu = sample;
            sample = null;
            if (delta == null) {
                return null;
            }
            try {
                return extractCoreStates(delta);
            } finally {
                delta.release();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (sample != null) {
                sample.release();
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
            prevSampleUtil.release();
            prevSampleUtil = null;
        }
        if (prevSamplePower != null) {
            prevSamplePower.release();
            prevSamplePower = null;
        }
        if (prevSampleCpu != null) {
            prevSampleCpu.release();
            prevSampleCpu = null;
        }
        subscribedChannels.release();
        subscription.release();
    }

    private long extractGpuEnergyMicrojoules(CFDictionaryRef delta) {
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try {
            Pointer arrPtr = delta.getValue(channelsKey);
            if (arrPtr == null) {
                return -1L;
            }
            CFArrayRef arr = new CFArrayRef(arrPtr);
            int count = arr.getCount();
            for (int i = 0; i < count; i++) {
                Pointer entryPtr = arr.getValueAtIndex(i);
                if (entryPtr == null) {
                    continue;
                }
                CFDictionaryRef entry = new CFDictionaryRef(entryPtr);
                CFStringRef groupRef = ioReport.IOReportChannelGetGroup(entry);
                if (groupRef == null || !GROUP_ENERGY.equals(groupRef.stringValue())) {
                    continue;
                }
                CFStringRef nameRef = ioReport.IOReportChannelGetChannelName(entry);
                if (nameRef == null || !CHANNEL_GPU_ENERGY.equals(nameRef.stringValue())) {
                    continue;
                }
                return ioReport.IOReportSimpleGetIntegerValue(entry, 0);
            }
        } finally {
            channelsKey.release();
        }
        return -1L;
    }

    /** Sums the state residency of every channel in the group into one map. */
    private Map<String, Long> extractChannelStates(CFDictionaryRef dict, String group, String subgroup) {
        Map<String, Long> result = new HashMap<>();
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try {
            Pointer arrPtr = dict.getValue(channelsKey);
            if (arrPtr == null) {
                return Collections.emptyMap();
            }
            CFArrayRef arr = new CFArrayRef(arrPtr);
            int count = arr.getCount();
            for (int i = 0; i < count; i++) {
                CFDictionaryRef entry = channelInGroup(arr, i, group, subgroup);
                if (entry == null) {
                    continue;
                }
                for (Map.Entry<String, Long> state : readStates(entry).entrySet()) {
                    result.merge(state.getKey(), state.getValue(), Long::sum);
                }
            }
        } finally {
            channelsKey.release();
        }
        return Collections.unmodifiableMap(result);
    }

    /** Keeps each channel's state residency separate, keyed by channel name, for the per-core CPU states. */
    private Map<String, Map<String, Long>> extractCoreStates(CFDictionaryRef dict) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try {
            Pointer arrPtr = dict.getValue(channelsKey);
            if (arrPtr == null) {
                return Collections.emptyMap();
            }
            CFArrayRef arr = new CFArrayRef(arrPtr);
            int count = arr.getCount();
            for (int i = 0; i < count; i++) {
                CFDictionaryRef entry = channelInGroup(arr, i, GROUP_CPU_STATS, SUBGROUP_CPU_CORE_PERF_STATES);
                if (entry == null) {
                    continue;
                }
                CFStringRef nameRef = ioReport.IOReportChannelGetChannelName(entry);
                if (nameRef == null) {
                    continue;
                }
                String channelName = nameRef.stringValue();
                if (!channelName.isEmpty()) {
                    result.put(channelName, readStates(entry));
                }
            }
        } finally {
            channelsKey.release();
        }
        return Collections.unmodifiableMap(result);
    }

    /** Returns the channel at the array index if it belongs to the group and subgroup, otherwise {@code null}. */
    private @Nullable CFDictionaryRef channelInGroup(CFArrayRef channels, int index, String group,
            @Nullable String subgroup) {
        Pointer entryPtr = channels.getValueAtIndex(index);
        if (entryPtr == null) {
            return null;
        }
        CFDictionaryRef entry = new CFDictionaryRef(entryPtr);
        CFStringRef groupRef = ioReport.IOReportChannelGetGroup(entry);
        if (groupRef == null || !group.equals(groupRef.stringValue())) {
            return null;
        }
        if (subgroup != null) {
            CFStringRef subRef = ioReport.IOReportChannelGetSubGroup(entry);
            if (subRef == null || !subgroup.equals(subRef.stringValue())) {
                return null;
            }
        }
        return entry;
    }

    /** Reads one channel's residency ticks, in channel state order. */
    private Map<String, Long> readStates(CFDictionaryRef entry) {
        int stateCount = ioReport.IOReportStateGetCount(entry);
        Map<String, Long> states = new LinkedHashMap<>();
        for (int s = 0; s < stateCount; s++) {
            CFStringRef nameRef = ioReport.IOReportStateGetNameForIndex(entry, s);
            if (nameRef == null) {
                continue;
            }
            String stateName = nameRef.stringValue();
            if (!stateName.isEmpty()) {
                states.merge(stateName, ioReport.IOReportStateGetResidency(entry, s), Long::sum);
            }
        }
        return Collections.unmodifiableMap(states);
    }
}
