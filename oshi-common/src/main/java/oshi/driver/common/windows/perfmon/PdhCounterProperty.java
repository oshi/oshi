/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

/**
 * Contract for Counter Property Enums. Enums implementing this interface define a specific PDH counter instance and
 * counter name pair.
 */
public interface PdhCounterProperty {
    /**
     * Gets the PDH counter instance name.
     *
     * @return Returns the instance.
     */
    String getInstance();

    /**
     * Gets the PDH counter name.
     *
     * @return Returns the counter.
     */
    String getCounter();
}
