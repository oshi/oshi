/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Connection-independent logic for locating macOS SMC sensor keys by index.
 * <p>
 * The SMC exposes its keys as a list addressed by index, and returns them in ascending order of the key's four
 * characters. That ordering lets a caller binary-search for a prefix rather than reading every key: on an M2 Max the
 * full index is 2409 keys, of which 297 begin with {@code T} and 16 with {@code Tg}.
 * <p>
 * These methods take the index size and a lookup function rather than an SMC connection, so the JNA and FFM backends
 * can share this logic despite their different connection handle types, and so it can be tested without Mac hardware.
 */
@ThreadSafe
public final class SmcKeyIndex {

    private static final Logger LOG = LoggerFactory.getLogger(SmcKeyIndex.class);

    /**
     * Apple Silicon GPU cluster temperature keys, e.g. {@code Tg0f} or {@code Tg1A}.
     * <p>
     * The third character is always a digit, but the fourth varies: across the sensor keys published for M1 through M5
     * and A18 it is uppercase in 42% of cases, lowercase in 43%, and a digit in 15%. A mask requiring an uppercase
     * fourth character would miss {@code Tg0f}, which is the only GPU key present on some M2 machines.
     */
    private static final Pattern GPU_TEMPERATURE_KEY = Pattern.compile("^Tg\\d[\\dA-Za-z]$");

    /**
     * Fan current-speed keys, e.g. {@code F0Ac} or {@code F1Ac}.
     * <p>
     * Unlike {@link #GPU_TEMPERATURE_KEY} the fourth character is fixed, not variable: the published fan keys are
     * {@code F%dAc} current, {@code F%dMn} minimum, {@code F%dMx} maximum, {@code F%dSf} safe and {@code F%dTg} target,
     * so only the {@code Ac} suffix names the current speed. The mask matters because the other fan keys sort inside
     * the same {@code F} prefix block: an M3 Pro's block is
     * {@code F0Ac F0CR F0Dc F0Fb F0Fc F0Md F0Mn F0Mx F0Sf F0St F0Tg}, the same again for {@code F1}, then
     * {@code FBAC FBAD FNum FOFC FOff FRmp Fpds Frqd Ftst}.
     */
    private static final Pattern FAN_SPEED_KEY = Pattern.compile("^F\\dAc$");

    /** SMC keys are four characters. */
    private static final int KEY_LENGTH = 4;

    /**
     * Upper bound on a fan count read from {@code FNum}.
     * <p>
     * The binding constraint is the key format, not the hardware: {@code String.format("F%dAc", 10)} produces the
     * five-character {@code "F10Ac"}, and reading a key truncates it to four characters, so an eleventh fan would
     * silently read {@code "F10A"} instead. Ten is therefore the highest index this naming scheme can express. It also
     * sits above the hardware bound, since no Mac has more than eight fans.
     */
    public static final int MAX_FANS = 10;

    /**
     * Upper bound on a plausible {@code #KEY} count, guarding against a garbage read. Observed counts are in the low
     * thousands.
     */
    private static final int MAX_KEY_COUNT = 65536;

    /** Upper bound on the forward scan, so an unsorted index cannot cause a runaway read. */
    private static final int MAX_SCAN = 256;

    private SmcKeyIndex() {
    }

