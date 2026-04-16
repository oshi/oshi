/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

/**
 * Contract for Wildcard Counter Property Enums. Enums implementing this interface define PDH counters where the first
 * enum element specifies an instance filter and remaining elements define counter names.
 */
public interface PdhCounterWildcardProperty {
    /**
     * @return Returns the counter. The first element of the enum will return the instance filter rather than a counter.
     */
    String getCounter();
}
