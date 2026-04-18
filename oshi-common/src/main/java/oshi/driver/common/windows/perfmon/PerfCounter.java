/*
 * Copyright 2018-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import java.util.Objects;

import oshi.annotation.concurrent.Immutable;

/**
 * Encapsulates the three string components of a PDH performance counter path, plus whether it refers to the base
 * (SecondValue) of a multi-value counter.
 */
@Immutable
public final class PerfCounter {

    private final String object;
    private final String instance;
    private final String counter;
    private final boolean baseCounter;

    /**
     * Suffix appended to counter names in enum definitions to indicate that the SecondValue (base) should be read
     * instead of the FirstValue.
     */
    public static final String BASE_SUFFIX = "_Base";

    public PerfCounter(String objectName, String instanceName, String counterName) {
        this.object = objectName;
        this.instance = instanceName;
        this.baseCounter = counterName.endsWith(BASE_SUFFIX);
        this.counter = this.baseCounter ? counterName.substring(0, counterName.length() - BASE_SUFFIX.length())
                : counterName;
    }

    /**
     * @return Returns the object.
     */
    public String getObject() {
        return object;
    }

    /**
     * @return Returns the instance.
     */
    public String getInstance() {
        return instance;
    }

    /**
     * @return Returns the counter.
     */
    public String getCounter() {
        return counter;
    }

    /**
     * @return Returns whether the counter is a base counter
     */
    public boolean isBaseCounter() {
        return baseCounter;
    }

    /**
     * Returns the path for this counter
     *
     * @return A string representing the counter path
     */
    public String getCounterPath() {
        StringBuilder sb = new StringBuilder();
        sb.append('\\').append(object);
        if (instance != null) {
            sb.append('(').append(instance).append(')');
        }
        sb.append('\\').append(counter);
        return sb.toString();
    }

    /**
     * Strips the {@link #BASE_SUFFIX} from a counter name if present.
     *
     * @param counterName The counter name, possibly ending with {@code _Base}
     * @return The counter name without the {@code _Base} suffix, or the original name if the suffix is not present
     */
    public static String stripBaseSuffix(String counterName) {
        if (counterName.endsWith(BASE_SUFFIX)) {
            return counterName.substring(0, counterName.length() - BASE_SUFFIX.length());
        }
        return counterName;
    }

    /**
     * Tests whether a counter name has the {@link #BASE_SUFFIX}.
     *
     * @param counterName The counter name to test
     * @return true if the counter name ends with {@code _Base}
     */
    public static boolean isBase(String counterName) {
        return counterName.endsWith(BASE_SUFFIX);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PerfCounter)) {
            return false;
        }
        PerfCounter other = (PerfCounter) o;
        return baseCounter == other.baseCounter && Objects.equals(object, other.object)
                && Objects.equals(instance, other.instance) && Objects.equals(counter, other.counter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, instance, counter, baseCounter);
    }
}
