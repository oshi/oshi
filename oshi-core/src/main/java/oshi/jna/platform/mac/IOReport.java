/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * IOReport is a private Apple framework that provides access to hardware performance counters, including GPU residency
 * and energy metrics on Apple Silicon.
 *
 * <p>
 * These are non-public APIs. Mappings are provided for OSHI internal use only and should not be considered stable.
 */
public interface IOReport extends Library {

    IOReport INSTANCE = Native.load("IOReport", IOReport.class);

    /**
     * Opaque handle returned by {@link #IOReportCreateSubscription}.
     */
    class IOReportSubscriptionRef extends CFTypeRef {
        public IOReportSubscriptionRef() {
            super();
        }

        public IOReportSubscriptionRef(Pointer p) {
            super(p);
        }
    }

    /**
     * Returns a mutable dictionary describing all channels in the given group (and optional subgroup).
     *
     * @param group    channel group name (e.g. {@code "GPU Stats"})
     * @param subgroup subgroup name, or {@code null} for all subgroups
     * @param a        reserved, pass 0
     * @param b        reserved, pass 0
     * @param c        reserved, pass 0
     * @return a CFDictionaryRef channel descriptor (caller must release), or {@code null} on failure
     */
    CFDictionaryRef IOReportCopyChannelsInGroup(CFStringRef group, CFStringRef subgroup, long a, long b, long c);

    /**
     * Merges the channel descriptors from {@code b} into {@code a} in place.
     *
     * @param a     destination channel descriptor (mutable)
     * @param b     source channel descriptor
     * @param null3 reserved, pass {@code null}
     */
    void IOReportMergeChannels(CFDictionaryRef a, CFDictionaryRef b, CFTypeRef null3);

    /**
     * Creates a subscription for the channels described by {@code channels}. The framework writes the actually
     * subscribed channel descriptor into {@code subscribedChannels}; callers must use that value (not the original
     * {@code channels}) when calling {@link #IOReportCreateSamples}.
     *
     * @param a                  reserved, pass {@code null}
     * @param channels           channel descriptor from {@link #IOReportCopyChannelsInGroup}
     * @param subscribedChannels receives the subscribed channel descriptor (caller must release)
     * @param b                  reserved, pass 0
     * @param c                  reserved, pass {@code null}
     * @return subscription handle (caller must release), or {@code null} on failure
     */
    IOReportSubscriptionRef IOReportCreateSubscription(Pointer a, CFDictionaryRef channels,
            PointerByReference subscribedChannels, long b, CFTypeRef c);

    /**
     * Takes a sample of all subscribed channels.
     *
     * @param subscription       the subscription handle
     * @param subscribedChannels the subscribed channel descriptor from {@link #IOReportCreateSubscription}
     * @param reserved           reserved, pass {@code null}
     * @return a CFDictionaryRef sample (caller must release), or {@code null} on failure
     */
    CFDictionaryRef IOReportCreateSamples(IOReportSubscriptionRef subscription, CFDictionaryRef subscribedChannels,
            CFTypeRef reserved);

    /**
     * Computes the delta between two samples taken from the same subscription.
     *
     * @param a        earlier sample
     * @param b        later sample
     * @param reserved reserved, pass {@code null}
     * @return a CFDictionaryRef delta (caller must release), or {@code null} on failure
     */
    CFDictionaryRef IOReportCreateSamplesDelta(CFDictionaryRef a, CFDictionaryRef b, CFTypeRef reserved);

    /**
     * Extracts the integer value from a single channel entry in a sample or delta dictionary.
     *
     * @param channel  a channel entry dictionary
     * @param reserved reserved, pass 0
     * @return the channel's integer value
     */
    long IOReportSimpleGetIntegerValue(CFDictionaryRef channel, int reserved);

    /**
     * Returns the group name of a channel entry.
     *
     * @param channel a channel entry dictionary
     * @return the group name as a CFStringRef
     */
    CFStringRef IOReportChannelGetGroup(CFDictionaryRef channel);

    /**
     * Returns the subgroup name of a channel entry.
     *
     * @param channel a channel entry dictionary
     * @return the subgroup name as a CFStringRef
     */
    CFStringRef IOReportChannelGetSubGroup(CFDictionaryRef channel);

    /**
     * Returns the channel name of a channel entry.
     *
     * @param channel a channel entry dictionary
     * @return the channel name as a CFStringRef
     */
    CFStringRef IOReportChannelGetChannelName(CFDictionaryRef channel);

    /**
     * Returns the number of states in a channel entry.
     *
     * @param channel a channel entry dictionary
     * @return the number of states
     */
    int IOReportStateGetCount(CFDictionaryRef channel);

    /**
     * Returns the name of the state at the given index.
     *
     * @param channel a channel entry dictionary
     * @param index   zero-based state index
     * @return the state name as a CFStringRef
     */
    CFStringRef IOReportStateGetNameForIndex(CFDictionaryRef channel, int index);

    /**
     * Returns the residency tick count for the state at the given index.
     *
     * @param channel a channel entry dictionary
     * @param index   zero-based state index
     * @return residency ticks
     */
    long IOReportStateGetResidency(CFDictionaryRef channel, int index);
}
