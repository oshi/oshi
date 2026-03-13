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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.jna.platform.mac.IOReport;
import oshi.jna.platform.mac.IOReport.IOReportSubscriptionRef;

/**
 * Queries Apple's private IOReport framework for GPU performance counters on Apple Silicon.
 *
 * <p>
 * Two channel groups are subscribed at initialisation time:
 * <ul>
 * <li>{@code "GPU Stats"} — per-state residency counters (active, idle, off, …) used to compute cumulative active ticks
 * for {@link GpuTicks}.</li>
 * <li>{@code "Energy Model"} — cumulative energy in microjoules used to derive instantaneous power in watts.</li>
 * </ul>
 *
 * <p>
 * Callers obtain a {@link GpuTicks} snapshot via {@link #sampleGpuTicks()} and instantaneous GPU power via
 * {@link #samplePowerWatts()}. Both methods are safe to call from any thread.
 */
@ThreadSafe
public final class IOReportDriver {

    private static IOReport ioReport;

    private static final String GROUP_GPU_STATS = "GPU Stats";
    private static final String GROUP_ENERGY = "Energy Model";
    // Channel name for GPU energy in the Energy Model group (values in microjoules)
    private static final String CHANNEL_GPU_ENERGY = "GPU Energy";
    // Subgroup name for GPU performance state residency
    private static final String SUBGROUP_GPU_PERF_STATES = "GPU Performance States";
    // Channel name for GPU performance state residency
    private static final String CHANNEL_GPUPH = "GPUPH";
    // State name for GPU off (non-active) residency
    private static final String STATE_OFF = "OFF";

    // IOReport dictionary key for the channel array
    private static final String KEY_CHANNELS = "IOReportChannels";

    // Subscription state — guarded by class lock
    private static IOReportSubscriptionRef subscription;
    private static CFDictionaryRef subscribedChannels;
    private static boolean initAttempted;

    // Previous sample for utilization delta — guarded by class lock
    private static CFDictionaryRef prevSampleUtil;

    // Previous sample and timestamp for power delta — guarded by class lock
    private static CFDictionaryRef prevSamplePower;
    private static long prevSamplePowerNanos;

    private IOReportDriver() {
    }

    /**
     * Returns a {@link GpuTicks} snapshot with cumulative GPU active ticks (in 100 ns units) and a matching monotonic
     * timestamp. Returns a zero-ticks snapshot if IOReport is unavailable.
     *
     * @return GpuTicks snapshot
     */
    public static synchronized GpuTicks sampleGpuTicks() {
        long timestamp = System.nanoTime() / 100L;
        if (!ensureInit()) {
            return new DefaultGpuTicks(timestamp, 0L);
        }
        CFDictionaryRef sample = null;
        try {
            sample = ioReport.IOReportCreateSamples(subscription, subscribedChannels, null);
            if (sample == null) {
                return new DefaultGpuTicks(timestamp, 0L);
            }
            Map<String, Long> states = extractChannelStates(sample, GROUP_GPU_STATS, SUBGROUP_GPU_PERF_STATES);
            long off = states.getOrDefault(STATE_OFF, 0L);
            long total = states.values().stream().mapToLong(Long::longValue).sum();
            long active = total - off;
            return new DefaultGpuTicks(timestamp, active > 0 ? active : 0L);
        } catch (Exception e) {
            return new DefaultGpuTicks(timestamp, 0L);
        } finally {
            if (sample != null) {
                sample.release();
            }
        }
    }