    /**
     * Locates the keys sharing a prefix, by binary searching the sorted key index and then scanning forward.
     * <p>
     * Returns {@code null}, rather than an empty list, if the index could not be read reliably. Callers must not cache
     * a {@code null} result: an empty list means "this machine has no such keys", while {@code null} means "ask again
     * later".
     *
     * @param keyCount   the number of keys in the index, from the SMC's {@code #KEY} key
     * @param keyAtIndex looks up the key name at an index, returning {@code null} if that read fails
     * @param prefix     the key prefix to locate, e.g. {@code "Tg"}
     * @param mask       an additional test each candidate key must pass
     * @return the matching keys in index order, or {@code null} if the index could not be read
     */
    public static @Nullable List<String> findKeys(int keyCount, IntFunction<String> keyAtIndex, String prefix,
            Predicate<String> mask) {
        if (keyCount <= 0 || keyCount > MAX_KEY_COUNT) {
            LOG.debug("Implausible SMC key count {}; skipping key discovery.", keyCount);
            return null; // NOSONAR java:S1168 - null and empty are different answers; see the javadoc
        }
        // Any failed read, in the binary search or in the forward scan, could have hidden a matching key: the search
        // substitutes a neighbour for an unreadable probe, which can move the landing point past the block entirely,
        // and the scan skips an unreadable index outright. Track failures through one wrapper so no path is missed.
        boolean[] readFailed = new boolean[1];
        IntFunction<String> tracked = i -> {
            String key = keyAtIndex.apply(i);
            if (key == null) {
                readFailed[0] = true;
            }
            return key;
        };
        int start = lowerBound(keyCount, tracked, prefix);
        if (start < 0) {
            return null; // NOSONAR java:S1168 - null and empty are different answers; see the javadoc
        }
        Set<String> found = new LinkedHashSet<>();
        int limit = Math.min(keyCount, start + MAX_SCAN);
        for (int i = start; i < limit; i++) {
            String key = tracked.apply(i);
            if (key == null) {
                key = tracked.apply(i); // one retry, in case the failure was transient
            }
            if (key == null) {
                // Skip rather than stop: an unreadable key in the middle of the block would otherwise discard every
                // key after it. The scan is bounded, and the prefix test below still ends it at the block boundary.
                LOG.debug("Could not read SMC key at index {}; continuing the scan.", i);
            } else {
                if (!key.startsWith(prefix)) {
                    break;
                }
                if (mask.test(key)) {
                    found.add(key);
                }
            }
        }
        if (found.isEmpty() && readFailed[0]) {
            // Nothing matched, but a read failed, so "no such keys" cannot be distinguished from "the one key this
            // machine has was unreadable". Returning null keeps the caller from caching an empty set and disabling the
            // sensor for the JVM lifetime. An empty result from a run with no failed read is different: that is a
            // genuine "this machine has none", and is a cacheable answer.
            LOG.debug("No SMC keys matched '{}' and at least one read failed; skipping key discovery.", prefix);
            return null; // NOSONAR java:S1168 - null and empty are different answers; see the javadoc
        }
        return Collections.unmodifiableList(new ArrayList<>(found));
    }

