/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Tests whether the Open Hardware Monitor and Libre Hardware Monitor sources of sensor data are disabled by
 * configuration.
 * <p>
 * Either application publishes its data only while it is running, and a user may start or stop it at any time, so OSHI
 * queries for it on every read rather than caching its availability. These switches let a user who does not run one of
 * them say so up front, so the query is never attempted. They are read on each call for the same reason the queries
 * are: configuration may be changed before the first query is made.
 *
 * @see GlobalConfig#OSHI_OS_WINDOWS_OHM_DISABLED
 * @see GlobalConfig#OSHI_OS_WINDOWS_LHM_DISABLED
 */
@ThreadSafe
public final class HardwareMonitorDisabled {

    private HardwareMonitorDisabled() {
        throw new AssertionError();
    }

    /**
     * Tests whether queries to a hardware monitor's WMI namespace are disabled.
     *
     * @param namespace the WMI namespace, either {@link OhmHardware#OHM_NAMESPACE} or {@link LhmSensor#LHM_NAMESPACE}
     * @return {@code true} if the namespace should not be queried, {@code false} otherwise, including for any other
     *         namespace
     */
    public static boolean isWmiDisabled(String namespace) {
        if (OhmHardware.OHM_NAMESPACE.equals(namespace)) {
            return GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, false);
        }
        if (LhmSensor.LHM_NAMESPACE.equals(namespace)) {
            return GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, false);
        }
        return false;
    }
}