    /**
     * Returns instantaneous GPU utilization as a percentage (0–100), derived from the GPU Stats active residency delta,
     * or {@code -1.0} if unavailable.
     *
     * @return GPU utilization percentage, or -1.0
     */
    public static synchronized double sampleGpuUtilization() {
        if (!ensureInit()) {
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
     * Returns instantaneous GPU power in watts derived from the Energy Model delta, or {@code -1.0} if unavailable.
     *
     * @return GPU power in watts, or -1.0
     */
    public static synchronized double samplePowerWatts() {
        if (!ensureInit()) {
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
                // microjoules / nanoseconds * 1000 = watts (1 µJ/ns = 1 mW)
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
     * Initialises the IOReport subscription if not already done. Returns {@code true} if the subscription is ready.
     *
     * @return true if the subscription is ready
     */
    private static boolean ensureInit() {
        if (subscription != null) {
            return true;
        }
        if (initAttempted) {
            return false;
        }
        initAttempted = true;

        try {
            ioReport = Native.load("IOReport", IOReport.class);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }

        CFStringRef gpuGroup = CFStringRef.createCFString(GROUP_GPU_STATS);
        CFStringRef energyGroup = CFStringRef.createCFString(GROUP_ENERGY);
        CFDictionaryRef gpuChannels = null;
        CFDictionaryRef energyChannels = null;
        try {
            gpuChannels = ioReport.IOReportCopyChannelsInGroup(gpuGroup, null, 0, 0, 0);
            energyChannels = ioReport.IOReportCopyChannelsInGroup(energyGroup, null, 0, 0, 0);
            if (gpuChannels == null) {
                return false;
            }
            if (energyChannels != null) {
                ioReport.IOReportMergeChannels(gpuChannels, energyChannels, null);
            }
            // Subscribe to merged channels; the framework writes the subscribed channel
            // descriptor into subRef — use that for all subsequent IOReportCreateSamples calls.
            PointerByReference subRef = new PointerByReference();
            IOReportSubscriptionRef sub = ioReport.IOReportCreateSubscription(null, gpuChannels, subRef, 0, null);
            if (sub == null) {
                return false;
            }
            Pointer subPtr = subRef.getValue();
            if (subPtr == null) {
                sub.release();
                return false;
            }
            subscription = sub;
            subscribedChannels = new CFDictionaryRef(subPtr);
            // Shutdown hook releases native resources on JVM exit.
            // A follow-up issue should unify setup/teardown across all OS drivers.
            Runtime.getRuntime().addShutdownHook(new Thread(IOReportDriver::cleanup, "oshi-ioreport-cleanup"));
            return true;
        } catch (Exception e) {
            return false;
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
     * Releases all static IOReport resources. Called automatically via a JVM shutdown hook.
     *
     * <p>
     * This method is terminal: after it returns, {@code initAttempted} remains {@code true} so {@code ensureInit()}
     * will not attempt to re-subscribe. Re-initialization after cleanup is not supported. A follow-up issue should
     * unify setup/teardown across all OS platform drivers.
     */
    static synchronized void cleanup() {
        if (prevSampleUtil != null) {
            prevSampleUtil.release();
            prevSampleUtil = null;
        }
        if (prevSamplePower != null) {
            prevSamplePower.release();
            prevSamplePower = null;
        }
        if (subscribedChannels != null) {
            subscribedChannels.release();
            subscribedChannels = null;
        }
        if (subscription != null) {
            subscription.release();
            subscription = null;
        }
    }

    /**
     * Walks the channel array in a delta dictionary and returns the GPU Energy channel value in microjoules, or -1 if
     * not found.
     *
     * @param delta IOReport delta dictionary
     * @return GPU energy in microjoules, or -1 if not found
     */
    private static long extractGpuEnergyMicrojoules(CFDictionaryRef delta) {
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

    /**
     * Extracts per-state residency values from all channels in the given group (and optional subgroup) within a sample
     * or delta dictionary. State names are used as map keys; values are cumulative ticks.
     *
     * @param dict     sample or delta dictionary
     * @param group    IOReport group name to filter on
     * @param subgroup IOReport subgroup name to filter on, or {@code null} to accept all subgroups
     * @return map of state name to tick count; empty if the group is not present
     */
    static Map<String, Long> extractChannelStates(CFDictionaryRef dict, String group, String subgroup) {
        CFStringRef channelsKey = CFStringRef.createCFString(KEY_CHANNELS);
        try {
            Pointer arrPtr = dict.getValue(channelsKey);
            if (arrPtr == null) {
                return Collections.emptyMap();
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
            return result;
        } finally {
            channelsKey.release();
        }
    }
}