    /**
     * Binary searches for the first index whose key sorts at or after the prefix.
     *
     * @param keyCount   the number of keys in the index
     * @param keyAtIndex looks up the key name at an index
     * @param prefix     the prefix to locate
     * @return that index, or {@code -1} if the index could not be read
     */
    private static int lowerBound(int keyCount, IntFunction<String> keyAtIndex, String prefix) {
        int lo = 0;
        int hi = keyCount;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            String key = probe(keyAtIndex, mid, keyCount);
            if (key == null) {
                return -1;
            }
            // A four-character key is never equal to the shorter prefix, so this also skips the prefix itself.
            if (key.compareTo(prefix) > 0) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    /**
     * Reads the key at an index, retrying at neighbouring indices if that read fails, so a single unreadable key does
     * not abort discovery. Walking outward keeps the binary search's ordering assumption intact, since neighbours sort
     * adjacently.
     *
     * @param keyAtIndex looks up the key name at an index
     * @param index      the index to read
     * @param keyCount   the number of keys in the index, bounding the outward walk
     * @return a key name, or {@code null} if nothing nearby could be read
     */
    private static @Nullable String probe(IntFunction<String> keyAtIndex, int index, int keyCount) {
        String key = keyAtIndex.apply(index);
        if (key != null) {
            return key;
        }
        for (int delta = 1; delta <= 4; delta++) {
            if (index - delta >= 0) {
                key = keyAtIndex.apply(index - delta);
                if (key != null) {
                    return key;
                }
            }
            if (index + delta < keyCount) {
                key = keyAtIndex.apply(index + delta);
                if (key != null) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * Tests whether a key names an Apple Silicon GPU cluster temperature sensor.
     *
     * @param key the four-character SMC key
     * @return true if the key matches the GPU temperature naming convention
     */
    public static boolean isGpuTemperatureKey(@Nullable String key) {
        return key != null && GPU_TEMPERATURE_KEY.matcher(key).matches();
    }

    /**
     * Tests whether a key names a fan's current speed.
     *
     * @param key the four-character SMC key
     * @return true if the key matches the fan current-speed naming convention
     */
    public static boolean isFanSpeedKey(@Nullable String key) {
        return key != null && FAN_SPEED_KEY.matcher(key).matches();
    }

    /**
     * Builds the fan current-speed keys for a fan count, as the naming convention implies them.
     *
     * @param fanCount the number of fans, from the SMC's {@code FNum} key
     * @return the keys {@code F0Ac} through {@code F(n-1)Ac}, clamped to {@link #MAX_FANS}, never null
     */
    public static List<String> fanSpeedKeys(long fanCount) {
        // Clamp before narrowing: FNum is a single byte, but a mis-sized read returns whatever the buffer held.
        int fans = (int) Math.max(0, Math.min(MAX_FANS, fanCount));
        if (fans < fanCount) {
            LOG.warn("Ignoring an implausible SMC fan count of {}; using {}.", fanCount, fans);
        }
        List<String> keys = new ArrayList<>(fans);
        for (int i = 0; i < fans; i++) {
            keys.add(String.format(Locale.ROOT, "F%dAc", i));
        }
        return Collections.unmodifiableList(keys);
    }

    /**
     * Reconciles the fan keys found in the key index against the count reported by {@code FNum}.
     * <p>
     * Discovery is preferred where it produced anything, because the index lists the keys that actually exist. Where it
     * did not, a positive {@code FNum} still implies the conventionally named keys, which is what earlier versions of
     * OSHI read directly, so this never reports fewer fans than they did.
     * <p>
     * Returns {@code null} when the two sources together cannot distinguish "this machine has no fans" from "the keys
     * could not be read", so that the caller does not cache the answer. That happens when discovery failed and
     * {@code FNum} read as zero.
     *
     * @param discovered the keys found by {@link #findKeys}, or {@code null} if the index could not be read
     * @param fanCount   the count from {@code FNum}, or 0 if that read failed
     * @return the keys to read, or {@code null} if the answer is not yet known
     */
    public static @Nullable List<String> reconcileFanKeys(@Nullable List<String> discovered, long fanCount) {
        if (discovered != null && !discovered.isEmpty()) {
            if (fanCount > 0 && discovered.size() != fanCount) {
                LOG.debug("Found {} fan speed keys {} but FNum reports {} fans; using the discovered keys.",
                        discovered.size(), discovered, fanCount);
            }
            return discovered;
        }
        if (fanCount > 0) {
            List<String> keys = fanSpeedKeys(fanCount);
            LOG.debug("Fan speed key discovery found none; using the {} keys FNum implies: {}", keys.size(), keys);
            return keys;
        }
        if (discovered == null) {
            LOG.debug("Neither the SMC key index nor FNum could be read; deferring the fan count.");
            return null; // NOSONAR java:S1168 - null and empty are different answers; see the javadoc
        }
        return Collections.emptyList();
    }

    /**
     * Parses a user-supplied comma-separated list of SMC keys, ignoring blanks and warning about malformed entries.
     *
     * @param csv the configured value, which may be null or empty
     * @return the keys, never null
     */
    public static List<String> parseConfiguredKeys(@Nullable String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keys = new ArrayList<>();
        for (String token : csv.split(",", -1)) {
            String key = token.trim();
            if (key.length() == KEY_LENGTH) {
                keys.add(key);
            } else if (!key.isEmpty()) {
                LOG.warn("Ignoring configured SMC key '{}': keys are exactly {} characters.", key, KEY_LENGTH);
            }
        }
        return Collections.unmodifiableList(keys);
    }

    /**
     * Returns the first plausible reading among the given keys, in the order given.
     * <p>
     * Plausibility is applied here, at read time, for the same reason as in {@link #maxPlausible}: whether a key exists
     * is a property of the hardware and does not change, but whether it currently reports a usable value is not.
     *
     * @param keys        the keys to read, in preference order
     * @param reader      reads a key and returns the value in its final units, 0 if unavailable
     * @param isPlausible tests whether a reading is usable
     * @param description what is being read, for log messages, e.g. {@code "temperature"}
     * @return the first plausible reading, or 0 if none were plausible
     */
    public static double firstPlausible(List<String> keys, ToDoubleFunction<String> reader, DoublePredicate isPlausible,
            String description) {
        for (String key : keys) {
            double value = reader.applyAsDouble(key);
            if (isPlausible.test(value)) {
                return value;
            }
            // A zero means the key was absent or undecodable, which is expected while scanning a candidate list; a
            // non-zero implausible value means a sensor answered with something unusable, which is worth a note.
            if (value != 0d) {
                LOG.debug("Ignoring implausible {} {} from SMC key {}.", description, value, key);
            }
        }
        return 0d;
    }

    /**
     * Returns the highest plausible temperature among the given keys.
     * <p>
     * Plausibility is applied here, at read time, rather than when the keys were discovered. Whether a key exists is a
     * property of the hardware and does not change; whether it currently reports a usable value is not, because an idle
     * sensor may report a sentinel below ambient. Filtering at discovery time would let a single unlucky first read
     * cache an empty set and disable the sensor for the lifetime of the JVM.
     *
     * @param keys        the keys to read
     * @param reader      reads a key, returning the temperature in degrees Celsius
     * @param isPlausible tests whether a reading is usable
     * @return the highest plausible reading, or 0 if none were plausible
     */
    public static double maxPlausible(List<String> keys, ToDoubleFunction<String> reader, DoublePredicate isPlausible) {
        double max = 0d;
        for (String key : keys) {
            double value = reader.applyAsDouble(key);
            if (isPlausible.test(value) && value > max) {
                max = value;
            }
        }
        if (max == 0d && !keys.isEmpty()) {
            LOG.debug("No plausible temperature among SMC keys {}; sensors are likely idle-gated.", keys);
        }
        return max;
    }
}
