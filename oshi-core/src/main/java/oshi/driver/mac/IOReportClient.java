/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;

import oshi.hardware.GpuTicks;
import oshi.jna.platform.mac.IOReport;
import oshi.jna.platform.mac.IOReport.IOReportSubscriptionRef;

/**
 * Manages a single IOReport subscription for GPU Stats and Energy Model channels, providing per-instance sampling of
 * GPU active ticks, utilization, and power draw.
 *
 * <p>
 * Each instance holds its own subscription and previous-sample state, making it suitable for use inside a
 * {@link oshi.hardware.GpuStats} session with explicit lifecycle management.
 *
 * <p>
 * Call {@link #close()} when done to release all CoreFoundation references. After {@code close()}, all sampling methods
 * return sentinel values.
 */
public final class IOReportClient {

    private static final String GROUP_GPU_STATS = "GPU Stats";
    private static final String GROUP_ENERGY = "Energy Model";
    private static final String CHANNEL_GPU_ENERGY = "GPU Energy";
    private static final String SUBGROUP_GPU_PERF_STATES = "GPU Performance States";
    private static final String STATE_OFF = "OFF";
    private static final String KEY_CHANNELS = "IOReportChannels";

    private final IOReport ioReport;
    private final IOReportSubscriptionRef subscription;
    private final CFDictionaryRef subscribedChannels;

    // Previous sample for utilization delta
    private CFDictionaryRef prevSampleUtil;

    // Previous sample and timestamp for power delta
    private CFDictionaryRef prevSamplePower;
    private long prevSamplePowerNanos;

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
    public static IOReportClient create() {
        IOReport io;
        try {
            io = Native.load("IOReport", IOReport.class);
        } catch (UnsatisfiedLinkError e) {
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
            PointerByReference subRef = new PointerByReference();
            IOReportSubscriptionRef sub = io.IOReportCreateSubscription(null, gpuChannels, subRef, 0, null);
            if (sub == null) {
                return null;
            }
            Pointer subPtr = subRef.getValue();
            if (subPtr == null) {
                sub.release();
                return null;
            }
            return new IOReportClient(io, sub, new CFDictionaryRef(subPtr));
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
     * Returns a {@link GpuTicks} snapshot of cumulative GPU active and idle ticks in raw IOReport residency units. The
     * kernel residency counters are monotonically increasing; callers diff two snapshots to compute utilization:
     * {@code dActive / (dActive + dIdle)}.
     *
     * @return GpuTicks snapshot; never null
     */
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
                ChannelStates cs = extractChannelStates(sample, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
                if (cs.getStates().isEmpty()) {
                    return new GpuTicks(0L, 0L);
                }
                long idle = cs.getStates().getOrDefault(STATE_OFF, 0L);
                long total = cs.getStates().values().stream().mapToLong(Long::longValue).sum();
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
                ChannelStates cs = extractChannelStates(delta, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
                if (cs.getStates().isEmpty()) {
                    return -1d;
                }
                long off = cs.getStates().getOrDefault(STATE_OFF, 0L);
                long total = cs.getStates().values().stream().mapToLong(Long::longValue).sum();
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
     * Releases all CoreFoundation references held by this client. Idempotent.
     */
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

    /** Holds the merged state-residency map and the number of IOReport channels that contributed to it. */
    private static final class ChannelStates {
        private final Map<String, Long> states;

        ChannelStates(Map<String, Long> states) {
            this.states = states;
        }

        Map<String, Long> getStates() {
            return states;
        }
    }

    private ChannelStates extractChannelStates(CFDictionaryRef dict, String group, String subgroup) {
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try {
            Pointer arrPtr = dict.getValue(channelsKey);
            if (arrPtr == null) {
                return new ChannelStates(Collections.emptyMap());
            }
            CFArrayRef arr = new CFArrayRef(arrPtr);
            int count = arr.getCount();
            Map<String, Long> result = new HashMap<>();
            for (int i = 0; i < count; i++) {
                Pointer entryPtr = arr.getValueAtIndex(i);
                if (entryPtr == null) {
                    continue;
                }
                CFDictionaryRef entry = new CFDictionaryRef(entryPtr);
                CFStringRef groupRef = ioReport.IOReportChannelGetGroup(entry);
                if (groupRef == null || !group.equals(groupRef.stringValue())) {
                    continue;
                }
                if (subgroup != null) {
                    CFStringRef subRef = ioReport.IOReportChannelGetSubGroup(entry);
                    if (subRef == null || !subgroup.equals(subRef.stringValue())) {
                        continue;
                    }
                }
                int stateCount = ioReport.IOReportStateGetCount(entry);
                for (int s = 0; s < stateCount; s++) {
                    CFStringRef nameRef = ioReport.IOReportStateGetNameForIndex(entry, s);
                    if (nameRef == null) {
                        continue;
                    }
                    String stateName = nameRef.stringValue();
                    long ticks = ioReport.IOReportStateGetResidency(entry, s);
                    if (!stateName.isEmpty()) {
                        result.merge(stateName, ticks, Long::sum);
                    }
                }
            }
            return new ChannelStates(result);
        } finally {
            channelsKey.release();
        }
    }
}
