/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import org.jspecify.annotations.Nullable;

/**
 * Contract for Counter Property Enums. Enums implementing this interface define a specific PDH counter instance and
 * counter name pair.
 */
public interface PdhCounterProperty {
    /**
     * Gets the PDH counter instance name.
     *
     * @return Returns the instance, or {@code null} for a counter with no instance filter.
     */
    @Nullable
    String getInstance();

    /**
     * Gets the PDH counter name.
     *
     * @return Returns the counter.
     */
    String getCounter();
}
