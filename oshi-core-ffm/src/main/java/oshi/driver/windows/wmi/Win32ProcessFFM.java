/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Process;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.common.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_Process} using FFM.
 */
@ThreadSafe
public final class Win32ProcessFFM extends Win32Process {

    private Win32ProcessFFM() {
    }

    /**
     * Returns process command lines.
     *
     * @param pidsToQuery Process IDs to query for command lines. Pass {@code null} to query all processes.
     * @return A {@link WmiResult} containing process IDs and command lines used to start the provided processes.
     */
    public static WmiResult<CommandLineProperty> queryCommandLines(Set<Integer> pidsToQuery) {
        WmiQuery<CommandLineProperty> commandLineQuery = new WmiQuery<>(buildWmiClassNameWithPidFilter(pidsToQuery),
                CommandLineProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(commandLineQuery);
    }

    /**
     * Returns process info.
     *
     * @param pids Process IDs to query.
     * @return Information on the provided processes.
     */
    public static WmiResult<ProcessXPProperty> queryProcesses(Collection<Integer> pids) {
        WmiQuery<ProcessXPProperty> processQueryXP = new WmiQuery<>(buildWmiClassNameWithPidFilter(pids),
                ProcessXPProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(processQueryXP);
    }
}
